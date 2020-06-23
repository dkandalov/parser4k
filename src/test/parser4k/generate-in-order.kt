package parser4k

import java.io.File

fun main() {
    File("src/main/parser4k/in-order-generated.kt").printWriter().use { writer ->
        CodeGenerator(println = { writer.println(it) }).generate()
    }
//    CodeGenerator(println = { println(it) }).generate()
}

private class CodeGenerator(private val println: (String) -> Unit) {
    private val maxN = 8

    fun generate() {
        generateHeader()
        generateSkipFirst()
        generateSkipLast()
        generateSkipWrapper()
        generateLeftAssoc()
        generateInOrderParsers()
        generateInOrderFunctions()
        generateLists()
    }

    private fun generateHeader() {
        println("""@file:Suppress("UNCHECKED_CAST", "unused")""")
        println("")
        println("package parser4k")
        println("")
        println("/////////////////////////////////////////////////")
        println("// This file is generated by generate-in-order.kt")
        println("/////////////////////////////////////////////////")
        println("")
    }

    private fun generateSkipFirst() {
        // For example:
        // fun <T2, T3> InOrder3<*, T2, T3>.skipFirst(): Parser<List2<T2, T3>> = map { (_, it2, it3) -> List2(it2, it3) }

        println("fun <T2> InOrder2<*, T2>.skipFirst(): Parser<T2> = map { (_, it2) -> it2 }") // Special case because returning List1 is pointless
        (3..maxN).forEach { n ->
            val ts = (2..n).joinToString { "T$it" }
            val its = (2..n).joinToString { "it$it" }
            println("fun <$ts> InOrder$n<*, $ts>.skipFirst(): Parser<List${n - 1}<$ts>> = map { (_, $its) -> List${n - 1}($its) }")
        }
        println("")
    }

    private fun generateSkipLast() {
        // For example:
        // fun <T1, T2> InOrder3<T1, T2, *>.skipLast(): Parser<List2<T1, T2>> = map { (it1, it2, _) -> List2(it1, it2) }

        println("fun <T1> InOrder2<T1, *>.skipLast(): Parser<T1> = map { (it1, _) -> it1 }") // Special case because returning List1 is pointless
        (2 until maxN).forEach { n ->
            val ts = (1..n).joinToString { "T$it" }
            val its = (1..n).joinToString { "it$it" }
            println("fun <$ts> InOrder${n + 1}<$ts, *>.skipLast(): Parser<List$n<$ts>> = map { ($its, _) -> List$n($its) }")
        }
        println("")
    }

    private fun generateSkipWrapper() {
        // For example:
        // fun <T2, T3> InOrder4<*, T2, T3, *>.skipWrapper(): Parser<List2<T2, T3>> = map { (_, it2, it3, _) -> List2(it2, it3) }

        println("fun <T2> InOrder3<*, T2, *>.skipWrapper(): Parser<T2> = map { (_, it2, _) -> it2 }") // Special case because returning List1 is pointless
        (3 until maxN).forEach { n ->
            val ts = (2..n).joinToString { "T$it" }
            val its = (2..n).joinToString { "it$it" }
            println("fun <$ts> InOrder${n + 1}<*, $ts, *>.skipWrapper(): Parser<List${n - 1}<$ts>> = map { (_, $its, _) -> List${n - 1}($its) }")
        }
        println("")
    }

    private fun generateLeftAssoc() {
        // For example:
        // fun <T1, T2, T3> InOrder3<T1, T2, T3>.leftAssoc(transform: (List3<T1, T2, T3>) -> T1): Parser<T1> =
        //    InOrder(listOf(parser1, parser2, parser3))
        //        .leftAssoc { (it1, it2, it3) -> transform(List3(it1 as T1, it2 as T2, it3 as T3)) } as Parser<T1>

        (3..maxN).forEach { n ->
            val ts = (1..n).joinToString { "T$it" }
            val its = (1..n).joinToString { "it$it" }
            val itsAsTs = (1..n).joinToString { "it$it as T$it" }
            val parsers = (1..n).joinToString { "parser$it" }
            println("""
                fun <$ts> InOrder$n<$ts>.leftAssoc(transform: (List$n<$ts>) -> T1): Parser<T1> =
                    InOrder(listOf($parsers))
                        .leftAssoc { ($its) -> transform(List$n($itsAsTs)) } as Parser<T1>
            """.trimIndent())
        }
        println("")
    }

    private fun generateInOrderParsers() {
        // For example:
        // class InOrder2<T1, T2>(val parser1: Parser<T1>, val parser2: Parser<T2>) : Parser<List2<T1, T2>> {
        //     override fun parse(input: Input): Output<List2<T1, T2>>? =
        //         InOrder(listOf(parser1, parser2)).map { List2(it[0] as T1, it[1] as T2) }.parse(input)
        // }

        (2..maxN).forEach { n ->
            val ts = (1..n).joinToString { "T$it" }
            val parserVals = (1..n).joinToString { "val parser$it: Parser<T$it>" }
            val parsers = (1..n).joinToString { "parser$it" }
            val itAsTs = (1..n).joinToString { "it[${it - 1}] as T$it" }
            println("""
                class InOrder$n<$ts>($parserVals) : Parser<List$n<$ts>> {
                    override fun parse(input: Input): Output<List$n<$ts>>? = 
                        InOrder(listOf($parsers)).map { List$n($itAsTs) }.parse(input)
                }
            """.trimIndent())
        }
        println("")
    }

    private fun generateInOrderFunctions() {
        // For example:
        // fun <T1, T2> inOrder(parser1: Parser<T1>, parser2: Parser<T2>): InOrder2<T1, T2> =
        //     InOrder2(parser1, parser2)

        (2..maxN).forEach { n ->
            val ts = (1..n).joinToString { "T$it" }
            val parserVals = (1..n).joinToString { "parser$it: Parser<T$it>" }
            val parsers = (1..n).joinToString { "parser$it" }
            println("""
                fun <$ts> inOrder($parserVals): InOrder$n<$ts> =
                    InOrder$n($parsers)
            """.trimIndent())
        }
        println("")
    }

    private fun generateLists() {
        // For example:
        // data class List2<T1, T2>(val value1: T1, val value2: T2) {
        //     operator fun <T3> plus(value3: T3): List3<T1, T2, T3> =
        //      List3(value1, value2, value3)
        // }

        (1..maxN).forEach { n ->
            val ts = (1..n).joinToString { "T$it" }
            val valuesAsTs = (1..n).joinToString { "val value$it: T$it" }
            println("""
                data class List$n<$ts>($valuesAsTs)
            """.trimIndent())
        }
        println("")
    }
}

