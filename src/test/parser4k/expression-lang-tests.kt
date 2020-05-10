@file:Suppress("PackageDirectoryMismatch")

package parser4k.expressionlang

import parser4k.*
import parser4k.CommonParsers.joinedWith
import parser4k.CommonParsers.token
import parser4k.expressionlang.ExpressionLang.Expr.*
import parser4k.expressionlang.ExpressionLang.evaluate
import parser4k.expressionlang.ExpressionLang.parse
import kotlin.test.Test

private object ExpressionLang {
    sealed class Expr {
        object True : Expr()
        object False : Expr()
        data class IntLiteral(val value: Int) : Expr()
        data class StringLiteral(val value: String) : Expr()
        data class ArrayLiteral(val value: List<Expr>) : Expr()

        data class Equal(val left: Expr, val right: Expr) : Expr()
        data class NotEqual(val left: Expr, val right: Expr) : Expr()
        data class Less(val left: Expr, val right: Expr) : Expr()
        data class Greater(val left: Expr, val right: Expr) : Expr()

        data class And(val left: Expr, val right: Expr) : Expr()
        data class Or(val left: Expr, val right: Expr) : Expr()

        data class InArray(val left: Expr, val right: Expr) : Expr()
        data class NotInArray(val left: Expr, val right: Expr) : Expr()
    }

    private fun Expr.eval(): Any =
        when (this) {
            True             -> true
            False            -> false
            is IntLiteral    -> value
            is StringLiteral -> value
            is ArrayLiteral  -> value.map { it.eval() }
            is Equal         -> left.eval() == right.eval()
            is NotEqual      -> left.eval() != right.eval()
            is Less          -> (left.eval() as Int) < (right.eval() as Int)
            is Greater       -> (left.eval() as Int) > (right.eval() as Int)
            is And           -> (left.eval() as Boolean) && (right.eval() as Boolean)
            is Or            -> (left.eval() as Boolean) || (right.eval() as Boolean)
            is InArray       -> left.eval() in (right.eval() as List<*>)
            is NotInArray    -> left.eval() !in (right.eval() as List<*>)
        }

    fun parse(s: String) = s.parseWith(expr)

    fun evaluate(s: String) = s.parseWith(expr).eval()

    private val cache = OutputCache<Expr>()

    private fun binaryExpr(tokenString: String, f: (Expr, Expr) -> Expr) =
        inOrder(ref { expr }, token(tokenString), ref { expr }).mapAsBinary(f)
            .with(cache)

    private val boolLiteral = oneOf(str("true"), str("false")).map { if (it == "true") True else False }
    private val integerLiteral = CommonParsers.integer.map { IntLiteral(it.toInt()) }
    private val stringLiteral = CommonParsers.string.map { StringLiteral(it) }
    private val arrayLiteral = inOrder(token("["), joinedWith(token(","), ref { expr }), token("]"))
        .map { (_, list, _) -> ArrayLiteral(list) }
        .with(cache)

    private val equal = binaryExpr("==", ::Equal)
    private val notEqual = binaryExpr("!=", ::NotEqual)
    private val less = binaryExpr("<", ::Less)
    private val greater = binaryExpr(">", ::Greater)

    private val inArray = binaryExpr("in", ::InArray)
    private val notInArray = binaryExpr("not in", ::NotInArray)

    private val and = binaryExpr("and", ::And)
    private val or = binaryExpr("or", ::Or)

    private val expr: Parser<Expr> = oneOfWithPrecedence(
        or,
        and,
        oneOf(inArray, notInArray),
        oneOf(equal, notEqual, less, greater),
        oneOf(arrayLiteral, stringLiteral, integerLiteral, boolLiteral)
    ).reset(cache)
}

class ExpressionLangTests {
    @Test fun literals() {
        evaluate("true") shouldEqual true

        evaluate("1") shouldEqual 1
        evaluate("123") shouldEqual 123

        evaluate("\"\"") shouldEqual ""
        evaluate("\"abc\"") shouldEqual "abc"
        evaluate("\"a\\\"c\"") shouldEqual """a\"c"""

        evaluate("[]") shouldEqual emptyList<Any>()
        evaluate("[1]") shouldEqual listOf(1)
        evaluate("[1, 2, 3]") shouldEqual listOf(1, 2, 3)
        evaluate("[[1]]") shouldEqual listOf(listOf(1))
        evaluate("[[1, 2], [3]]") shouldEqual listOf(listOf(1, 2), listOf(3))
    }

    @Test fun comparison() {
        evaluate("true == true") shouldEqual true
        evaluate("true == false") shouldEqual false

        evaluate("1 != 2") shouldEqual true
        evaluate("123 != 123") shouldEqual false

        evaluate("1 < 2") shouldEqual true
        evaluate("2 < 2") shouldEqual false

        evaluate("2 > 1") shouldEqual true
        evaluate("2 > 2") shouldEqual false
    }

    @Test fun `array operations`() {
        evaluate("1 in []") shouldEqual false
        evaluate("1 in [\"\"]") shouldEqual false
        evaluate("1 in [1, 2, 3]") shouldEqual true

        evaluate("1 not in []") shouldEqual true
        evaluate("1 not in [\"\"]") shouldEqual true
        evaluate("1 not in [1, 2, 3]") shouldEqual false
    }

    @Test fun `and-or expressions`() {
        evaluate("1==1 and 2==2") shouldEqual true
        evaluate("1==1 and 2==3") shouldEqual false
        evaluate("1==1 or 2==2") shouldEqual true
        evaluate("1==2 or 2==3") shouldEqual false

        parse("1 and 2 or 3") shouldEqual Or(And(IntLiteral(1), IntLiteral(2)), IntLiteral(3))
        parse("1 or 2 and 3") shouldEqual Or(IntLiteral(1), And(IntLiteral(2), IntLiteral(3)))
    }
}