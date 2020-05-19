# Parser4k

[![Build Status](https://travis-ci.org/dkandalov/parser4k.svg?branch=master)](https://travis-ci.org/dkandalov/parser4k)

THIS IS CURRENTLY WORK-IN-PROGRESS 🍼👶

Parser4k is a recursive descent parser combinator library for Kotlin.

It aims to be:
 - **simple** - very few core concepts, no magic execution workflow or DSL
 - **easy to use** - you can quickly figure out how to write a parser for a small language
 - **production-ready** - not a toy project, good-enough performance to be embedded into real applications
 
### Example
```kotlin
object MinimalCalculator {
    val cache = OutputCache<BigDecimal>()
    fun binaryExpr(s: String) = inOrder(ref { expr }, token(s), ref { expr })

    val number = regex("\\d+").map { it.toBigDecimal() }.with(cache)
    val paren = inOrder(token("("), ref { expr }, token(")")).skipWrapper().with(cache)

    val power = binaryExpr("^").map { (l, _, r) -> l.pow(r.toInt()) }.with(cache)
    val divide = binaryExpr("/").leftAssoc { (l, _, r) -> l.divide(r) }.with(cache)
    val multiply = binaryExpr("*").leftAssoc { (l, _, r) -> l * r }.with(cache)

    val minus = binaryExpr("-").leftAssoc { (l, _, r) -> l - r }.with(cache)
    val plus = binaryExpr("+").leftAssoc { (l, _, r) -> l + r }.with(cache)

    val expr: Parser<BigDecimal> = oneOfWithPrecedence(
        oneOf(plus, minus),
        oneOf(multiply, divide),
        power,
        paren.nestedPrecedence(),
        number
    ).reset(cache)

    fun evaluate(s: String) = s.parseWith(expr)
}
```
