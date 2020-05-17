@file:Suppress("PackageDirectoryMismatch")

package parser4k.calculatortests

import parser4k.*
import parser4k.CommonParsers.token
import parser4k.Expression.*
import parser4k.Expression.Number
import java.math.BigDecimal
import kotlin.test.Test


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

    @Test fun `it works`() {
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

    @Test fun `it works`() {
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

    @Test fun `it works`() {
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

interface IEvaluate {
    fun evaluate(s: String): BigDecimal
}

private object Calculator : IEvaluate {
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

    override fun evaluate(s: String) = s.parseWith(expr).evaluate()

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

private object MinimalCalculator : IEvaluate {
    private val cache = OutputCache<BigDecimal>()

    private val number = regex("\\d+").map { it.toBigDecimal() }
    private val paren = inOrder(token("("), ref { expr }, token(")")).map { (_, it, _) -> it }
    private val power = inOrder(ref { expr }, token("^"), ref { expr }).map { (l, _, r) -> l.pow(r.toInt()) }.with(cache)
    private val divide = inOrder(ref { expr }, token("/"), ref { expr }).leftAssoc { (l, _, r) -> l.divide(r) }.with(cache)
    private val multiply = inOrder(ref { expr }, token("*"), ref { expr }).leftAssoc { (l, _, r) -> l * r }.with(cache)
    private val minus = inOrder(ref { expr }, token("-"), ref { expr }).leftAssoc { (l, _, r) -> l - r }.with(cache)
    private val plus = inOrder(ref { expr }, token("+"), ref { expr }).leftAssoc { (l, _, r) -> l + r }.with(cache)

    private val expr: Parser<BigDecimal> = oneOfWithPrecedence(
        oneOf(plus, minus),
        oneOf(multiply, divide),
        power,
        paren.nestedPrecedence(),
        number
    ).reset(cache)

    override fun evaluate(s: String) = s.parseWith(expr)
}


class CalculatorTests {
    @Test fun `valid input`() = listOf(Calculator, MinimalCalculator).forEach {
        it.evaluate("1") shouldEqual BigDecimal(1)
        it.evaluate("1 + 2") shouldEqual BigDecimal(3)
        it.evaluate("1 + 2 * 3") shouldEqual BigDecimal(7)
        it.evaluate("1 - 2 * 3") shouldEqual BigDecimal(-5)
        it.evaluate("1 - 2 * 3 + 4 / 5") shouldEqual BigDecimal("-4.2")
        it.evaluate("(1 + 2) * 3 - 4 / 5") shouldEqual BigDecimal("8.2")
        it.evaluate("(1 + 2) * (3 - 4) / 5") shouldEqual BigDecimal("-0.6")
        it.evaluate("2^12 - 2^10") shouldEqual BigDecimal(3072)
    }

    @Test fun `large valid input`() = listOf(Calculator, MinimalCalculator).forEach {
        it.evaluate(List(1000) { "1" }.joinToString("+")) shouldEqual BigDecimal(1000)
    }

    @Test fun `invalid input`() {
        { Calculator.evaluate("+1") } shouldFailWith { it is NoMatchingParsers }
        { Calculator.evaluate("()") } shouldFailWith  { it is NoMatchingParsers }

        { Calculator.evaluate("(1))") } shouldFailWithMessage """
            |
            |(1))
            |   ^
            |payload = Number(value=1)
        """

        { Calculator.evaluate("1 + 2 + ") } shouldFailWithMessage """
            |
            |1 + 2 + 
            |     ^
            |payload = Plus(left=Number(value=1), right=Number(value=2))
        """
    }
}