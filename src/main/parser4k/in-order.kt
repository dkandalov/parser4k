@file:Suppress("DuplicatedCode")

package parser4k

fun <T1, T3, R> ((T1, T3) -> R).asBinary() = { list: List3<T1, *, T3> ->
    this(list.value1, list.value3)
}

fun <T> Parser<*>.skip(): Parser<T> = object : Parser<T> {
    override fun parse(input: Input): Output<T>? {
        val (_, nextInput) = this@skip.parse(input) ?: return null
        @Suppress("UNCHECKED_CAST")
        return Output(null as T, nextInput)
    }
}

class InOrder<T>(val parsers: List<Parser<T>>) : Parser<List<T>> {
    override fun parse(input: Input): Output<List<T>>? {
        val payload = ArrayList<T>(parsers.size)
        var nextInput = input
        parsers.forEach { parser ->
            val output = parser.parse(nextInput) ?: return null
            nextInput = output.nextInput
            payload.add(output.payload)
        }
        return Output(payload, nextInput)
    }
}

fun <T> inOrder(parsers: List<Parser<T>>) = InOrder(parsers)

fun <T> inOrder(vararg parsers: Parser<T>) = inOrder(parsers.toList())
