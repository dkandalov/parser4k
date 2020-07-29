package parser4k

import kotlin.test.Test

class StringParserTests {
    @Test fun `it works`() {
        val parser = str("foo")

        // not enough input
        parser.parse(Input("")) shouldEqual null
        parser.parse(Input("f")) shouldEqual null
        parser.parse(Input("foo", offset = 1)) shouldEqual null

        // input mismatch
        parser.parse(Input("bar")) shouldEqual null
        parser.parse(Input("fo0")) shouldEqual null

        // match
        parser.parse(Input("foo")) shouldEqual Output("foo", Input("foo", offset = 3))
        parser.parse(Input("foo__")) shouldEqual Output("foo", Input("foo__", offset = 3))
        parser.parse(Input("_foo_", offset = 1)) shouldEqual Output("foo", Input("_foo_", offset = 4))
        parser.parse(Input("__foo", offset = 2)) shouldEqual Output("foo", Input("__foo", offset = 5))
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
        regex("[abc]").parse(Input("abc", offset = 2)) shouldEqual Output("c", Input("abc", offset = 3))
    }
}