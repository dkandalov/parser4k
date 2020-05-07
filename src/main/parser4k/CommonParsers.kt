package parser4k

object CommonParsers {
    val whitespaces: Parser<String> = regex("[ \\t\\r\\n]*")

    fun token(s: String): Parser<String> =
        inOrder(whitespaces, str(s), whitespaces).map { (_, op, _) -> op }
}