package parser4k

fun <T> String.parseWith(parser: Parser<T>): T {
    val output = parser.parse(Input(this)) ?: throw NoMatchingParsers(this)
    if (output.nextInput.offset < output.nextInput.value.length) throw InputIsNotConsumed(output)
    return output.payload
}

sealed class ParsingError(override val message: String) : RuntimeException(message)

class NoMatchingParsers(override val message: String) : ParsingError(message)

class InputIsNotConsumed(override val message: String) : ParsingError(message) {
    constructor(output: Output<*>) : this(
        "\n" + // start new line after "parser4k.InputIsNotConsumed: "
        "${output.nextInput.value}\n" +
        " ".repeat(output.nextInput.offset) + "^\n" +
        "payload = ${output.payload}"
    )
}

operator fun <T> List<T>.component6(): T = this[5]
operator fun <T> List<T>.component7(): T = this[6]
operator fun <T> List<T>.component8(): T = this[7]
