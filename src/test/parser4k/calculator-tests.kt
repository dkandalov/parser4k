@file:Suppress("PackageDirectoryMismatch")

package parser4k.calculatortests

import parser4k.*
import parser4k.CommonParsers.token
import parser4k.calculatortests.Calculator.evaluate
import parser4k.calculatortests.Expression.*
import parser4k.calculatortests.Expression.Number
import java.math.BigDecimal
import kotlin.test.Test


sealed class Expression {
    data class Number(val value: BigDecimal): Expression()
    data class Plus(val left: Expression, val right: Expression): Expression()
    data class Minus(val left: Expression, val right: Expression): Expression()
    data class Multiply(val left: Expression, val right: Expression): Expression()
    data class Divide(val left: Expression, val right: Expression): Expression()
    data class Power(val left: Expression, val right: Expression): Expression()
}

private val number = regex("\\d+").map { Number(it.toBigDecimal()) }

class NoneRecursiveParserTests {
    private val expression = inOrder(number, repeat(inOrder(oneOf(token("+"), token("-")), number)))
        .map { (first, rest) ->
            rest.fold(first as Expression) { left, (operator, right) ->
                when (operator) {
                    "+"  -> Plus(left, right)
                    "-"  -> Minus(left, right)
                    else -> error("")
                }
            }
        }

    @Test fun `valid input`() {
        "123" shouldParseTo "123"
        "1 + 2" shouldParseTo "[1 + 2]"
        "1 - 2" shouldParseTo "[1 - 2]"
        "1 + 2 + 3" shouldParseTo "[[1 + 2] + 3]"
        "1 + 2 - 3" shouldParseTo "[[1 + 2] - 3]"
    }

    private infix fun String.shouldParseTo(expected: String) = parseWith(expression).toExpressionString() shouldEqual expected
}

class RecursiveParserTests {
    private val power = inOrder(nonRecRef { expr }, token("^"), ref { expr }).mapAsBinary(::Power)
    private val expr: Parser<Expression> = oneOf(power, number)

    @Test fun `valid input`() {
        "123" shouldParseTo "123"
        "1 ^ 2" shouldParseTo "[1 ^ 2]"
        "1 ^ 2 ^ 3" shouldParseTo "[1 ^ [2 ^ 3]]"
        "1 ^ 2 ^ 3 ^ 4" shouldParseTo "[1 ^ [2 ^ [3 ^ 4]]]"
    }

    private infix fun String.shouldParseTo(expected: String) = parseWith(expr).toExpressionString() shouldEqual expected
}

class ParserPrecedenceTests {
    private val paren = inOrder(token("("), ref { expr }, token(")")).map { (_, it, _) -> it }
    private val multiply = inOrder(nonRecRef { expr }, token("*"), ref { expr }).mapAsBinary(::Multiply)
    private val plus = inOrder(nonRecRef { expr }, token("+"), ref { expr }).mapAsBinary(::Plus)

    private val expr: Parser<Expression> = oneOfWithPrecedence(
        plus,
        multiply,
        paren.nestedPrecedence(),
        number
    )

    @Test fun `valid input`() {
        "123" shouldParseTo "123"
        "1 + 2" shouldParseTo "[1 + 2]"
        "1 * 2" shouldParseTo "[1 * 2]"
        "1 + 2 + 3" shouldParseTo "[1 + [2 + 3]]"
        "1 * 2 * 3" shouldParseTo "[1 * [2 * 3]]"
        "1 * 2 + 3" shouldParseTo "[[1 * 2] + 3]"
        "1 + 2 * 3" shouldParseTo "[1 + [2 * 3]]"

        "(123)" shouldParseTo "123"
        "((123))" shouldParseTo "123"
        "(1 + 2) * 3" shouldParseTo "[[1 + 2] * 3]"
        "1 + (2 * 3)" shouldParseTo "[1 + [2 * 3]]"
        "((1 + 2) * 3)" shouldParseTo "[[1 + 2] * 3]"

        "(1 + 2) + (3 + 4)" shouldParseTo "[[1 + 2] + [3 + 4]]"
    }

