package parser4k

import parser4k.commonparsers.token
import kotlin.test.Test

abstract class TestGrammar {
    abstract val expr: Parser<Node>

    infix fun String.shouldParseAs(expected: String) = parseWith(expr).toString() shouldEqual expected

    interface Node

    class Number(private val value: String) : Node {
        override fun toString() = value
    }

    class Plus(private val left: Node, private val right: Node) : Node {
        override fun toString() = "($left + $right)"
    }

    class Power(private val left: Node, private val right: Node) : Node {
        override fun toString() = "($left ^ $right)"
    }

    class AccessByIndex(private val left: Node, private val right: Node) : Node {
        override fun toString() = "$left[$right]"
    }
}

class ParserAssociativityTests: TestGrammar() {
    private val number = regex("\\d+").map(::Number)
    private val plus = inOrder(nonRecRef { expr }, token("+"), ref { expr }).mapLeftAssoc(::Plus.asBinary())
    private val power = inOrder(nonRecRef { expr }, token("^"), ref { expr }).map(::Power.asBinary())
    override val expr: Parser<Node> = oneOfWithPrecedence(
        plus,
        power,
        number
    )

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
}

class ParserAssociativityAndNestedPrecedenceTests: TestGrammar() {
    private val number = regex("\\d+").map(::Number)

    private val plus = inOrder(ref { expr }, token("+"), ref { expr })
        .mapLeftAssoc(::Plus.asBinary())

    private val accessByIndex = inOrder(ref { expr }, token("["), ref { expr }, token("]"))
        .mapLeftAssoc { (left, _, right, _) -> AccessByIndex(left, right) }

    override val expr: Parser<Node> = oneOfWithPrecedence(
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
}
