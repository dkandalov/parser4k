package parser4k

import org.junit.Test

class LogTests {
    private val log = ParsingLog()

    private val boolean = oneOf(str("true"), str("false")).with("boolean", log)

    private val and = inOrder(
        nonRecRef { expr }.with("left", log),
        str(" && "),
        ref { expr }.with("right", log)
    ).with("and", log)

    private val or = inOrder(
        nonRecRef { expr }.with("left", log),
        str(" || "),
        ref { expr }.with("right", log)
    ).with("or", log)

    private val expr: Parser<Any> = oneOfWithPrecedence(or, and, boolean)

    @Test fun `boolean literal`() {
        "true || false".parseWith(expr) shouldEqual List3("true", " || ", "false")
        log.events().joinToString("\n") { it.toDebugString() } shouldEqual """
            "true || false" or:0
            "true || false" or:0 left:0
            "true || false" or:0 left:0 or:0
            "true || false" or:0 left:0 or:0 left:0
            "true || false" or:0 left:0 or:0 left:0 -- X
            "true || false" or:0 left:0 or:0 -- X
            "true || false" or:0 left:0 and:0
            "true || false" or:0 left:0 and:0 left:0
            "true || false" or:0 left:0 and:0 left:0 and:0
            "true || false" or:0 left:0 and:0 left:0 and:0 left:0
            "true || false" or:0 left:0 and:0 left:0 and:0 left:0 -- X
            "true || false" or:0 left:0 and:0 left:0 and:0 -- X
            "true || false" or:0 left:0 and:0 left:0 boolean:0
            "true || false" or:0 left:0 and:0 left:0 boolean:0 -- true
            "true || false" or:0 left:0 and:0 left:0 -- true
            "true || false" or:0 left:0 and:0 -- X
            "true || false" or:0 left:0 boolean:0
            "true || false" or:0 left:0 boolean:0 -- true
            "true || false" or:0 left:0 -- true
            "true || false" or:0 right:8
            "true || false" or:0 right:8 or:8
            "true || false" or:0 right:8 or:8 left:8
            "true || false" or:0 right:8 or:8 left:8 or:8
            "true || false" or:0 right:8 or:8 left:8 or:8 left:8
            "true || false" or:0 right:8 or:8 left:8 or:8 left:8 -- X
            "true || false" or:0 right:8 or:8 left:8 or:8 -- X
            "true || false" or:0 right:8 or:8 left:8 and:8
            "true || false" or:0 right:8 or:8 left:8 and:8 left:8
            "true || false" or:0 right:8 or:8 left:8 and:8 left:8 and:8
            "true || false" or:0 right:8 or:8 left:8 and:8 left:8 and:8 left:8
            "true || false" or:0 right:8 or:8 left:8 and:8 left:8 and:8 left:8 -- X
            "true || false" or:0 right:8 or:8 left:8 and:8 left:8 and:8 -- X
            "true || false" or:0 right:8 or:8 left:8 and:8 left:8 boolean:8
            "true || false" or:0 right:8 or:8 left:8 and:8 left:8 boolean:8 -- false
            "true || false" or:0 right:8 or:8 left:8 and:8 left:8 -- false
            "true || false" or:0 right:8 or:8 left:8 and:8 -- X
            "true || false" or:0 right:8 or:8 left:8 boolean:8
            "true || false" or:0 right:8 or:8 left:8 boolean:8 -- false
            "true || false" or:0 right:8 or:8 left:8 -- false
            "true || false" or:0 right:8 or:8 -- X
            "true || false" or:0 right:8 and:8
            "true || false" or:0 right:8 and:8 left:8
            "true || false" or:0 right:8 and:8 left:8 and:8
            "true || false" or:0 right:8 and:8 left:8 and:8 left:8
            "true || false" or:0 right:8 and:8 left:8 and:8 left:8 -- X
            "true || false" or:0 right:8 and:8 left:8 and:8 -- X
            "true || false" or:0 right:8 and:8 left:8 boolean:8
            "true || false" or:0 right:8 and:8 left:8 boolean:8 -- false
            "true || false" or:0 right:8 and:8 left:8 -- false
            "true || false" or:0 right:8 and:8 -- X
            "true || false" or:0 right:8 boolean:8
            "true || false" or:0 right:8 boolean:8 -- false
            "true || false" or:0 right:8 -- false
            "true || false" or:0 -- true || false
        """.trimIndent()
    }
}