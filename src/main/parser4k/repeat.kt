package parser4k

fun <T> repeat(parser: Parser<T>, atLeast: Int = 0, atMost: Int = Int.MAX_VALUE) = object : Parser<List<T>> {
    override fun invoke(input: Input): Output<List<T>>? {
        val payload = ArrayList<T>()
        var nextInput = input
        while (true) {
            val output = parser.invoke(nextInput) ?: break
            nextInput = output.nextInput
            payload.add(output.payload)
            if (payload.size == atMost) break
        }
        return if (payload.size >= atLeast) Output(payload, nextInput) else null
    }
}

fun <T> zeroOrMore(parser: Parser<T>): Parser<List<T>> = repeat(parser, atLeast = 0)

fun <T> oneOrMore(parser: Parser<T>): Parser<List<T>> = repeat(parser, atLeast = 1)

fun <T> optional(parser: Parser<T>): Parser<T?> = repeat(parser, atLeast = 0, atMost = 1).map { it.firstOrNull() }
