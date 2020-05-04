package parser4k

import parser4k.PlusMinusLanguageTests.Expression.*
import parser4k.PlusMinusLanguageTests.OperatorType.Minus
import parser4k.PlusMinusLanguageTests.OperatorType.Plus
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Goals:
 * - simple (has very few core concepts)
 * - easy to use (you can quickly figure out how to write a parser for a small language)
 * - production-ready (not a toy project, good-enough performance for real-world applications)
 *
 * - testable 🤔
 * - standalone (no dependencies)
 * - small
 * - multi-platform
 */

data class Input(val s: String, val offset: Int = 0) {
    fun read(n: Int): Pair<String, Input>? =
        if (offset + n > s.length) null
        else s.substring(offset, offset + n) to copy(offset = offset + n)
}

data class Output<out T>(val payload: T, val input: Input)

interface Parser<T> {
    fun parse(input: Input): Output<T>?
}

fun str(s: String) = object: Parser<String> {
    override fun parse(input: Input): Output<String>? {
        val (token, nextInput) = input.read(s.length) ?: return null
        return if (token != s) null else Output(token, nextInput)
    }
}

fun regex(pattern: String) = object: Parser<String> {
    override fun parse(input: Input): Output<String>? {
        val matchResult = pattern.toRegex().find(input.s, input.offset) ?: return null
        if (matchResult.range.first != input.offset) return null
        return Output(input.s.substring(matchResult.range), input.copy(offset = matchResult.range.last + 1))
    }
}

fun <T> repeat(parser: Parser<T>, atLeast: Int = 0) = object: Parser<List<T>> {
    override fun parse(input: Input): Output<List<T>>? {
        val payload = ArrayList<T>()
        var nextInput = input
        while (true) {
            val output = parser.parse(nextInput) ?: break
            nextInput = output.input
            payload.add(output.payload)
        }
        return if (payload.size >= atLeast) Output(payload, nextInput) else null
    }
}

fun <T> or(vararg parsers: Parser<T>) = object: Parser<T> {
    override fun parse(input: Input): Output<T>? {
        parsers.forEach {
            val output = it.parse(input)
            if (output != null) return output
        }
        return null
    }
}

fun <T1> inOrder(parser1: Parser<T1>) = object: Parser<List1<T1>> {
    override fun parse(input: Input): Output<List1<T1>>? {
        val (payload, nextInput) = parser1.parse(input) ?: return null
        return Output(List1(payload), nextInput)
    }
}

fun <T1, T2> inOrder(parser1: Parser<T1>, parser2: Parser<T2>) = object: Parser<List2<T1, T2>> {
    override fun parse(input: Input): Output<List2<T1, T2>>? {
        val (payload, nextInput) = inOrder(parser1).parse(input) ?: return null
        val (lastPayload, lastInput) = parser2.parse(nextInput) ?: return null
        return Output(payload + lastPayload, lastInput)
    }
}

fun <T1, T2, T3> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>) =
    object: Parser<List3<T1, T2, T3>> {
        override fun parse(input: Input): Output<List3<T1, T2, T3>>? {
            val (payload, nextInput) = inOrder(parser1, parser2).parse(input) ?: return null
            val (lastPayload, lastInput) = parser3.parse(nextInput) ?: return null
            return Output(payload + lastPayload, lastInput)
        }
    }

fun <T1, T2, T3, T4> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>) =
    object: Parser<List4<T1, T2, T3, T4>> {
        override fun parse(input: Input): Output<List4<T1, T2, T3, T4>>? {
            val (payload, nextInput) = inOrder(parser1, parser2, parser3).parse(input) ?: return null
            val (lastPayload, lastInput) = parser4.parse(nextInput) ?: return null
            return Output(payload + lastPayload, lastInput)
        }
    }

data class List1<T1>(val value1: T1) {
    operator fun <T2> plus(value2: T2): List2<T1, T2> = List2(value1, value2)
}

