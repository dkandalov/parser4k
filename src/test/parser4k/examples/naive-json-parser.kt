package parser4k.examples

import parser4k.*
import parser4k.commonparsers.Tokens.digit
import parser4k.commonparsers.Tokens.integer
import parser4k.commonparsers.Tokens.letter
import parser4k.commonparsers.Tokens.string
import parser4k.commonparsers.joinedWith
import parser4k.commonparsers.token
import parser4k.examples.NaiveJsonParser.parse
import kotlin.test.Test

private object NaiveJsonParser {
    private val obj = inOrder(token("{"), ref { property }.joinedWith(token(",")), token("}")).skipWrapper().map { it.toMap() }
    private val array = inOrder(token("["), ref { term }.joinedWith(token(",")), token("]")).skipWrapper()
    private val term: Parser<Any> = oneOf(integer, string, obj, array)
    private val identifier = inOrder(str("\""), repeat(oneOf(digit, letter, noneOf('"'))), str("\"")).skipWrapper().map { it.joinToString("") }
    private val property = inOrder(identifier, token(":"), term).map { (id, _, term) -> Pair(id, term) }
    private val json = oneOf(obj, array)

    fun parse(s: String) = s.parseWith(json)
}

class NaiveJsonParserTests {
    private val emptyObject = emptyMap<String, Any>()
    private val emptyList = emptyList<Any>()

    @Test fun `it works`() {
        parse("""{}""") shouldEqual emptyObject
        parse("""{ "foo": 123 }""") shouldEqual mapOf("foo" to "123")
        parse("""{ "foo": 123, "bar": "woof" }""") shouldEqual mapOf("foo" to "123", "bar" to "woof")
        parse("""{ "foo": { "bar": 123 }}""") shouldEqual mapOf("foo" to mapOf("bar" to "123"))
        parse("""{ "foo": [1,2,3] }""") shouldEqual mapOf("foo" to listOf("1", "2", "3"))

        parse("""[]""") shouldEqual emptyList
        parse("""[1, 2, 3]""") shouldEqual listOf("1", "2", "3")
        parse("""[[1, 2, 3], [4, 5, 6]]""") shouldEqual listOf(listOf("1", "2", "3"), listOf("4", "5", "6"))
        parse("""[{ "foo": 123 }]""") shouldEqual listOf(mapOf("foo" to "123"))
    }
}