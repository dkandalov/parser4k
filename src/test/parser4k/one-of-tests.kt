package parser4k

import kotlin.test.Test

class OneOfParserTests {
    @Test fun `it works`() {
        val abParser = oneOf(str("a"), str("b"))

        // not enough input
        abParser.invoke(Input("")) shouldEqual null
        abParser.invoke(Input("a", offset = 1)) shouldEqual null
        abParser.invoke(Input("b", offset = 1)) shouldEqual null

        // input mismatch
        abParser.invoke(Input("c")) shouldEqual null

        // match
        abParser.invoke(Input("ab")) shouldEqual Output("a", Input("ab", offset = 1))
        abParser.invoke(Input("ba")) shouldEqual Output("b", Input("ba", offset = 1))
    }
}