package parser4k

import parser4k.commonparsers.token
import kotlin.test.Test

abstract class TestGrammar {
    val number = regex("\\d+").map(::Number)
    abstract val expr: Parser<Node>

    infix fun String.shouldParseAs(expected: String) = parseWith(expr).toString() shouldEqual expected

    interface Node

    class Number(private val value: String) : Node {
        override fun toString() = value
    }

    class Not(private val expression: Node) : Node {
        override fun toString() = "!($expression)"
    }

    class Field(private val expression: Node, private val name: String) : Node {
        override fun toString() = "($expression.$name)"
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

class DirectRightRecursion_OnUnaryOperator: TestGrammar() {
    override val expr: Parser<Node> = oneOf(
        inOrder(str("!"), ::expr.ref()).map { (_, it) -> Not(it) },
        number
    )

    @Test fun `it works`() {
        "123" shouldParseAs "123"
        "!123" shouldParseAs "!(123)"
        "!!123" shouldParseAs "!(!(123))"
    }
}

class RightRecursion_OnUnaryOperator: TestGrammar() {
    private val not = inOrder(str("!"), ref { expr }).map { (_, it) -> Not(it) }
    override val expr: Parser<Node> = oneOf(not, number)

    @Test fun `it works`() {
        "123" shouldParseAs "123"
        "!123" shouldParseAs "!(123)"
        "!!123" shouldParseAs "!(!(123))"
    }
}

class DirectLeftRecursion_OnUnaryOperator: TestGrammar() {
    override val expr: Parser<Node> = oneOf(
        inOrder(::expr.ref(), str(".foo")).mapLeftAssoc { (expr, _) -> Field(expr, "foo") },
        number
    )

    @Test fun `it works`() {
        "1" shouldParseAs "1"
        "1.foo" shouldParseAs "(1.foo)"
        "1.foo.foo" shouldParseAs "((1.foo).foo)"
    }
}

class LeftRecursion_OnUnaryOperator: TestGrammar() {
    private val foo = inOrder(ref { expr }, str(".foo")).mapLeftAssoc { (expr, _) -> Field(expr, "foo") }
    override val expr: Parser<Node> = oneOf(foo, number)

    @Test fun `it works`() {
        "1" shouldParseAs "1"
        "1.foo" shouldParseAs "(1.foo)"
        "1.foo.foo" shouldParseAs "((1.foo).foo)"
    }
}

class RightRecursion_OnBinaryOperator: TestGrammar() {
    private val power = inOrder(nonRecRef { expr }, str(" ^ "), ref { expr }).map(::Power.asBinary())
    override val expr: Parser<Node> = oneOf(power, number)

    @Test fun `it works`() {
        "1" shouldParseAs "1"
        "1 ^ 2" shouldParseAs "(1 ^ 2)"
        "1 ^ 2 ^ 3" shouldParseAs "(1 ^ (2 ^ 3))"
    }
}

class LeftRecursion_OnBinaryOperator: TestGrammar() {
    private val plus = inOrder(ref { expr }, str(" + "), ref { expr }).mapLeftAssoc(::Plus.asBinary())
    override val expr: Parser<Node> = oneOf(plus, number)

    @Test fun `it works`() {
        "1" shouldParseAs "1"
        "1 + 2" shouldParseAs "(1 + 2)"
        "1 + 2 + 3" shouldParseAs "((1 + 2) + 3)"
    }
}

class ParserAssociativityTests: TestGrammar() {
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