    private infix fun String.shouldParseTo(expected: String) = parseWith(expr).toExpressionString() shouldEqual expected
}

class ParserAssociativityTests {
    private val plus = inOrder(nonRecRef { expr }, token("+"), ref { expr })
        .leftAssoc { (left, _, right) -> Plus(left, right) }

    private val power = inOrder(nonRecRef { expr }, token("^"), ref { expr })
        .map { (left, _, right) -> Power(left, right) }

    private val expr: Parser<Expression> = oneOfWithPrecedence(plus, power, number)

    @Test fun `it works`() {
        "123" shouldParseTo "123"

        "1 + 2" shouldParseTo "[1 + 2]"
        "1 + 2 + 3" shouldParseTo "[[1 + 2] + 3]"
        "1 + 2 + 3 + 4" shouldParseTo "[[[1 + 2] + 3] + 4]"

        "1 ^ 2" shouldParseTo "[1 ^ 2]"
        "1 ^ 2 ^ 3" shouldParseTo "[1 ^ [2 ^ 3]]"
        "1 ^ 2 ^ 3 ^ 4" shouldParseTo "[1 ^ [2 ^ [3 ^ 4]]]"

        "1^2 + 3" shouldParseTo "[[1 ^ 2] + 3]"
        "1 + 2^3" shouldParseTo "[1 + [2 ^ 3]]"
        "1^2 + 3^4" shouldParseTo "[[1 ^ 2] + [3 ^ 4]]"

        "1^2 + 3 + 4" shouldParseTo "[[[1 ^ 2] + 3] + 4]"
        "1 + 2^3 + 4" shouldParseTo "[[1 + [2 ^ 3]] + 4]"
        "1 + 2 + 3^4" shouldParseTo "[[1 + 2] + [3 ^ 4]]"

        "1 + 2^3 + 4^5" shouldParseTo "[[1 + [2 ^ 3]] + [4 ^ 5]]"
        "1^2 + 3 + 4^5" shouldParseTo "[[[1 ^ 2] + 3] + [4 ^ 5]]"
        "1^2 + 3^4 + 5" shouldParseTo "[[[1 ^ 2] + [3 ^ 4]] + 5]"

        "1 + 2^3^4" shouldParseTo "[1 + [2 ^ [3 ^ 4]]]"
        "1^2^3 + 4" shouldParseTo "[[1 ^ [2 ^ 3]] + 4]"
    }

    private infix fun String.shouldParseTo(expected: String) = parseWith(expr).toExpressionString() shouldEqual expected
}

private fun Expression.toExpressionString(): String =
    when (this) {
        is Number   -> value.toString()
        is Plus     -> "[${left.toExpressionString()} + ${right.toExpressionString()}]"
        is Minus    -> "[${left.toExpressionString()} - ${right.toExpressionString()}]"
        is Multiply -> "[${left.toExpressionString()} * ${right.toExpressionString()}]"
        is Divide   -> "[${left.toExpressionString()} / ${right.toExpressionString()}]"
        is Power    -> "[${left.toExpressionString()} ^ ${right.toExpressionString()}]"
    }

class ParserPerformanceTests {
    private val log = ArrayList<String>()
    private val cache = OutputCache<Expression>()

    private val divide = inOrder(ref { expr }, token("/"), ref { expr }).leftAssocAsBinary(::Divide).logNoOutput("divide").with(cache)
    private val multiply = inOrder(ref { expr }, token("*"), ref { expr }).leftAssocAsBinary(::Multiply).logNoOutput("multiply").with(cache)
    private val minus = inOrder(ref { expr }, token("-"), ref { expr }).leftAssocAsBinary(::Minus).logNoOutput("minus").with(cache)
    private val plus = inOrder(ref { expr }, token("+"), ref { expr }).leftAssocAsBinary(::Plus).logNoOutput("plus").with(cache)

    private val expr: Parser<Expression> = oneOf(plus, minus, multiply, divide, number).reset(cache)

