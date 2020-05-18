package parser4k

import parser4k.CommonParsers.token
import parser4k.Expression.*
import kotlin.test.Test

class OutputCacheTests {
    private val logEvents = ArrayList<ParsingEvent>()
    private val log = ParsingLog { logEvents.add(it) }
    private val cache = OutputCache<Expression>()

    private val number = regex("\\d+").map { Number(it.toBigDecimal()) }.with("num", log).with(cache)
    private val minus = inOrder(ref { expr }, token("-"), ref { expr }).leftAssocAsBinary(::Minus).with("minus", log).with(cache)
    private val plus = inOrder(ref { expr }, token("+"), ref { expr }).leftAssocAsBinary(::Plus).with("plus", log).with(cache)

    private val expr: Parser<Expression> = oneOf(plus, minus, number).reset(cache)

    @Test fun `use each parser once at each input offset`() {
        expectMinimalLog { "1" shouldParseTo "1" }
        expectMinimalLog { "1 + 2" shouldParseTo "[1 + 2]" }
        expectMinimalLog { "1 - 2" shouldParseTo "[1 - 2]" }
        expectMinimalLog { "1 - 2 + 3" shouldParseTo "[[1 - 2] + 3]" }
    }

    @Test fun `log events after parsing a number`() {
        logEvents.clear()
        "123" shouldParseTo "123"

        logEvents.joinToString("\n") { it.toDebugString() } shouldEqual """
            "123" plus:0
            "123" plus:0 minus:0
            "123" plus:0 minus:0 num:0
            "123" plus:0 minus:0 num:0 -- 123
            "123" plus:0 minus:0 -- X
            "123" plus:0 -- X
        """.trimIndent()
    }

    private fun expectMinimalLog(f: () -> Unit) {
        logEvents.clear()
        f()
        val framesWithOutput = logEvents.filterIsInstance<AfterParsing<*>>().map { it.stackTrace.last() }
        framesWithOutput shouldEqual framesWithOutput.distinct()
    }

    private infix fun String.shouldParseTo(expected: String) = parseWith(expr).toExpressionString() shouldEqual expected
}