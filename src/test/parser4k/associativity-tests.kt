package parser4k

import parser4k.commonparsers.token
import kotlin.test.Test

class ParserAssociativityTests {
    private val number = regex("\\d+").map(::Number)
    private val plus = inOrder(nonRecRef { expr }, token("+"), ref { expr }).mapLeftAssoc(::Plus.asBinary())
    private val power = inOrder(nonRecRef { expr }, token("^"), ref { expr }).map(::Power.asBinary())
    private val expr: Parser<Node> = oneOfWithPrecedence(plus, power, number)

    @Test fun `it works`() {
        "123" shouldParseAs "123"

        "1 + 2" shouldParseAs "(1 + 2)"
        "1 + 2 + 3" shouldParseAs "((1 + 2) + 3)"
        "1 + 2 + 3 + 4" shouldParseAs "(((1 + 2) + 3) + 4)"

        "1 ^ 2" shouldParseAs "(1 ^ 2)"
        "1 ^ 2 ^ 3" shouldParseAs "(1 ^ (2 ^ 3))"
        "1 ^ 2 ^ 3 ^ 4" shouldParseAs "(1 ^ (2 ^ (3 ^ 4)))"

        "1^2 + 3" shouldParseAs "((1 ^ 2) + 3)"
        "1 + 2^3" shouldParseAs "(1 + (2 ^ 3))"
        "1^2 + 3^4" shouldParseAs "((1 ^ 2) + (3 ^ 4))"

        "1^2 + 3 + 4" shouldParseAs "(((1 ^ 2) + 3) + 4)"
        "1 + 2^3 + 4" shouldParseAs "((1 + (2 ^ 3)) + 4)"
        "1 + 2 + 3^4" shouldParseAs "((1 + 2) + (3 ^ 4))"

        "1 + 2^3 + 4^5" shouldParseAs "((1 + (2 ^ 3)) + (4 ^ 5))"
        "1^2 + 3 + 4^5" shouldParseAs "(((1 ^ 2) + 3) + (4 ^ 5))"
        "1^2 + 3^4 + 5" shouldParseAs "(((1 ^ 2) + (3 ^ 4)) + 5)"

        "1 + 2^3^4" shouldParseAs "(1 + (2 ^ (3 ^ 4)))"
        "1^2^3 + 4" shouldParseAs "((1 ^ (2 ^ 3)) + 4)"
    }

    private infix fun String.shouldParseAs(expected: String) = parseWith(expr).toString() shouldEqual expected

    private interface Node

    private class Number(val value: String) : Node {
        override fun toString() = value
    }

    private class Plus(val left: Node, val right: Node) : Node {
        override fun toString() = "($left + $right)"
    }

    private class Power(val left: Node, val right: Node) : Node {
        override fun toString() = "($left ^ $right)"
    }
}

class ParserAssociativityAndNestedPrecedenceTests {
    private val number = regex("\\d+").map(::Number)

    private val plus = inOrder(ref { expr }, token("+"), ref { expr })
        .mapLeftAssoc(::Plus.asBinary())

    private val accessByIndex = inOrder(ref { expr }, token("["), ref { expr }, token("]"))
        .mapLeftAssoc { (left, _, right, _) -> AccessByIndex(left, right) }

    private val expr: Parser<Node> = oneOfWithPrecedence(
        plus,
        accessByIndex.nestedPrecedence(),
        number
    )

    @Test fun `it works`() {
        "123" shouldParseAs "123"
        "1 + 2" shouldParseAs "(1 + 2)"
        "1 + 2 + 3" shouldParseAs "((1 + 2) + 3)"
        "1 + 2 + 3 + 4" shouldParseAs "(((1 + 2) + 3) + 4)"

        "123[0]" shouldParseAs "123[0]"
        "123[0][1]" shouldParseAs "123[0][1]"
        "123[1 + 2]" shouldParseAs "123[(1 + 2)]"
        "123[1 + 2] + 3" shouldParseAs "(123[(1 + 2)] + 3)"
    }

    private infix fun String.shouldParseAs(expected: String) = parseWith(expr).toString() shouldEqual expected

    private interface Node

    private class Number(val value: String) : Node {
        override fun toString() = value
    }

    private class Plus(val left: Node, val right: Node) : Node {
        override fun toString() = "($left + $right)"
    }

    private class AccessByIndex(val left: Node, val right: Node) : Node {
        override fun toString() = "$left[$right]"
    }
}
