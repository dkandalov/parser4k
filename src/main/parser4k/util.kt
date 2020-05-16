package parser4k

fun <T> String.parseWith(parser: Parser<T>): T {
    val output = parser.parse(Input(this)) ?: throw NoMatchingParsers(this)
    if (output.input.offset < output.input.value.length) throw InputIsNotConsumed(output)
    return output.payload
}

sealed class ParsingError(override val message: String) : RuntimeException(message)

class NoMatchingParsers(override val message: String) : ParsingError(message)

class InputIsNotConsumed(override val message: String) : ParsingError(message) {
    constructor(output: Output<*>) : this(
        "\n" + // start new line after "parser4k.InputIsNotConsumed: "
        "${output.input.value}\n" +
        " ".repeat(output.input.offset) + "^\n" +
        "payload = ${output.payload}"
    )
}
