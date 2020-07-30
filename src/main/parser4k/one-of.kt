package parser4k

import java.util.*


fun <T> oneOf(vararg parsers: Parser<T>): Parser<T> = oneOf(parsers.toList())

fun <T> oneOf(parsers: List<Parser<T>>) = object : Parser<T> {
    override fun invoke(input: Input): Output<T>? {
        parsers.forEach { parser ->
            val output = parser.invoke(input)
            if (output != null) return output
        }
        return null
    }
}

fun <T> oneOfLongest(vararg parsers: Parser<T>): Parser<T> = nonRecursive(object : Parser<T> {
    override fun invoke(input: Input) =
        parsers.mapNotNull { it.invoke(input) }.maxBy { it.nextInput.offset }
})

fun <T> oneOfWithPrecedence(vararg parsers: Parser<T>): Parser<T> = oneOfWithPrecedence(parsers.toList())

fun <T> oneOfWithPrecedence(parsers: List<Parser<T>>) = object : Parser<T> {
    val stack: LinkedList<Parser<T>> = LinkedList()

    override fun invoke(input: Input): Output<T>? {
        val prevParser = stack.peek()
        val prevParserIndex = parsers.indexOf(prevParser)
        val isNestedPrecedence = prevParser == null || prevParser is NestedPrecedence
        val filteredParsers =
            if (isNestedPrecedence) parsers
            else parsers.drop(parsers.indexOf(prevParser))

        filteredParsers.forEach { parser ->
            stack.push(parser)
            val parserIndex = parsers.indexOf(parser)
            val output =
                if (prevParserIndex < parserIndex || (isNestedPrecedence && prevParser != parser)) {
                    parser.invoke(input.copy(leftPayload = null))
                } else {
                    parser.invoke(input)
                }
            stack.pop()
            if (output != null) return output
        }
        return null
    }
}

fun <T> Parser<T>.nestedPrecedence() = NestedPrecedence(this)

class NestedPrecedence<T>(private val parser: Parser<T>) : Parser<T> {
    override fun invoke(input: Input) = parser.invoke(input)
}
