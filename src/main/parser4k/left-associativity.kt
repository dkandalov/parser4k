@file:Suppress("UNCHECKED_CAST")

package parser4k

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
