@file:Suppress("PackageDirectoryMismatch")

package parser4k.expressionlang

import parser4k.*
import parser4k.commonparsers.Tokens
import parser4k.commonparsers.joinedWith
import parser4k.commonparsers.token
import parser4k.expressionlang.ExpressionLang.Expr.*
import parser4k.expressionlang.ExpressionLang.evaluate
import parser4k.expressionlang.ExpressionLang.parse
import kotlin.test.Test

private object ExpressionLang {
    private val cache = OutputCache<Expr>()

    private fun binaryExpr(tokenString: String, f: (Expr, Expr) -> Expr) =
        inOrder(ref { expr }, token(tokenString), ref { expr }).leftAssocAsBinary(f).with(cache)

    private fun unaryExpr(tokenString: String, f: (Expr) -> Expr) =
        inOrder(token(tokenString), ref { expr }).map { (_, it) -> f(it) }.with(cache)

    private val boolLiteral = oneOf(str("true"), str("false")).map { if (it == "true") True else False }
    private val intLiteral = Tokens.integer.map { IntLiteral(it.toInt()) }
    private val stringLiteral = Tokens.string.map { StringLiteral(it) }
    private val arrayLiteral = inOrder(token("["), ref { expr }.joinedWith(token(",")), token("]"))
        .skipWrapper().map(::ArrayLiteral)
        .with(cache)

    private val equal = binaryExpr("==", ::Equal)
    private val notEqual = binaryExpr("!=", ::NotEqual)
    private val less = binaryExpr("<", ::Less)
    private val greater = binaryExpr(">", ::Greater)

    private val divide = binaryExpr("/", ::Divide)
    private val multiply = binaryExpr("*", ::Multiply)
    private val minus = binaryExpr("-", ::Minus)
    private val plus = binaryExpr("+", ::Plus)
    private val unaryMinus = unaryExpr("-", ::UnaryMinus)

    private val inArray = binaryExpr("in", ::InArray)
    private val notInArray = binaryExpr("not in", ::NotInArray)
    private val arrayAccess = inOrder(ref { expr }, token("["), inOrder(ref { expr }, token("]")).skipLast())
        .leftAssocAsBinary(::ArrayAccess).with(cache)

    private val and = binaryExpr("and", ::And)
    private val or = binaryExpr("or", ::Or)
    private val not = unaryExpr("not", ::Not)
    private val ifThenElse = inOrder(token("if"), ref { expr }, token("then"), ref { expr }, token("else"), ref { expr })
        .map { (_, cond, _, ifTrue, _, ifFalse) -> IfThenElse(cond, ifTrue, ifFalse) }
        .with(cache)

    private val paren = inOrder(token("("), ref { expr }, token(")")).skipWrapper().with(cache)

    private val identifier = Tokens.identifier.map(::Identifier)

    private val fieldAccess = inOrder(ref { expr }, token("."), identifier)
        .leftAssoc { (left, _, right) -> FieldAccess(left, right) }.with(cache)

    private val expr: Parser<Expr> = oneOfWithPrecedence(
        ifThenElse,
        or,
        and,
        oneOf(inArray, notInArray),
        oneOf(equal, notEqual, less, greater),
        oneOf(plus, minus),
        oneOf(multiply, divide),
        oneOf(unaryMinus, not),
        fieldAccess,
        arrayAccess.nestedPrecedence(),
        paren.nestedPrecedence(),
        arrayLiteral.nestedPrecedence(),
        oneOf(stringLiteral, intLiteral, boolLiteral)
    ).reset(cache)

    fun parse(s: String): Expr = s.parseWith(expr)

    fun evaluate(s: String): Any = s.parseWith(expr).eval()

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

        data class Plus(val left: Expr, val right: Expr) : Expr()
        data class Minus(val left: Expr, val right: Expr) : Expr()
        data class Multiply(val left: Expr, val right: Expr) : Expr()
        data class Divide(val left: Expr, val right: Expr) : Expr()
        data class UnaryMinus(val value: Expr) : Expr()

        data class And(val left: Expr, val right: Expr) : Expr()
        data class Or(val left: Expr, val right: Expr) : Expr()
        data class Not(val value: Expr) : Expr()
        data class IfThenElse(val cond: Expr, val ifTrue: Expr, val ifFalse: Expr) : Expr()

        data class InArray(val left: Expr, val right: Expr) : Expr()
        data class NotInArray(val left: Expr, val right: Expr) : Expr()
        data class ArrayAccess(val left: Expr, val right: Expr) : Expr()

        data class Identifier(val value: String) : Expr()
        data class FieldAccess(val obj: Expr, val fieldName: Identifier) : Expr()
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

            is Plus          -> (left.eval() as Int) + (right.eval() as Int)
            is Minus         -> (left.eval() as Int) - (right.eval() as Int)
            is Multiply      -> (left.eval() as Int) * (right.eval() as Int)
            is Divide        -> (left.eval() as Int) / (right.eval() as Int)
            is UnaryMinus    -> -(value.eval() as Int)

