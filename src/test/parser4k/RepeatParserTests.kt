package parser4k

import kotlin.test.Test

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
        repeat(str("a"), atLeast = 1).parse(Input("aaa")) shouldEqual Output(listOf("a", "a", "a"), Input("aaa", offset = 3))
        repeat(str("a"), atLeast = 2).parse(Input("aaa")) shouldEqual Output(listOf("a", "a", "a"), Input("aaa", offset = 3))
        repeat(str("a"), atMost = 1).parse(Input("aaa")) shouldEqual Output(listOf("a"), Input("aaa", offset = 1))
        repeat(str("a"), atMost = 2).parse(Input("aaa")) shouldEqual Output(listOf("a", "a"), Input("aaa", offset = 2))
        repeat(str("a"), atMost = 3).parse(Input("aaa")) shouldEqual Output(listOf("a", "a", "a"), Input("aaa", offset = 3))
    }
}