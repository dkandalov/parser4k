@file:Suppress("DuplicatedCode")

package parser4k

fun <T1, T3, R> Parser<List3<T1, *, T3>>.mapAsBinary(transform: (T1, T3) -> R): Parser<R> =
    map { (left, _, right) -> transform(left, right) }

fun <T> InOrder3<T, *, T>.leftAssocAsBinary(transform: (T, T) -> T): Parser<T> =
    leftAssoc { (left, _, right) -> transform(left, right) }

fun <T> InOrder<T>.leftAssocAsBinary(transform: (T, T) -> T): Parser<T> =
    leftAssoc { transform(it.first(), it.last()) }

fun <T> Parser<*>.skip(): Parser<T> = object : Parser<T> {
    override fun parse(input: Input): Output<T>? {
        val (_, nextInput) = this@skip.parse(input) ?: return null
        return Output(null as T, nextInput)
    }
}

fun <T> InOrder<T>.leftAssoc(transform: (List<T>) -> T) =
    object : Parser<T> {
        override fun parse(input: Input): Output<T>? {
            val firstParser = parsers.first()
            val innerParsers = parsers.drop(1).dropLast(1)
            val lastParser = parsers.last()

            var output: Output<T>? = null
            var (payload, nextInput) = firstParser.parseWithInject(input) ?: return null
            while (nextInput.offset < nextInput.value.length) {
                val payloads = ArrayList<T>()
                innerParsers.forEach { parser ->
                    val output_ = parser.parse(nextInput) ?: return output
                    payloads.add(output_.payload)
                    nextInput = output_.nextInput
                }
                val output_ = lastParser.parseWithInject(nextInput, object : InjectPayload {
                    override fun invoke(it: Any?) = transform(listOf(payload) + payloads + it as T)
                }) ?: return output
                payload = output_.payload
                nextInput = output_.nextInput
                output = Output(payload, nextInput)
            }
            return output
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
