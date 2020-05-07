package parser4k

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

fun <T> repeat(parser: Parser<T>, atLeast: Int = 0) = object : Parser<List<T>> {
    override fun parse(input: Input): Output<List<T>>? {
        val payload = ArrayList<T>()
        var nextInput = input
        while (true) {
            val output = parser.parse(nextInput) ?: break
            nextInput = output.input
            payload.add(output.payload)
        }
        return if (payload.size >= atLeast) Output(payload, nextInput) else null
    }
}

fun <T> or(vararg parsers: Parser<T>): Parser<T> = or(parsers.toList())

fun <T> or(parsers: List<Parser<T>>) = object : Parser<T> {
    override fun parse(input: Input): Output<T>? {
        parsers.forEach { parser ->
            val output = parser.parse(input)
            if (output != null) return output
        }
        return null
    }
}

fun <T> orWithPrecedence(vararg parsers: Parser<T>): Parser<T> = orWithPrecedence(parsers.toList())

fun <T> orWithPrecedence(parsers: List<Parser<T>>) = object : Parser<T> {
    var index = 0

    override fun parse(input: Input): Output<T>? {
        parsers.subList(index, parsers.size).forEachIndexed { parserIndex, parser ->
            val lastIndex = index
            index = if (parser is ResetPrecedence) 0 else parserIndex

            val output = parser.parse(input)

            index = lastIndex
            if (output != null) return output
        }
        return null
    }
}

fun <T> Parser<T>.nestedPrecedence() = ResetPrecedence(this)

class ResetPrecedence<T>(private val parser: Parser<T>) : Parser<T> {
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