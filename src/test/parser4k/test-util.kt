package parser4k

import kotlin.test.assertEquals

infix fun Any?.shouldEqual(expected: Any?) =
    assertEquals(expected = expected, actual = this)

infix fun (() -> Any?).shouldFailWith(f: (ParsingError) -> Boolean) =
    try {
        this()
    } catch (e: ParsingError) {
        assert(f(e))
    }

infix fun (() -> Any?).shouldFailWithMessage(expectedMessage: String) =
    try {
        this()
    } catch (e: ParsingError) {
        e.message shouldEqual expectedMessage.trimMargin()
    }

