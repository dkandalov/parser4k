package parser4k

object CommonParsers {
    val whitespace: Parser<String> = regex("[ \\t\\r\\n]")
    val digit: Parser<String> = regex("[0-9]")
    val letter: Parser<String> = regex("[a-zA-Z\$_]")

    val integer: Parser<String> = oneOrMore(digit).map { it.joinToString("") }

    val number: Parser<String> = inOrder(oneOrMore(digit), optional(inOrder(str("."), oneOrMore(digit))))
        .map { (digits, optional) ->
            digits.joinToString("") + (optional?.let { it.value1 + it.value2.joinToString("") } ?: "")
        }

    val identifier: Parser<String> = inOrder(letter, repeat(oneOf(letter, digit)))
        .map { (letter, lettersAndDigits) -> letter + lettersAndDigits.joinToString("") }

    val string: Parser<String> = inOrder(str("\""), repeat(oneOf(str("\\\""), regex("[^\"\n\r]"))), str("\""))
        .map { (_, it, _) -> it.joinToString("") }

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
}