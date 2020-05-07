@file:Suppress("PackageDirectoryMismatch")

package parser4k.calculatortests

import parser4k.*
import parser4k.CommonParsers.token
import parser4k.calculatortests.Expression.*
import parser4k.calculatortests.Expression.Number
import kotlin.test.Test
import kotlin.test.assertEquals


sealed class Expression {
    data class Number(val value: Int): Expression()
    data class Plus(val left: Expression, val right: Expression): Expression()
    data class Minus(val left: Expression, val right: Expression): Expression()
    data class Multiply(val left: Expression, val right: Expression): Expression()
    data class Divide(val left: Expression, val right: Expression): Expression()
}

private val number = regex("\\d+").map { Number(it.toInt()) }

class NoneRecursiveParserTests {
    private val expression = inOrder(number, repeat(inOrder(or(token("+"), token("-")), number)))
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
        "123" shouldParseTo Number(123)
        "1 + 2" shouldParseTo Plus(Number(1), Number(2))
        "1 - 2" shouldParseTo Minus(Number(1), Number(2))
        "1 + 2 + 3" shouldParseTo Plus(
            Plus(Number(1), Number(2)),
            Number(3)
        )
        "1 + 2 - 3" shouldParseTo Minus(
            Plus(Number(1), Number(2)),
            Number(3)
        )
    }

    @Test fun `invalid input`() {
        expression.parse(Input("abc")) shouldEqual null
        expression.parse(Input("+123")) shouldEqual null
        expression.parse(Input("-123")) shouldEqual null
    }

    @Test fun `partial input`() {
        expression.parse(Input("1 + 2 +")) shouldEqual Output(
            payload = Plus(Number(1), Number(2)),
            input = Input(value = "1 + 2 +", offset = 5)
        )
        expression.parse(Input("1 ++ 2")) shouldEqual Output(
            payload = Number(1),
            input = Input(value = "1 ++ 2", offset = 1)
        )
    }

    private infix fun String.shouldParseTo(expected: Expression) =
        assertEquals(expected, expression.parseAllInputOrFail(this))
}

class RecursiveParserTests {
    private val plus = inOrder(leftRef { expr }, token("+"), ref { expr }).mapAsBinary(::Plus)
    private val minus = inOrder(leftRef { expr }, token("-"), ref { expr }).mapAsBinary(::Minus)
    private val expr: Parser<Expression> = or(minus, plus, number)

    @Test fun `valid input`() {
        "123" shouldParseTo Number(123)
        "1 + 2" shouldParseTo Plus(Number(1), Number(2))
        "1 - 2" shouldParseTo Minus(Number(1), Number(2))
        "1 + 2 + 3" shouldParseTo Plus(
            Number(1),
            Plus(Number(2), Number(3))
        )
        "1 - 2 - 3" shouldParseTo Minus(
            Number(1),
            Minus(Number(2), Number(3))
        )
        "1 + 2 - 3" shouldParseTo Plus(
            Number(1),
            Minus(Number(2), Number(3))
        )
        "1 - 2 + 3" shouldParseTo Minus(
            Number(1),
            Plus(Number(2), Number(3))
        )
    }

    private infix fun String.shouldParseTo(expected: Expression) =
        assertEquals(expected, expr.parseAllInputOrFail(this))
}

class ParserPrecedenceTests {
    private val paren = inOrder(token("("), ref { expr }, token(")")).map { (_, it, _) -> it }
    private val multiply = inOrder(leftRef { expr }, token("*"), ref { expr }).mapAsBinary(::Multiply)
    private val plus = inOrder(leftRef { expr }, token("+"), ref { expr }).mapAsBinary(::Plus)

    private val expr: Parser<Expression> = orWithPrecedence(plus, multiply, paren.resetPrecedence(), number)

    @Test fun `valid input`() {
        "123" shouldParseTo Number(123)
        "1 + 2" shouldParseTo Plus(Number(1), Number(2))
        "1 * 2" shouldParseTo Multiply(Number(1), Number(2))
        "1 + 2 + 3" shouldParseTo Plus(
            Number(1),
            Plus(Number(2), Number(3))
        )
        "1 * 2 * 3" shouldParseTo Multiply(
            Number(1),
            Multiply(Number(2), Number(3))
        )
        "1 + 2 * 3" shouldParseTo Plus(
            Number(1),
            Multiply(Number(2), Number(3))
        )
        "1 * 2 + 3" shouldParseTo Plus(
            Multiply(Number(1), Number(2)),
            Number(3)
        )

        "(123)" shouldParseTo Number(123)
        "((123))" shouldParseTo Number(123)

        "(1 * 2) + 3" shouldParseTo Plus(
            Multiply(Number(1), Number(2)),
            Number(3)
        )
        "1 * (2 + 3)" shouldParseTo Multiply(
            Number(1),
            Plus(Number(2), Number(3))
        )
        "((1 * 2) + 3)" shouldParseTo Plus(
            Multiply(Number(1), Number(2)),
            Number(3)
        )

        "(1 + 2) + (3 + 4)" shouldParseTo Plus(
            Plus(Number(1), Number(2)),
            Plus(Number(3), Number(4))
        )
    }

    private infix fun String.shouldParseTo(expected: Expression) =
        assertEquals(expected, expr.parseAllInputOrFail(this))
}

class ParserPerformanceTests {
    private val log = ArrayList<String>()
    private val cache = OutputCache<Expression>()

    private val divide = inOrder(ref { expr }, token("/"), ref { expr }).mapAsBinary(::Divide).logNoOutput("divide").with(cache)
    private val multiply = inOrder(ref { expr }, token("*"), ref { expr }).mapAsBinary(::Multiply).logNoOutput("multiply").with(cache)
    private val minus = inOrder(ref { expr }, token("-"), ref { expr }).mapAsBinary(::Minus).logNoOutput("minus").with(cache)
    private val plus = inOrder(ref { expr }, token("+"), ref { expr }).mapAsBinary(::Plus).logNoOutput("plus").with(cache)

    private val expr: Parser<Expression> = or(plus, minus, multiply, divide, number).reset(cache)

    @Test fun `use each parser once at each input offset`() {
        expectMinimalLog { "1 + 2" shouldParseTo Plus(Number(1), Number(2)) }
        expectMinimalLog { "1 - 2" shouldParseTo Minus(Number(1), Number(2)) }
        expectMinimalLog { "1 * 2" shouldParseTo Multiply(Number(1), Number(2)) }
        expectMinimalLog { "1 / 2" shouldParseTo Divide(Number(1), Number(2)) }
        expectMinimalLog { "1" shouldParseTo Number(1) }
    }

    private infix fun String.shouldParseTo(expected: Expression) =
        assertEquals(expected, expr.parseAllInputOrFail(this))

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
}
