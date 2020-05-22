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

fun <T1, T2> inOrder(parser1: Parser<T1>, parser2: Parser<T2>) =
    InOrder2(parser1, parser2)

fun <T1, T2, T3> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>) =
    InOrder3(parser1, parser2, parser3)

fun <T1, T2, T3, T4> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>) =
    InOrder4(parser1, parser2, parser3, parser4)

fun <T1, T2, T3, T4, T5> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>, parser5: Parser<T5>) =
    InOrder5(parser1, parser2, parser3, parser4, parser5)

fun <T1, T2, T3, T4, T5, T6> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>, parser5: Parser<T5>, parser6: Parser<T6>) =
    InOrder6(parser1, parser2, parser3, parser4, parser5, parser6)

fun <T1, T2, T3, T4, T5, T6, T7> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>, parser5: Parser<T5>, parser6: Parser<T6>, parser7: Parser<T7>) =
    InOrder7(parser1, parser2, parser3, parser4, parser5, parser6, parser7)

fun <T1, T2, T3, T4, T5, T6, T7, T8> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>, parser5: Parser<T5>, parser6: Parser<T6>, parser7: Parser<T7>, parser8: Parser<T8>) =
    InOrder8(parser1, parser2, parser3, parser4, parser5, parser6, parser7, parser8)
