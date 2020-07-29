package parser4k.commonparsers

import parser4k.*
import parser4k.commonparsers.Tokens.whitespace

object Tokens {
    val whitespace: Parser<String> = oneOf(' ', '\t', '\r', '\n')
    val digit: Parser<String> = oneOf('0'..'9')
    val letter: Parser<String> = oneOf(oneOf('a'..'z'), oneOf('A'..'Z'))

    val integer: Parser<String> = oneOrMore(digit).map { it.joinToString("") }

    val number: Parser<String> = inOrder(oneOrMore(digit), optional(inOrder(str("."), oneOrMore(digit))))
        .map { (digits, optional) ->
            digits.joinToString("") + (optional?.let { it.value1 + it.value2.joinToString("") } ?: "")
        }

    val identifier: Parser<String> = inOrder(letter, repeat(oneOf(letter, digit, oneOf('$', '_'))))
        .map { (letter, lettersAndDigits) -> letter + lettersAndDigits.joinToString("") }

    val string: Parser<String> = inOrder(str("\""), repeat(oneOf(str("\\\""), noneOf('"', '\n', '\r'))), str("\""))
        .map { (_, it, _) -> it.joinToString("") }
}

fun token(s: String): Parser<String> =
    inOrder(zeroOrMore(whitespace), str(s), zeroOrMore(whitespace)).skipWrapper()

fun <T> Parser<T>.joinedWith(separator: Parser<*>): Parser<List<T>> =
    optional(inOrder(this, repeat(inOrder(separator, this))))
        .map { optional ->
            if (optional == null) emptyList()
            else {
                val (head, tail) = optional
                listOf(head) + tail.map { it.value2 }
            }
        }