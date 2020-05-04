package parser4k

import parser4k.CommonParsers.token
import parser4k.Expression.Minus
import parser4k.Expression.Multiply
import parser4k.Expression.Number
import parser4k.Expression.Plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

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

interface Parser<out T> {
    fun parse(input: Input): Output<T>?
}

fun str(s: String) = object : Parser<String> {
    override fun parse(input: Input): Output<String>? {
        val (token, nextInput) = input.read(s.length) ?: return null
        return if (token != s) null else Output(token, nextInput)
    }
}

fun regex(pattern: String) = object : Parser<String> {
    val regex = pattern.toRegex()

    override fun parse(input: Input): Output<String>? {
        val matchResult = regex.find(input.s, input.offset) ?: return null
        if (matchResult.range.first != input.offset) return null
        return Output(input.s.substring(matchResult.range), input.copy(offset = matchResult.range.last + 1))
    }
}

fun <T> repeat(parser: Parser<T>, atLeast: Int = 0) = object : Parser<List<T>> {
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

fun <T> or(vararg parsers: Parser<T>) = object : Parser<T> {
    override fun parse(input: Input): Output<T>? {
        parsers.forEach { parser ->
            val output = parser.parse(input)
            if (output != null) return output
        }
        return null
    }
}

fun <T> orWithPrecedence(vararg parsers: Parser<T>) = object : Parser<T> {
    var index = 0

    override fun parse(input: Input): Output<T>? {
        parsers.drop(index).forEachIndexed { parserIndex, parser ->
            val lastIndex = index
            index = if (parser is ResetPrecedence) 0 else parserIndex

            val output = parser.parse(input)

            index = lastIndex
            if (output != null) return output
        }
        return null
    }
}

fun <T> Parser<T>.resetPrecedence() = ResetPrecedence(this)

class ResetPrecedence<T>(private val parser: Parser<T>) : Parser<T> {
    override fun parse(input: Input) = parser.parse(input)
}

fun <T1> inOrder(parser1: Parser<T1>) =
    object : Parser<List1<T1>> {
        override fun parse(input: Input): Output<List1<T1>>? {
            val (payload, nextInput) = parser1.parse(input) ?: return null
            return Output(List1(payload), nextInput)
        }
    }

fun <T1, T2> inOrder(parser1: Parser<T1>, parser2: Parser<T2>) =
    object : Parser<List2<T1, T2>> {
        override fun parse(input: Input): Output<List2<T1, T2>>? {
            val (payload, nextInput) = inOrder(parser1).parse(input) ?: return null
            val (lastPayload, lastInput) = parser2.parse(nextInput) ?: return null
            return Output(payload + lastPayload, lastInput)
        }
    }

fun <T1, T2, T3> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>) =
    object : Parser<List3<T1, T2, T3>> {
        override fun parse(input: Input): Output<List3<T1, T2, T3>>? {
            val (payload, nextInput) = inOrder(parser1, parser2).parse(input) ?: return null
            val (lastPayload, lastInput) = parser3.parse(nextInput) ?: return null
            return Output(payload + lastPayload, lastInput)
        }
    }

