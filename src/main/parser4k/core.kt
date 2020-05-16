package parser4k

data class Input(
    val value: String,
    val offset: Int = 0,
    val injectPayload: InjectPayload? = null
)

fun interface InjectPayload: (Any?) -> Any?

data class Output<out T>(val payload: T, val input: Input)

interface Parser<out T> {
    fun parse(input: Input): Output<T>?
}

fun <T, R> Parser<T>.map(transform: (T) -> R) = object : Parser<R> {
    override fun parse(input: Input): Output<R>? {
        val (payload, nextInput) = this@map.parse(input) ?: return null
        return Output(transform(payload), nextInput)
    }
}