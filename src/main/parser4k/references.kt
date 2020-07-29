package parser4k

import kotlin.reflect.KProperty0

fun <T> ref(f: () -> Parser<T>): Parser<T> = object : Parser<T> {
    private val parser by lazy { f() }
    override fun parse(input: Input) = parser.parse(input)
}

fun <T> nonRecRef(f: () -> Parser<T>): Parser<T> = nonRec(ref(f))

fun <T> nonRec(parser: Parser<T>): Parser<T> = object : Parser<T> {
    val offsets: HashSet<Int> = HashSet()

    override fun parse(input: Input): Output<T>? {
        if (!offsets.add(input.offset)) return null
        val output = parser.parse(input)
        offsets.remove(input.offset)
        return output
    }
}

fun <T> KProperty0<Parser<T>>.ref(): Parser<T> = ref { get() }

fun <T> KProperty0<Parser<T>>.nonRecRef(): Parser<T> = nonRecRef { get() }
