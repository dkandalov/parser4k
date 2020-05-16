package parser4k

fun str(s: String) = object : Parser<String> {
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