package parser4k

import java.util.*
import kotlin.collections.ArrayList

fun <T> Parser<T>.with(parserId: String, log: ParsingLog) = object : Parser<T> {
    override fun parse(input: Input): Output<T>? {
        log.before(parserId, input)
        val output = this@with.parse(input)
        log.after(parserId, output)
        return output
    }
}

class ParsingLog {
    private val idStack = LinkedList<String>()
    private val inputStack = LinkedList<Input>()
    private val events = ArrayList<ParsingEvent>()

    fun events(): List<ParsingEvent> = events

    internal fun before(parserId: String, input: Input) {
        idStack.push(parserId)
        inputStack.push(input)
        events.add(BeforeParsing(input, stackTrace()))
    }

    internal fun <T> after(parserId: String, output: Output<T>?) {
        events.add(AfterParsing(inputStack.peek(), output, stackTrace()))
        inputStack.pop()
        idStack.pop().let { id ->
            require(id == parserId) { "Expected id '$parserId' but was '$id'" }
        }
    }

    private fun stackTrace(): List<Pair<String, Int>> = idStack.zip(inputStack.map { it.offset }).asReversed()
}

fun List<ParsingEvent>.print() = forEach { println(it.toDebugString()) }

sealed class ParsingEvent
data class BeforeParsing(val input: Input, val stackTrace: List<Pair<String, Int>>) : ParsingEvent()
data class AfterParsing<T>(val input: Input, val output: Output<T>?, val stackTrace: List<Pair<String, Int>>) : ParsingEvent()

fun ParsingEvent.toDebugString() =
    when (this) {
        is BeforeParsing   -> "${input.string()} ${stackTrace.string()}"
        is AfterParsing<*> -> "${input.string()} ${stackTrace.string()} -- ${if (output == null) "X" else input.diff(output.input) }"
    }

private fun Input.diff(that: Input) = value.substring(this.offset, that.offset)

private fun List<Pair<String, Int>>.string() = joinToString(" ") { it.first + ":" + it.second }

private fun Input.string() = "\"$value\""
