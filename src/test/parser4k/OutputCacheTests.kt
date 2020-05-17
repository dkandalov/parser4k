package parser4k

import kotlin.test.Test

class OutputCacheTests {
    private val log = ParsingLog()
    private val cache = OutputCache<Expression>()

    private val number = CommonParsers.number.map { Expression.Number(it.toBigDecimal()) }.with("num", log).with(cache)
    private val minus = inOrder(ref { expr }, CommonParsers.token("-"), ref { expr }).leftAssocAsBinary(Expression::Minus).with("minus", log).with(cache)
    private val plus = inOrder(ref { expr }, CommonParsers.token("+"), ref { expr }).leftAssocAsBinary(Expression::Plus).with("plus", log).with(cache)

    private val expr: Parser<Expression> = oneOf(plus, minus, number).reset(cache)

    @Test fun `use each parser once at each input offset`() {
        expectMinimalLog { "1" shouldParseTo "1" }
        expectMinimalLog { "1 + 2" shouldParseTo "[1 + 2]" }
        expectMinimalLog { "1 - 2" shouldParseTo "[1 - 2]" }
        expectMinimalLog { "1 - 2 + 3" shouldParseTo "[[1 - 2] + 3]" }
    }

    @Test fun `log events after parsing a number`() {
        log.clear()
        "123" shouldParseTo "123"

        log.events().joinToString("\n") { it.toDebugString() } shouldEqual """
            "123" plus:0
            "123" plus:0 minus:0
            "123" plus:0 minus:0 num:0
            "123" plus:0 minus:0 num:0 -- 123
            "123" plus:0 minus:0 -- X
            "123" plus:0 -- X
        """.trimIndent()
    }

    private fun expectMinimalLog(f: () -> Unit) {
        log.clear()
        f()
        val framesWithOutput = log.events().filterIsInstance<AfterParsing<*>>().map { it.stackTrace.last() }
        framesWithOutput shouldEqual framesWithOutput.distinct()
    }

    private infix fun String.shouldParseTo(expected: String) = parseWith(expr).toExpressionString() shouldEqual expected
}