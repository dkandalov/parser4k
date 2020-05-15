package parser4k

import java.lang.RuntimeException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

fun str(s: String): Parser<String> = object : Parser<String> {
    override fun parse(input: Input): Output<String>? = input.run {
        val newOffset = offset + s.length
        if (newOffset > value.length) null
        else {
            val token = value.substring(offset, newOffset)
            if (token == s) Output(token, copy(offset = newOffset)) else null
        }
    }
}

fun regex(pattern: String) = object : Parser<String> {
    val regex = pattern.toRegex()

    override fun parse(input: Input): Output<String>? {
        val matchResult = regex.find(input.value, input.offset) ?: return null
        if (matchResult.range.first != input.offset) return null
        return Output(input.value.substring(matchResult.range), input.copy(offset = matchResult.range.last + 1))
    }
}

fun <T> repeat(parser: Parser<T>, atLeast: Int = 0, atMost: Int = Int.MAX_VALUE) = object : Parser<List<T>> {
    override fun parse(input: Input): Output<List<T>>? {
        val payload = ArrayList<T>()
        var nextInput = input
        while (true) {
            val output = parser.parse(nextInput) ?: break
            nextInput = output.input
            payload.add(output.payload)
            if (payload.size == atMost) break
        }
        return if (payload.size >= atLeast) Output(payload, nextInput) else null
    }
}

fun <T> zeroOrMore(parser: Parser<T>) = repeat(parser, atLeast = 0)

fun <T> oneOrMore(parser: Parser<T>) = repeat(parser, atLeast = 1)

fun <T> optional(parser: Parser<T>): Parser<T?> =
    repeat(parser, atLeast = 0, atMost = 1).map { it.firstOrNull() }

fun <T> oneOf(vararg parsers: Parser<T>): Parser<T> = oneOf(parsers.toList())

fun <T> oneOf(parsers: List<Parser<T>>) = object : Parser<T> {
    override fun parse(input: Input): Output<T>? {
        parsers.forEach { parser ->
            val output = parser.parse(input)
            if (output != null) return output
        }
        return null
    }
}

fun <T> oneOfWithPrecedence(vararg parsers: Parser<T>): Parser<T> = oneOfWithPrecedence(parsers.toList())

fun <T> oneOfWithPrecedence(parsers: List<Parser<T>>) = object : Parser<T> {
    val stack: LinkedList<Parser<T>> = LinkedList()

    override fun parse(input: Input): Output<T>? {
        val head = stack.peek()
        val filteredParsers =
            if (head == null || head is NestedPrecedence) parsers
            else parsers.drop(parsers.indexOf(head))

        filteredParsers.forEach { parser ->
            stack.push(parser)
            val output = parser.parse(input)
            stack.pop()
            if (output != null) return output
        }
        return null
    }
}

fun <T> Parser<T>.nestedPrecedence() = NestedPrecedence(this)

class NestedPrecedence<T>(private val parser: Parser<T>) : Parser<T> {
    override fun parse(input: Input) = parser.parse(input)
}

fun <T> ref(f: () -> Parser<T>) = object : Parser<T> {
    override fun parse(input: Input) = f().parse(input)
}

fun <T> leftRef(f: () -> Parser<T>) = object : Parser<T> {
    val offsets: HashSet<Int> = HashSet()

    override fun parse(input: Input): Output<T>? {
        if (!offsets.add(input.offset)) return null // Prevent stack overflow on left recursion
        val output = f().parse(input)
        offsets.remove(input.offset)
        return output
    }
}

fun <T, R> Parser<T>.map(f: (T) -> R) = object : Parser<R> {
    override fun parse(input: Input): Output<R>? {
        val (payload, nextInput) = this@map.parse(input) ?: return null
        return Output(f(payload), nextInput)
    }
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

fun <T> String.parseWith(parser: Parser<T>): T {
    val output = parser.parse(Input(this)) ?: throw NoMatchingParsers(this)
    if (output.input.offset < output.input.value.length) throw InputIsNotConsumed(output)
    return output.payload
}
