package parser4k

import kotlin.test.assertEquals
import kotlin.test.fail

infix fun Any?.shouldEqual(expected: Any?) =
    assertEquals(expected = expected, actual = this)

fun Parser<*>.parseAllInputOrFail(s: String): Any? {
    val (payload, input) = parse(Input(s)) ?: fail("Couldn't parse '$s'")
    if (input.offset < input.value.length) {
        fail(
            "Input was not fully consumed:\n" +
            "$s\n" +
            " ".repeat(input.offset) + "^\n" +
            "payload = $payload"
        )
    }
    return payload
}
