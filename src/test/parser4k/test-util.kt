package parser4k

import java.math.BigDecimal
import kotlin.test.assertEquals

infix fun Any?.shouldEqual(expected: Any?) =
    assertEquals(expected = expected, actual = this)

infix fun (() -> Any?).shouldFailWith(f: (ParsingError) -> Boolean) =
    try {
        this()
    } catch (e: ParsingError) {
        assert(f(e))
    }

infix fun (() -> Any?).shouldFailWithMessage(expectedMessage: String) =
    try {
        this()
    } catch (e: ParsingError) {
        e.message shouldEqual expectedMessage.trimMargin()
    }


sealed class Expression {
    data class Number(val value: BigDecimal) : Expression()
    data class Plus(val left: Expression, val right: Expression) : Expression()
    data class Minus(val left: Expression, val right: Expression) : Expression()
    data class Multiply(val left: Expression, val right: Expression) : Expression()
    data class Divide(val left: Expression, val right: Expression) : Expression()
    data class Power(val left: Expression, val right: Expression) : Expression()
}

fun Expression.toExpressionString(): String =
    when (this) {
        is Expression.Number   -> value.toString()
        is Expression.Plus     -> "(${left.toExpressionString()} + ${right.toExpressionString()})"
        is Expression.Minus    -> "(${left.toExpressionString()} - ${right.toExpressionString()})"
        is Expression.Multiply -> "(${left.toExpressionString()} * ${right.toExpressionString()})"
        is Expression.Divide   -> "(${left.toExpressionString()} / ${right.toExpressionString()})"
        is Expression.Power    -> "(${left.toExpressionString()} ^ ${right.toExpressionString()})"
    }