fun <T1, T2, T3, T4> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>) =
    object : Parser<List4<T1, T2, T3, T4>> {
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

fun <T, R> Parser<T>.map(f: (T) -> R) = object : Parser<R> {
    override fun parse(input: Input): Output<R>? {
        val (payload, nextInput) = this@map.parse(input) ?: return null
        return Output(f(payload), nextInput)
    }
}

fun <T> ref(f: () -> Parser<T>) = object : Parser<T> {
    override fun parse(input: Input) = f().parse(input)
}

fun <T> leftRef(f: () -> Parser<T>) = object : Parser<T> {
    val offsets: HashSet<Int> = HashSet()

    override fun parse(input: Input): Output<T>? {
        if (!offsets.add(input.offset)) return null // Prevent stack overflow on left recursion
        val output = f().parse(input)
        offsets.remove(input.offset)
        return output
    }
}


class StrParserTests {
    @Test fun `it works`() {
        // not enough input
        str("foo").parse(Input("")) shouldEqual null
        str("foo").parse(Input("f")) shouldEqual null
        str("foo").parse(Input("foo", offset = 1)) shouldEqual null

        // input mismatch
        str("foo").parse(Input("bar")) shouldEqual null
        str("foo").parse(Input("fo0")) shouldEqual null

        // match
        str("foo").parse(Input("foo")) shouldEqual Output("foo", Input("foo", offset = 3))
        str("foo").parse(Input("foo__")) shouldEqual Output("foo", Input("foo__", offset = 3))
        str("foo").parse(Input("_foo_", offset = 1)) shouldEqual Output("foo", Input("_foo_", offset = 4))
        str("foo").parse(Input("__foo", offset = 2),) shouldEqual Output("foo", Input("__foo", offset = 5))
    }
}

class RegexParserTests {
    @Test fun `it works`() {
        // not enough input
        regex("a").parse(Input("")) shouldEqual null
        regex("a+").parse(Input("")) shouldEqual null
        regex("a").parse(Input("foo", offset = 3)) shouldEqual null

        // input mismatch
        regex("a").parse(Input("b")) shouldEqual null
        regex("a").parse(Input("ba")) shouldEqual null
        regex("[abc]").parse(Input("zzz")) shouldEqual null

        // match
        regex("a").parse(Input("a")) shouldEqual Output("a", Input("a", offset = 1))
        regex("a*").parse(Input("a")) shouldEqual Output("a", Input("a", offset = 1))
        regex("a*").parse(Input("aaa")) shouldEqual Output("aaa", Input("aaa", offset = 3))
        regex("a").parse(Input("abc")) shouldEqual Output("a", Input("abc", offset = 1))
        regex("[abc]").parse(Input("abc", offset = 0)) shouldEqual Output("a", Input("abc", offset = 1))
        regex("[abc]").parse(Input("abc", offset = 1)) shouldEqual Output("b", Input("abc", offset = 2))
        regex("[abc]").parse(Input("abc", offset = 2),) shouldEqual Output("c", Input("abc", offset = 3))
    }
}

class RepeatParserTests {
    @Test fun `it works`() {
        // not enough input
        repeat(str("a"), atLeast = 1).parse(Input("")) shouldEqual null
        repeat(str("a"), atLeast = 1).parse(Input("a", offset = 1)) shouldEqual null
        repeat(str("a"), atLeast = 2).parse(Input("a")) shouldEqual null

        // input mismatch
        repeat(str("a"), atLeast = 1).parse(Input("b")) shouldEqual null

        // match
        repeat(str("a")).parse(Input("")) shouldEqual Output(emptyList<String>(), Input(""))
        repeat(str("a")).parse(Input("b")) shouldEqual Output(emptyList<String>(), Input("b"))
        repeat(str("a")).parse(Input("aaa")) shouldEqual Output(listOf("a", "a", "a"), Input("aaa", offset = 3))
    }
}

class OrParserTests {
    @Test fun `it works`() {
        val abParser = or(str("a"), str("b"))

        // not enough input
        abParser.parse(Input("")) shouldEqual null
        abParser.parse(Input("a", offset = 1)) shouldEqual null
        abParser.parse(Input("b", offset = 1)) shouldEqual null

        // input mismatch
        abParser.parse(Input("c")) shouldEqual null

        // match
        abParser.parse(Input("ab")) shouldEqual Output("a", Input("ab", offset = 1))
        abParser.parse(Input("ba")) shouldEqual Output("b", Input("ba", offset = 1))
    }
}

class InOrderParserTests {
    @Test fun `it works`() {
        val abParser = inOrder(str("a"), str("b"))

        // not enough input
        abParser.parse(Input("")) shouldEqual null
        abParser.parse(Input("a")) shouldEqual null
        abParser.parse(Input("ab", offset = 1)) shouldEqual null

        // input mismatch
        abParser.parse(Input("foo")) shouldEqual null
        abParser.parse(Input("aa")) shouldEqual null

        // match
        abParser.parse(Input("ab")) shouldEqual Output(List2("a", "b"), Input("ab", offset = 2))
        abParser.parse(Input("ab__")) shouldEqual Output(List2("a", "b"), Input("ab__", offset = 2))
        abParser.parse(Input("_ab_", offset = 1)) shouldEqual Output(List2("a", "b"), Input("_ab_", offset = 3))
        abParser.parse(Input("__ab", offset = 2),) shouldEqual Output(List2("a", "b"), Input("__ab", offset = 4))

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

sealed class Expression {
    data class Number(val value: Int) : Expression()
    data class Plus(val left: Expression, val right: Expression) : Expression()
    data class Minus(val left: Expression, val right: Expression) : Expression()
    data class Multiply(val left: Expression, val right: Expression) : Expression()
}

object CommonParsers {
    val whitespaces: Parser<String> = regex("[ \\t\\r\\n]*")

    fun token(s: String): Parser<String> =
        inOrder(whitespaces, str(s), whitespaces).map { (_, op, _) -> op }
}

class PlusMinusParserTests {
    private val numberTerm = regex("\\d+").map { Number(it.toInt()) }

    private val expression = inOrder(numberTerm, repeat(inOrder(or(token("+"), token("-")), numberTerm)))
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
            input = Input(s = "1 + 2 +", offset = 5)
        )
        expression.parse(Input("1 ++ 2")) shouldEqual Output(
            payload = Number(1),
            input = Input(s = "1 ++ 2", offset = 1)
        )
    }

    private infix fun String.shouldParseTo(expected: Expression) =
        assertEquals(expected, expression.parseAllInputOrFail(this))
}

class PlusMinusWithRecursionParserTests {
    private val numberTerm = regex("\\d+").map { Number(it.toInt()) }

    private val plusExpression = inOrder(leftRef { expression }, token("+"), ref { expression })
        .map { (left, _, right) -> Plus(left, right) }

    private val minusExpression = inOrder(leftRef { expression }, token("-"), ref { expression })
        .map { (left, _, right) -> Minus(left, right) }

    private val expression: Parser<Expression> = or(minusExpression, plusExpression, numberTerm)

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
        assertEquals(expected, expression.parseAllInputOrFail(this))
}

class PlusMultiplyParserTests {
    private val numberTerm = regex("\\d+").map { Number(it.toInt()) }

    private val parenExpression = inOrder(token("("), ref { expression }, token(")"))
        .map { (_, expression, _) -> expression }

    private val multiplyExpression = inOrder(leftRef { expression }, token("*"), ref { expression })
        .map { (left, _, right) -> Multiply(left, right) }

    private val plusExpression = inOrder(leftRef { expression }, token("+"), ref { expression })
        .map { (left, _, right) -> Plus(left, right) }

    private val expression: Parser<Expression> = orWithPrecedence(
        plusExpression,
        multiplyExpression,
        parenExpression.resetPrecedence(),
        numberTerm
    )

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
        "1 * (2 + 3)" shouldParseTo Multiply(
            Number(1),
            Plus(Number(2), Number(3))
        )
        "1 * (2 + 3) + 4" shouldParseTo Plus(
            Multiply(
                Number(1),
                Plus(Number(2), Number(3))
            ),
            Number(4)
        )
    }

    private infix fun String.shouldParseTo(expected: Expression) =
        assertEquals(expected, expression.parseAllInputOrFail(this))
}

private infix fun Any?.shouldEqual(expected: Any?) =
    assertEquals(expected = expected, actual = this)

private fun Parser<*>.parseAllInputOrFail(s: String): Any? {
    val (payload, input) = parse(Input(s)) ?: fail("Couldn't parse '$s'")
    if (input.offset < input.s.length) {
        fail(
            "Input was not fully consumed:\n" +
                "$s\n" +
                " ".repeat(input.offset) + "^\n" +
                "payload = $payload"
        )
    }
    return payload
}
