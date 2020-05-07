package parser4k

import kotlin.test.Test

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
        str("foo").parse(Input("__foo", offset = 2)) shouldEqual Output("foo", Input("__foo", offset = 5))
    }
}
