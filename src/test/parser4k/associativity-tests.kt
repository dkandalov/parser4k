package parser4k

import parser4k.commonparsers.token
import kotlin.test.Test

abstract class TestGrammar {
    val number = regex("\\d+").map(::Number)
    abstract val expr: Parser<Node>

    infix fun String.shouldBeParsedAs(expected: String) = parseWith(expr).toString() shouldEqual expected

    interface Node

    class Number(private val value: String) : Node {
        override fun toString() = value
    }

    class PreIncrement(private val expression: Node) : Node {
        override fun toString() = "++($expression)"
    }

    class PreDecrement(private val expression: Node) : Node {
        override fun toString() = "--($expression)"
    }

    class Field(private val expression: Node, private val name: String) : Node {
        override fun toString() = "($expression.$name)"
    }

    class Plus(private val left: Node, private val right: Node) : Node {
        override fun toString() = "($left + $right)"
    }

    class Minus(private val left: Node, private val right: Node) : Node {
        override fun toString() = "($left - $right)"
    }

    class Power(private val left: Node, private val right: Node) : Node {
        override fun toString() = "($left ^ $right)"
    }

    class AccessByIndex(private val left: Node, private val right: Node) : Node {
        override fun toString() = "$left[$right]"
    }
}

class LeftAssociativityTests {
    @Test fun `single unary operator`() =
        object : TestGrammar() {
            val foo = inOrder(ref { expr }, str(".foo")).mapLeftAssoc { (expr, _) -> Field(expr, "foo") }
            override val expr: Parser<Node> = oneOf(
                foo,
                number
            )
        }.run {
            "1" shouldBeParsedAs "1"
            "1.foo" shouldBeParsedAs "(1.foo)"
            "1.foo.foo" shouldBeParsedAs "((1.foo).foo)"
        }

    @Test fun `two unary operators`() =
        object : TestGrammar() {
            val foo = inOrder(ref { expr }, str(".foo")).mapLeftAssoc { (expr, _) -> Field(expr, "foo") }
            val bar = inOrder(ref { expr }, str(".bar")).mapLeftAssoc { (expr, _) -> Field(expr, "bar") }
            override val expr: Parser<Node> = oneOf(
                foo,
                bar,
                number
            )
        }.run {
            "1" shouldBeParsedAs "1"

            "1.foo" shouldBeParsedAs "(1.foo)"
            "1.foo.foo" shouldBeParsedAs "((1.foo).foo)"

            "1.bar" shouldBeParsedAs "(1.bar)"
            "1.bar.bar" shouldBeParsedAs "((1.bar).bar)"

            "1.foo.bar" shouldBeParsedAs "((1.foo).bar)"
            "1.bar.foo" shouldBeParsedAs "((1.bar).foo)"
        }

    @Test fun `single binary operator`() =
        object : TestGrammar() {
            val plus = inOrder(ref { expr }, str(" + "), ref { expr }).mapLeftAssoc(::Plus.asBinary())
            override val expr: Parser<Node> = oneOf(
                plus,
                number
            )
        }.run {
            "1" shouldBeParsedAs "1"
            "1 + 2" shouldBeParsedAs "(1 + 2)"
            "1 + 2 + 3" shouldBeParsedAs "((1 + 2) + 3)"
        }

    @Test fun `two binary operators`() =
        object : TestGrammar() {
            val plus = inOrder(ref { expr }, str(" + "), ref { expr }).mapLeftAssoc(::Plus.asBinary())
            val minus = inOrder(ref { expr }, str(" - "), ref { expr }).mapLeftAssoc(::Minus.asBinary())
            override val expr: Parser<Node> = oneOf(
                plus,
                minus,
                number
            )
        }.run {
            "1" shouldBeParsedAs "1"

            "1 + 2" shouldBeParsedAs "(1 + 2)"
            "1 + 2 + 3" shouldBeParsedAs "((1 + 2) + 3)"

            "1 - 2" shouldBeParsedAs "(1 - 2)"
            "1 - 2 - 3" shouldBeParsedAs "((1 - 2) - 3)"

            "1 + 2 - 3" shouldBeParsedAs "((1 + 2) - 3)"
            "1 - 2 + 3" shouldBeParsedAs "((1 - 2) + 3)"
            "1 + 2 - 3 + 4" shouldBeParsedAs "(((1 + 2) - 3) + 4)"
        }
}

class RightAssociativity {
    @Test fun `single unary operator`() =
        object : TestGrammar() {
            val preIncrement = inOrder(str("++"), ref { expr }).map { (_, it) -> PreIncrement(it) }
            override val expr: Parser<Node> = oneOf(
                preIncrement,
                number
            )
        }.run {
            "123" shouldBeParsedAs "123"
            "++123" shouldBeParsedAs "++(123)"
            "++++123" shouldBeParsedAs "++(++(123))"
        }

