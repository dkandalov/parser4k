package parser4k

import java.util.*

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

    fun before(parserId: String, input: Input) {
        idStack.push(parserId)
        inputStack.push(input)
        println("${input.string()}: ${stackTrace()}")
    }

    fun <T> after(parserId: String, output: Output<T>?) {
        val input = inputStack.peek()
        val s =
            if (output == null) "X"
            else {
                val consumedInput = input.value.substring(input.offset, output.input.offset)
                "$consumedInput $ ${output.payload}"
            }
        println("${input.string()}: ${stackTrace()}: $s")

        inputStack.pop()
        idStack.pop().let { id ->
            require(id == parserId) { "Expected id '$parserId' but was '$id'" }
        }
    }

    private fun Input.string() = "Input($value, $offset)"

    private fun stackTrace(): String =
        idStack.zip(inputStack).asReversed().joinToString(" ") { (id, input) -> id + input.offset }
}
