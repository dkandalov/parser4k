package parser4k

fun <T> ref(f: () -> Parser<T>) = object : Parser<T> {
    override fun parse(input: Input) = f().parse(input)
}

fun <T> nonRecRef(f: () -> Parser<T>) = object : Parser<T> {
    val offsets: HashSet<Int> = HashSet()

    override fun parse(input: Input): Output<T>? {
        if (!offsets.add(input.offset)) return null // Prevent stack overflow on (left) recursion
        val output = f().parse(input)
        offsets.remove(input.offset)
        return output
    }
}