data class List2<T1, T2>(val value1: T1, val value2: T2) {
    operator fun <T3> plus(value3: T3): List3<T1, T2, T3> = List3(value1, value2, value3)
}

data class List3<T1, T2, T3>(val value1: T1, val value2: T2, val value3: T3) {
    operator fun <T4> plus(value4: T4): List4<T1, T2, T3, T4> = List4(value1, value2, value3, value4)
}

data class List4<T1, T2, T3, T4>(val value1: T1, val value2: T2, val value3: T3, val value4: T4)


class StrParserTests {
    @Test fun `it works`() {
        // not enough input
        str("foo").parse(Input("")) shouldEqual null
        str("foo").parse(Input("f")) shouldEqual null
        str("foo").parse(Input("foo", offset = 1)) shouldEqual null

        // input mismatch
        str("foo").parse(Input("bar")) shouldEqual null
        str("foo").parse(Input("fo0")) shouldEqual null

        // matches
        str("foo").parse(Input("foo")) shouldEqual Output("foo", Input("foo", offset = 3))
        str("foo").parse(Input("foo__")) shouldEqual Output("foo", Input("foo__", offset = 3))
        str("foo").parse(Input("_foo_", offset = 1)) shouldEqual Output("foo", Input("_foo_", offset = 4))
        str("foo").parse(Input("__foo", offset = 2), ) shouldEqual Output("foo", Input("__foo", offset = 5))
    }
}

class RegexParserTests {
    @Test fun `it works`() {
        // not enough input
        regex("a").parse(Input("")) shouldEqual null
        regex("a").parse(Input("foo", offset = 3)) shouldEqual null

        // input mismatch
        regex("a").parse(Input("b")) shouldEqual null
        regex("a").parse(Input("ba")) shouldEqual null
        regex("[abc]").parse(Input("zzz")) shouldEqual null

        // matches
        regex("a").parse(Input("a")) shouldEqual Output("a", Input("a", offset = 1))
        regex("a").parse(Input("abc")) shouldEqual Output("a", Input("abc", offset = 1))
        regex("[abc]").parse(Input("abc", offset = 0)) shouldEqual Output("a", Input("abc", offset = 1))
        regex("[abc]").parse(Input("abc", offset = 1)) shouldEqual Output("b", Input("abc", offset = 2))
        regex("[abc]").parse(Input("abc", offset = 2), ) shouldEqual Output("c", Input("abc", offset = 3))
    }
}

class RepeatParserTests {
    @Test fun `it works`() {
        inOrder(str("a"), str("b")).parse(Input("")) shouldEqual null
        inOrder(str("a"), str("b")).parse(Input("a")) shouldEqual null

        inOrder(str("a")).parse(Input("abc")) shouldEqual Output(List1("a"), Input("abc", offset = 1))
        inOrder(str("a"), str("b")).parse(Input("abc")) shouldEqual Output(List2("a", "b"), Input("abc", offset = 2))
        inOrder(str("a"), str("b"), str("c")).parse(Input("abc")) shouldEqual Output(
            List3("a", "b", "c"),
            Input("abc", offset = 3)
        )
        inOrder(str("a"), str("b"), str("c"), str("d")).parse(Input("abcd")) shouldEqual Output(
            List4("a", "b", "c", "d"),
            Input("abcd", offset = 4)
        )
    }
}

class InOrderParserTests {
    @Test fun `it works`() {
        // not enough input
        str("foo").parse(Input("")) shouldEqual null
        str("foo").parse(Input("f")) shouldEqual null
        str("foo").parse(Input("foo", offset = 1)) shouldEqual null

        // input mismatch
        str("foo").parse(Input("bar")) shouldEqual null
        str("foo").parse(Input("fo0")) shouldEqual null

        // matches
        str("foo").parse(Input("foo")) shouldEqual Output("foo", Input("foo", offset = 3))
        str("foo").parse(Input("foo__")) shouldEqual Output("foo", Input("foo__", offset = 3))
        str("foo").parse(Input("_foo_", offset = 1)) shouldEqual Output("foo", Input("_foo_", offset = 4))
        str("foo").parse(Input("__foo", offset = 2), ) shouldEqual Output("foo", Input("__foo", offset = 5))
    }
}


