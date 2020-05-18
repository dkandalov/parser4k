package parser4k

import parser4k.CommonParsers.token
import parser4k.Expression.*
import kotlin.test.Test

class ParserAssociativityTests {
    private val number = regex("\\d+").map { Number(it.toBigDecimal()) }

    private val plus = inOrder(nonRecRef { expr }, token("+"), ref { expr })
        .leftAssoc { (left, _, right) -> Plus(left, right) }

    private val power = inOrder(nonRecRef { expr }, token("^"), ref { expr })
        .map { (left, _, right) -> Power(left, right) }

    private val expr: Parser<Expression> = oneOfWithPrecedence(plus, power, number)

    @Test fun `it works`() {
        "123" shouldParseTo "123"

        "1 + 2" shouldParseTo "[1 + 2]"
        "1 + 2 + 3" shouldParseTo "[[1 + 2] + 3]"
        "1 + 2 + 3 + 4" shouldParseTo "[[[1 + 2] + 3] + 4]"

        "1 ^ 2" shouldParseTo "[1 ^ 2]"
        "1 ^ 2 ^ 3" shouldParseTo "[1 ^ [2 ^ 3]]"
        "1 ^ 2 ^ 3 ^ 4" shouldParseTo "[1 ^ [2 ^ [3 ^ 4]]]"

        "1^2 + 3" shouldParseTo "[[1 ^ 2] + 3]"
        "1 + 2^3" shouldParseTo "[1 + [2 ^ 3]]"
        "1^2 + 3^4" shouldParseTo "[[1 ^ 2] + [3 ^ 4]]"

        "1^2 + 3 + 4" shouldParseTo "[[[1 ^ 2] + 3] + 4]"
        "1 + 2^3 + 4" shouldParseTo "[[1 + [2 ^ 3]] + 4]"
        "1 + 2 + 3^4" shouldParseTo "[[1 + 2] + [3 ^ 4]]"

        "1 + 2^3 + 4^5" shouldParseTo "[[1 + [2 ^ 3]] + [4 ^ 5]]"
        "1^2 + 3 + 4^5" shouldParseTo "[[[1 ^ 2] + 3] + [4 ^ 5]]"
        "1^2 + 3^4 + 5" shouldParseTo "[[[1 ^ 2] + [3 ^ 4]] + 5]"

        "1 + 2^3^4" shouldParseTo "[1 + [2 ^ [3 ^ 4]]]"
        "1^2^3 + 4" shouldParseTo "[[1 ^ [2 ^ 3]] + 4]"
    }

    private infix fun String.shouldParseTo(expected: String) = parseWith(expr).toExpressionString() shouldEqual expected
}