            is InArray       -> left.eval() in (right.eval() as List<*>)
            is NotInArray    -> left.eval() !in (right.eval() as List<*>)
            is ArrayAccess   -> (left.eval() as List<*>)[right.eval() as Int]!!

            is And           -> (left.eval() as Boolean) && (right.eval() as Boolean)
            is Or            -> (left.eval() as Boolean) || (right.eval() as Boolean)
            is Not           -> !(value.eval() as Boolean)
            is IfThenElse    -> if (cond.eval() as Boolean) ifTrue.eval() else ifFalse.eval()

            is Identifier    -> error("Should not be evaluated on its own")
            is FieldAccess   -> {
                val obj = obj.eval()
                val name = fieldName.value
                // Not using reflection here because it's as slow as all other tests.
                when {
                    obj is List<*> && name == "size" -> obj.size
                    obj is String && name == "size"  -> obj.length
                    else                             -> error("Unsupported field '$name' on $obj")
                }
            }
        }
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
        evaluate("[1 + 2, 3 * 4]") shouldEqual listOf(3, 12)
        evaluate("[1, 2, 3]") shouldEqual listOf(1, 2, 3)
        evaluate("[[1]]") shouldEqual listOf(listOf(1))
        evaluate("[[1, 2], [3]]") shouldEqual listOf(listOf(1, 2), listOf(3))
    }

    @Test fun comparison() {
        evaluate("true == true") shouldEqual true
        evaluate("true == false") shouldEqual false
        evaluate("\"foo\" == \"bar\"") shouldEqual false
        evaluate("[1, 2] == [1, 2]") shouldEqual true

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

        evaluate("[1, 2, 3][0]") shouldEqual 1
        evaluate("[1, 2, 3][1]") shouldEqual 2
        evaluate("[1, 2, 3][2]") shouldEqual 3
        evaluate("[1, 2, 3][3 - 1]") shouldEqual 3

        evaluate("[[123]][0]") shouldEqual listOf(123)
        evaluate("[[123]][0][0]") shouldEqual 123
    }

    @Test fun `and, or, not expressions`() {
        evaluate("1==1 and 2==2") shouldEqual true
        evaluate("1==1 and 2==3") shouldEqual false
        evaluate("1==1 or 2==2") shouldEqual true
        evaluate("1==2 or 2==3") shouldEqual false
        evaluate("not false") shouldEqual true

        parse("1 and 2 or 3") shouldEqual Or(And(IntLiteral(1), IntLiteral(2)), IntLiteral(3))
        parse("1 or 2 and 3") shouldEqual Or(IntLiteral(1), And(IntLiteral(2), IntLiteral(3)))
        parse("1 or 2 or 3") shouldEqual Or(Or(IntLiteral(1), IntLiteral(2)), IntLiteral(3))
    }

    @Test fun `arithmetic expressions`() {
        evaluate("1 + 2") shouldEqual 3
        evaluate("1 - 2 + 3") shouldEqual 2
        evaluate("1 + 2 * 3") shouldEqual 7
        evaluate("1 + 2 * 3 / 4") shouldEqual 2

        evaluate("-1 + 2") shouldEqual 1
        evaluate("-1 + -2") shouldEqual -3
        evaluate("-1 - -2") shouldEqual 1
    }

    @Test fun `paren expressions`() {
        evaluate("(123)") shouldEqual 123
        evaluate("((123))") shouldEqual 123
        evaluate("((1 + 2) * 3 + 4)") shouldEqual 13

        parse("not (1 or 2)") shouldEqual Not(Or(IntLiteral(1), IntLiteral(2)))
    }

    @Test fun `if-then-else expressions`() {
        evaluate("if true then 1 else 2") shouldEqual 1
        evaluate("if false then 1 else 2") shouldEqual 2
        evaluate("if false then 1 else if false then 2 else 3") shouldEqual 3

        parse("if false then 1 else if false then 2 else 3") shouldEqual
            IfThenElse(False, IntLiteral(1), IfThenElse(False, IntLiteral(2), IntLiteral(3)))
    }

    @Test fun `field access expressions`() {
        evaluate("[0,1,2].size") shouldEqual 3
        evaluate("\"abcde\".size") shouldEqual 5

        parse("1.foo") shouldEqual FieldAccess(IntLiteral(1), Identifier("foo"))
        parse("1.foo.bar") shouldEqual FieldAccess(FieldAccess(IntLiteral(1), Identifier("foo")), Identifier("bar"))
        parse("1.foo.bar.woof") shouldEqual FieldAccess(FieldAccess(FieldAccess(IntLiteral(1), Identifier("foo")), Identifier("bar")), Identifier("woof"))
    }
}