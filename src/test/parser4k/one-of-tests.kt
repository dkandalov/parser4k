package parser4k

import kotlin.test.Test

class OneOfParserTests {
    @Test fun `it works`() {
        val abParser = oneOf(str("a"), str("b"))

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