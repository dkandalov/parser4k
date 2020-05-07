package parser4k

import kotlin.test.Test

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
        regex("[abc]").parse(Input("abc", offset = 2)) shouldEqual Output("c", Input("abc", offset = 3))
    }
}