    @Test fun `two unary operators`() =
        object : TestGrammar() {
            val preIncrement = inOrder(str("++"), ref { expr }).map { (_, it) -> PreIncrement(it) }
            val preDecrement = inOrder(str("--"), ref { expr }).map { (_, it) -> PreDecrement(it) }
            override val expr: Parser<Node> = oneOf(
                preIncrement,
                preDecrement,
                number
            )
        }.run {
            "123" shouldBeParsedAs "123"

            "++123" shouldBeParsedAs "++(123)"
            "++++123" shouldBeParsedAs "++(++(123))"

            "--123" shouldBeParsedAs "--(123)"
            "----123" shouldBeParsedAs "--(--(123))"

            "++--123" shouldBeParsedAs "++(--(123))"
            "--++123" shouldBeParsedAs "--(++(123))"
        }

    @Test fun `single binary operator`() =
        object : TestGrammar() {
            val power = inOrder(nonRecRef { expr }, str(" ^ "), ref { expr }).map(::Power.asBinary())
            override val expr: Parser<Node> = oneOf(
                power,
                number
            )
        }.run {
            "1" shouldBeParsedAs "1"
            "1 ^ 2" shouldBeParsedAs "(1 ^ 2)"
            "1 ^ 2 ^ 3" shouldBeParsedAs "(1 ^ (2 ^ 3))"
        }

    @Test fun `two binary operators`() =
        object : TestGrammar() {
            val power = inOrder(nonRecRef { expr }, str(" ^ "), ref { expr }).map(::Power.asBinary())
            override val expr: Parser<Node> = oneOf(
                power,
                number
            )
        }.run {
            "1" shouldBeParsedAs "1"
            "1 ^ 2" shouldBeParsedAs "(1 ^ 2)"
            "1 ^ 2 ^ 3" shouldBeParsedAs "(1 ^ (2 ^ 3))"
        }
}

class LeftAndRightAssociativityTests : TestGrammar() {
    private val plus = inOrder(nonRecRef { expr }, token("+"), ref { expr }).mapLeftAssoc(::Plus.asBinary())
    private val power = inOrder(nonRecRef { expr }, token("^"), ref { expr }).map(::Power.asBinary())
    override val expr: Parser<Node> = oneOfWithPrecedence(
        plus,
        power,
        number
    )

    @Test fun `it works`() {
        "123" shouldBeParsedAs "123"

        "1 + 2" shouldBeParsedAs "(1 + 2)"
        "1 + 2 + 3" shouldBeParsedAs "((1 + 2) + 3)"
        "1 + 2 + 3 + 4" shouldBeParsedAs "(((1 + 2) + 3) + 4)"

        "1 ^ 2" shouldBeParsedAs "(1 ^ 2)"
        "1 ^ 2 ^ 3" shouldBeParsedAs "(1 ^ (2 ^ 3))"
        "1 ^ 2 ^ 3 ^ 4" shouldBeParsedAs "(1 ^ (2 ^ (3 ^ 4)))"

        "1^2 + 3" shouldBeParsedAs "((1 ^ 2) + 3)"
        "1 + 2^3" shouldBeParsedAs "(1 + (2 ^ 3))"
        "1^2 + 3^4" shouldBeParsedAs "((1 ^ 2) + (3 ^ 4))"

        "1^2 + 3 + 4" shouldBeParsedAs "(((1 ^ 2) + 3) + 4)"
        "1 + 2^3 + 4" shouldBeParsedAs "((1 + (2 ^ 3)) + 4)"
        "1 + 2 + 3^4" shouldBeParsedAs "((1 + 2) + (3 ^ 4))"

        "1 + 2^3 + 4^5" shouldBeParsedAs "((1 + (2 ^ 3)) + (4 ^ 5))"
        "1^2 + 3 + 4^5" shouldBeParsedAs "(((1 ^ 2) + 3) + (4 ^ 5))"
        "1^2 + 3^4 + 5" shouldBeParsedAs "(((1 ^ 2) + (3 ^ 4)) + 5)"

        "1 + 2^3^4" shouldBeParsedAs "(1 + (2 ^ (3 ^ 4)))"
        "1^2^3 + 4" shouldBeParsedAs "((1 ^ (2 ^ 3)) + 4)"
    }
}

class ParserAssociativityAndNestedPrecedenceTests : TestGrammar() {
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
        "123" shouldBeParsedAs "123"
        "1 + 2" shouldBeParsedAs "(1 + 2)"
        "1 + 2 + 3" shouldBeParsedAs "((1 + 2) + 3)"
        "1 + 2 + 3 + 4" shouldBeParsedAs "(((1 + 2) + 3) + 4)"

        "123[0]" shouldBeParsedAs "123[0]"
        "123[0][1]" shouldBeParsedAs "123[0][1]"
        "123[1 + 2]" shouldBeParsedAs "123[(1 + 2)]"
        "123[1 + 2] + 3" shouldBeParsedAs "(123[(1 + 2)] + 3)"
    }
}
