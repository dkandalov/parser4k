package parser4k

class OutputCache<T> {
    private val outputCache = HashMap<Pair<Parser<T>, Int>, Output<T>?>()

    fun contains(key: Pair<Parser<T>, Int>) = outputCache.containsKey(key)

    operator fun get(key: Pair<Parser<T>, Int>) = outputCache[key]

    operator fun set(key: Pair<Parser<T>, Int>, output: Output<T>?) {
        outputCache[key] = output
    }

    fun clear() = outputCache.clear()
}

fun <T> Parser<T>.with(outputCache: OutputCache<T>): Parser<T> = object : Parser<T> {
    override fun parse(input: Input): Output<T>? {
        val parser = this@with
        val pair = Pair(parser, input.offset)
        if (outputCache.contains(pair)) return outputCache[pair]
        outputCache[pair] = null // Mark parser at offset as work-in-progress

        val output = parser.parse(input)

        outputCache[pair] = output
        return output
    }
}

fun <T> Parser<T>.reset(outputCache: OutputCache<T>) = object : Parser<T> {
    private var depth = 0

    override fun parse(input: Input): Output<T>? {
        depth++
        val output = this@reset.parse(input)
        depth--
        if (depth == 0) outputCache.clear()
        return output
    }
}