fun <T, R> Parser<T>.map(f: (T) -> R) = object: Parser<R> {
    override fun parse(input: Input): Output<R>? {
        val (payload, nextInput) = this@map.parse(input) ?: return null
        return Output(f(payload), nextInput)
    }
}


class PlusMinusLanguageTests {
    private val whitespaces = repeat(str(" "))
    private val numberTerm: Parser<NumberLiteral> = regex("\\d+").map { NumberLiteral(it.toInt()) }
    private val plusTerm: Parser<OperatorType> = inOrder(whitespaces, str("+"), whitespaces).map { Plus }
    private val minusTerm: Parser<OperatorType> = inOrder(whitespaces, str("-"), whitespaces).map { Minus }

    private val langExpr: Parser<Expression> = inOrder(numberTerm, repeat(inOrder(or(plusTerm, minusTerm), numberTerm)))
        .map { (number, rest) ->
            rest.fold(number as Expression) { expression, (operator, number) ->
                when (operator) {
                    Plus  -> PlusExpression(expression, number)
                    Minus -> MinusExpression(expression, number)
                }
            }
        }

    private enum class OperatorType { Plus, Minus }

    private sealed class Expression {
        data class NumberLiteral(val value: Int): Expression()
        data class PlusExpression(val left: Expression, val right: Expression): Expression()
        data class MinusExpression(val left: Expression, val right: Expression): Expression()
    }

    private fun Expression.eval(): Int =
        when (this) {
            is NumberLiteral   -> this.value
            is MinusExpression -> left.eval() - right.eval()
            is PlusExpression  -> left.eval() + right.eval()
        }

    @Test fun `123`() {
        val expression = langExpr.parse(Input("123"))?.payload!!
        expression shouldEqual NumberLiteral(123)
        expression.eval() shouldEqual 123
    }

    @Test fun `1 + 2`() {
        val expression = langExpr.parse(Input("1 + 2"))?.payload!!
        expression shouldEqual PlusExpression(
            NumberLiteral(1),
            NumberLiteral(2)
        )
        expression.eval() shouldEqual 3
    }

    @Test fun `1 + 2 + 3`() {
        val expression = langExpr.parse(Input("1 + 2 + 3"))?.payload!!
        expression shouldEqual PlusExpression(
            PlusExpression(
                NumberLiteral(1),
                NumberLiteral(2)
            ),
            NumberLiteral(3)
        )
        expression.eval() shouldEqual 6
    }

    @Test fun `1 + 2 - 3`() {
        val expression = langExpr.parse(Input("1 + 2 - 3"))?.payload!!
        expression shouldEqual MinusExpression(
            PlusExpression(
                NumberLiteral(1),
                NumberLiteral(2)
            ),
            NumberLiteral(3)
        )
        expression.eval() shouldEqual 0
    }

    @Test fun `invalid input`() {
        langExpr.parse(Input("abc")) shouldEqual null
        langExpr.parse(Input("+123")) shouldEqual null
        langExpr.parse(Input("-123")) shouldEqual null
    }

    @Test fun `partial input`() {
        langExpr.parse(Input("1 + 2 +")) shouldEqual Output(
            payload = PlusExpression(NumberLiteral(1), NumberLiteral(2)),
            input = Input(s = "1 + 2 +", offset = 5)
        )
        langExpr.parse(Input("1 ++ 2")) shouldEqual Output(
            payload = NumberLiteral(1),
            input = Input(s = "1 ++ 2", offset = 1)
        )
    }
}

private infix fun Any?.shouldEqual(expected: Any?) {
    assertEquals(expected = expected, actual = this)
}