    @Test fun `use each parser once at each input offset`() {
        expectMinimalLog { "1" shouldParseTo "1" }
        expectMinimalLog { "1 + 2" shouldParseTo "[1 + 2]" }
        expectMinimalLog { "1 - 2" shouldParseTo "[1 - 2]" }
        expectMinimalLog { "1 * 2" shouldParseTo "[1 * 2]" }
        expectMinimalLog { "1 / 2" shouldParseTo "[1 / 2]" }
    }

    private fun expectMinimalLog(f: () -> Unit) {
        log.clear()
        f()
        log.size shouldEqual log.distinct().size
    }

    private fun <T> Parser<T>.logNoOutput(parserId: String): Parser<T> {
        return onNoOutput {
            log.add("offset ${it.offset}: $parserId")
        }
    }

    private fun <T> Parser<T>.onNoOutput(f: (Input) -> Unit) = object: Parser<T> {
        override fun parse(input: Input): Output<T>? {
            val output = this@onNoOutput.parse(input)
            if (output == null) f(input)
            return output
        }
    }

    private infix fun String.shouldParseTo(expected: String) = parseWith(expr).toExpressionString() shouldEqual expected
}

private object Calculator {
    private val cache = OutputCache<Expression>()

    private val number = regex("\\d+").map { Number(it.toBigDecimal()) }
    private val paren = inOrder(token("("), ref { expr }, token(")")).map { (_, it, _) -> it }
    private val divide = inOrder(ref { expr }, token("/"), ref { expr }).leftAssocAsBinary(::Divide).with(cache)
    private val multiply = inOrder(ref { expr }, token("*"), ref { expr }).leftAssocAsBinary(::Multiply).with(cache)
    private val minus = inOrder(ref { expr }, token("-"), ref { expr }).leftAssocAsBinary(::Minus).with(cache)
    private val plus = inOrder(ref { expr }, token("+"), ref { expr }).leftAssocAsBinary(::Plus).with(cache)
    private val power = inOrder(ref { expr }, token("^"), ref { expr }).mapAsBinary(::Power).with(cache)

    private val expr: Parser<Expression> = oneOfWithPrecedence(
        oneOf(plus, minus),
        oneOf(multiply, divide),
        power,
        paren.nestedPrecedence(),
        number
    ).reset(cache)

    fun evaluate(s: String) = s.parseWith(expr).evaluate()

    private fun Expression.evaluate(): BigDecimal =
        when (this) {
            is Number   -> value
            is Plus     -> left.evaluate() + right.evaluate()
            is Minus    -> left.evaluate() - right.evaluate()
            is Multiply -> left.evaluate() * right.evaluate()
            is Divide   -> left.evaluate().divide(right.evaluate())
            is Power    -> left.evaluate().pow(right.evaluate().toInt())
        }
}


class CalculatorTests {
    @Test fun `valid input`() {
        evaluate("1") shouldEqual BigDecimal(1)
        evaluate("1 + 2") shouldEqual BigDecimal(3)
        evaluate("1 + 2 * 3") shouldEqual BigDecimal(7)
        evaluate("1 - 2 * 3") shouldEqual BigDecimal(-5)
        evaluate("1 - 2 * 3 + 4 / 5") shouldEqual BigDecimal("-4.2")
        evaluate("(1 + 2) * 3 - 4 / 5") shouldEqual BigDecimal("8.2")
        evaluate("(1 + 2) * (3 - 4) / 5") shouldEqual BigDecimal("-0.6")
        evaluate("2^12 - 2^10") shouldEqual BigDecimal(3072)
    }

    @Test fun `large valid input`() {
        evaluate(List(1000) { "1" }.joinToString("+")) shouldEqual BigDecimal(1000)
    }

    @Test fun `invalid input`() {
        { evaluate("+1") } shouldFailWith { it is NoMatchingParsers }
        { evaluate("()") } shouldFailWith  { it is NoMatchingParsers }

        { evaluate("(1))") } shouldFailWithMessage """
            |
            |(1))
            |   ^
            |payload = Number(value=1)
        """

        { evaluate("1 + 2 + ") } shouldFailWithMessage """
            |
            |1 + 2 + 
            |     ^
            |payload = Plus(left=Number(value=1), right=Number(value=2))
        """
    }
}