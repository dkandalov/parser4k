@file:Suppress("UNCHECKED_CAST")

package parser4k

fun <T> InOrder<T>.mapLeftAssoc(transform: (List<T>) -> T) = object : Parser<T> {
    private val leftParser = nonRecRef { (parsers.first()) }
    private val midParsers = parsers.drop(1).dropLast(1)
    private val rightParser = parsers.last()

    override fun parse(input: Input): Output<T>? {
        if (input.leftPayload == RightRecursionMarker) return null

        val leftOutput =
            if (input.leftPayload == null) leftParser.parse(input) ?: return null
            else Output(input.leftPayload as T, input.copy(leftPayload = null))
        val midOutput = InOrder(midParsers).parse(leftOutput.nextInput) ?: return null
        val rightOutput = rightParser.parse(midOutput.nextInput.copy(leftPayload = RightRecursionMarker)) ?: return null

        val payload = transform(listOf(leftOutput.payload) + midOutput.payload + rightOutput.payload)
        val nextInput = rightOutput.nextInput.copy(leftPayload = payload)
        return leftParser.parse(nextInput) ?: return Output(payload, nextInput)
    }
}

private object RightRecursionMarker


fun <T> InOrder<T>.leftAssoc(transform: (List<T>) -> T) = object : Parser<T> {
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

internal fun <T> Parser<T>.parseWithInject(input: Input, injectPayload: InjectPayload? = input.injectPayload): Output<T>? {
    val (payload, nextInput) = parse(input.copy(injectPayload = injectPayload)) ?: return null
    return Output(
        if (nextInput.injectPayload == null) payload else nextInput.injectPayload.invoke(payload) as T,
        nextInput.noInjectPayload()
    )
}

internal fun <T> Parser<T>.parseHidingInject(input: Input): Output<T>? {
    val (payload, nextInput) = parse(input.noInjectPayload()) ?: return null
    return Output(payload, nextInput.copy(injectPayload = input.injectPayload))
}

private fun Input.noInjectPayload() = copy(injectPayload = null)
