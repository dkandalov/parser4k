package parser4k

import kotlin.test.Test

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
        abParser.parse(Input("__ab", offset = 2)) shouldEqual Output(List2("a", "b"), Input("__ab", offset = 4))

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