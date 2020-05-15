@file:Suppress("DuplicatedCode")

package parser4k

fun <A, B, R> Parser<List3<A, *, B>>.mapAsBinary(f: (A, B) -> R) = object : Parser<R> {
    override fun parse(input: Input): Output<R>? {
        val (payload, nextInput) = this@mapAsBinary.parse(input) ?: return null
        val (left, _, right) = payload
        return Output(f(left, right), nextInput)
    }
}

fun <T1> inOrder(parser1: Parser<T1>) =
    object : Parser<List1<T1>> {
        override fun parse(input: Input): Output<List1<T1>>? {
            val (payload1, input1) = parser1.parse(input) ?: return null
            return Output(List1(payload1), input1)
        }
    }

fun <T1, T2> inOrder(parser1: Parser<T1>, parser2: Parser<T2>) =
    object : Parser<List2<T1, T2>> {
        override fun parse(input: Input): Output<List2<T1, T2>>? {
            val (payload1, input1) = parser1.parse(input) ?: return null
            val (payload2, input2) = parser2.parse(input1) ?: return null
            return Output(List2(payload1, payload2), input2)
        }
    }

fun <T1, T2, T3> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>) =
    object : Parser<List3<T1, T2, T3>> {
        override fun parse(input: Input): Output<List3<T1, T2, T3>>? {
            val (payload1, input1) = parser1.parse(input) ?: return null
            val (payload2, input2) = parser2.parse(input1) ?: return null
            val (payload3, input3) = parser3.parse(input2) ?: return null
            return Output(List3(payload1, payload2, payload3), input3)
        }
    }

fun <T1, T2, T3, T4> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>) =
    object : Parser<List4<T1, T2, T3, T4>> {
        override fun parse(input: Input): Output<List4<T1, T2, T3, T4>>? {
            val (payload1, input1) = parser1.parse(input) ?: return null
            val (payload2, input2) = parser2.parse(input1) ?: return null
            val (payload3, input3) = parser3.parse(input2) ?: return null
            val (payload4, input4) = parser4.parse(input3) ?: return null
            return Output(List4(payload1, payload2, payload3, payload4), input4)
        }
    }

fun <T1, T2, T3, T4, T5> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>, parser5: Parser<T5>) =
    object : Parser<List5<T1, T2, T3, T4, T5>> {
        override fun parse(input: Input): Output<List5<T1, T2, T3, T4, T5>>? {
            val (payload1, input1) = parser1.parse(input) ?: return null
            val (payload2, input2) = parser2.parse(input1) ?: return null
            val (payload3, input3) = parser3.parse(input2) ?: return null
            val (payload4, input4) = parser4.parse(input3) ?: return null
            val (payload5, input5) = parser5.parse(input4) ?: return null
            return Output(List5(payload1, payload2, payload3, payload4, payload5), input5)
        }
    }

fun <T1, T2, T3, T4, T5, T6> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>, parser5: Parser<T5>, parser6: Parser<T6>) =
    object : Parser<List6<T1, T2, T3, T4, T5, T6>> {
        override fun parse(input: Input): Output<List6<T1, T2, T3, T4, T5, T6>>? {
            val (payload1, input1) = parser1.parse(input) ?: return null
            val (payload2, input2) = parser2.parse(input1) ?: return null
            val (payload3, input3) = parser3.parse(input2) ?: return null
            val (payload4, input4) = parser4.parse(input3) ?: return null
            val (payload5, input5) = parser5.parse(input4) ?: return null
            val (payload6, input6) = parser6.parse(input5) ?: return null
            return Output(List6(payload1, payload2, payload3, payload4, payload5, payload6), input6)
        }
    }

fun <T1, T2, T3, T4, T5, T6, T7> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>, parser5: Parser<T5>, parser6: Parser<T6>, parser7: Parser<T7>) =
    object : Parser<List7<T1, T2, T3, T4, T5, T6, T7>> {
        override fun parse(input: Input): Output<List7<T1, T2, T3, T4, T5, T6, T7>>? {
            val (payload1, input1) = parser1.parse(input) ?: return null
            val (payload2, input2) = parser2.parse(input1) ?: return null
            val (payload3, input3) = parser3.parse(input2) ?: return null
            val (payload4, input4) = parser4.parse(input3) ?: return null
            val (payload5, input5) = parser5.parse(input4) ?: return null
            val (payload6, input6) = parser6.parse(input5) ?: return null
            val (payload7, input7) = parser7.parse(input6) ?: return null
            return Output(List7(payload1, payload2, payload3, payload4, payload5, payload6, payload7), input7)

        }
    }

fun <T1, T2, T3, T4, T5, T6, T7, T8> inOrder(parser1: Parser<T1>, parser2: Parser<T2>, parser3: Parser<T3>, parser4: Parser<T4>, parser5: Parser<T5>, parser6: Parser<T6>, parser7: Parser<T7>, parser8: Parser<T8>) =
    object : Parser<List8<T1, T2, T3, T4, T5, T6, T7, T8>> {
        override fun parse(input: Input): Output<List8<T1, T2, T3, T4, T5, T6, T7, T8>>? {
            val (payload1, input1) = parser1.parse(input) ?: return null
            val (payload2, input2) = parser2.parse(input1) ?: return null
            val (payload3, input3) = parser3.parse(input2) ?: return null
            val (payload4, input4) = parser4.parse(input3) ?: return null
            val (payload5, input5) = parser5.parse(input4) ?: return null
            val (payload6, input6) = parser6.parse(input5) ?: return null
            val (payload7, input7) = parser7.parse(input6) ?: return null
            val (payload8, input8) = parser8.parse(input7) ?: return null
            return Output(List8(payload1, payload2, payload3, payload4, payload5, payload6, payload7, payload8), input8)
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

data class List4<T1, T2, T3, T4>(val value1: T1, val value2: T2, val value3: T3, val value4: T4) {
    operator fun <T5> plus(value5: T5): List5<T1, T2, T3, T4, T5> = List5(value1, value2, value3, value4, value5)
}

data class List5<T1, T2, T3, T4, T5>(val value1: T1, val value2: T2, val value3: T3, val value4: T4, val value5: T5) {
    operator fun <T6> plus(value6: T6): List6<T1, T2, T3, T4, T5, T6> = List6(value1, value2, value3, value4, value5, value6)
}

data class List6<T1, T2, T3, T4, T5, T6>(val value1: T1, val value2: T2, val value3: T3, val value4: T4, val value5: T5, val value6: T6) {
    operator fun <T7> plus(value7: T7): List7<T1, T2, T3, T4, T5, T6, T7> = List7(value1, value2, value3, value4, value5, value6, value7)
}

data class List7<T1, T2, T3, T4, T5, T6, T7>(val value1: T1, val value2: T2, val value3: T3, val value4: T4, val value5: T5, val value6: T6, val value7: T7) {
    operator fun <T8> plus(value8: T8): List8<T1, T2, T3, T4, T5, T6, T7, T8> = List8(value1, value2, value3, value4, value5, value6, value7, value8)
}

data class List8<T1, T2, T3, T4, T5, T6, T7, T8>(val value1: T1, val value2: T2, val value3: T3, val value4: T4, val value5: T5, val value6: T6, val value7: T7, val value8: T8)

