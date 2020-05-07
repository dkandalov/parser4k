package parser4k

fun <T1> inOrder(parser1: Parser<T1>) =
    object : Parser<List1<T1>> {
        override fun parse(input: Input): Output<List1<T1>>? {
            val (payload, nextInput) = parser1.parse(input) ?: return null
            return Output(List1(payload), nextInput)
        }
    }

fun <T1, T2> inOrder(parser1: Parser<T1>, parser2: Parser<T2>) =
    object : Parser<List2<T1, T2>> {
        override fun parse(input: Input): Output<List2<T1, T2>>? {
            val (payload, nextInput) = inOrder(parser1).parse(input) ?: return null
            val (lastPayload, lastInput) = parser2.parse(nextInput) ?: return null
            return Output(payload + lastPayload, lastInput)
        }
    }

fun <T1, T2, T3> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>) =
    object : Parser<List3<T1, T2, T3>> {
        override fun parse(input: Input): Output<List3<T1, T2, T3>>? {
            val (payload, nextInput) = inOrder(parser1, parser2).parse(input) ?: return null
            val (lastPayload, lastInput) = parser3.parse(nextInput) ?: return null
            return Output(payload + lastPayload, lastInput)
        }
    }

fun <T1, T2, T3, T4> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>) =
    object : Parser<List4<T1, T2, T3, T4>> {
        override fun parse(input: Input): Output<List4<T1, T2, T3, T4>>? {
            val (payload, nextInput) = inOrder(parser1, parser2, parser3).parse(input) ?: return null
            val (lastPayload, lastInput) = parser4.parse(nextInput) ?: return null
            return Output(payload + lastPayload, lastInput)
        }
    }


fun <A, B, R> Parser<List3<A, *, B>>.mapAsBinary(f: (A, B) -> R) = object : Parser<R> {
    override fun parse(input: Input): Output<R>? {
        val (payload, nextInput) = this@mapAsBinary.parse(input) ?: return null
        val (left, _, right) = payload
        return Output(f(left, right), nextInput)
    }
}


data class List1<T1>(val value1: T1) {
    operator fun <T2> plus(value2: T2): List2<T1, T2> = List2(value1, value2)
}

data class List2<T1, T2>(val value1: T1, val value2: T2) {
    operator fun <T3> plus(value3: T3): List3<T1, T2, T3> = List3(value1, value2, value3)
}

data class List3<T1, T2, T3>(val value1: T1, val value2: T2, val value3: T3) {
    operator fun <T4> plus(value4: T4): List4<T1, T2, T3, T4> = List4(value1, value2, value3, value4)
}

data class List4<T1, T2, T3, T4>(val value1: T1, val value2: T2, val value3: T3, val value4: T4)

