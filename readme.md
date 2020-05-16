# Parser4k

[![Build Status](https://travis-ci.org/dkandalov/parser4k.svg?branch=master)](https://travis-ci.org/dkandalov/parser4k)

Parser4k is a recursive descent parser combinator library for Kotlin.
It aims to be:
 - **simple** - very few core concepts, no magic workflow or DSL
 - **easy to use** - you can quickly figure out how to write a parser for a small language
 - **production-ready** - not a toy project, good-enough performance to be embedded into real applications
 
### Example
```kotlin
object MinimalCalculator {
    private val cache = OutputCache<BigDecimal>()

    private val number = regex("\\d+").map { it.toBigDecimal() }
    private val paren = inOrder(token("("), ref { expr }, token(")")).map { (_, it, _) -> it }
    private val power = inOrder(ref { expr }, token("^"), ref { expr }).map { (l, _, r) -> l.pow(r.toInt()) }.with(cache)
    private val divide = inOrder(ref { expr }, token("/"), ref { expr }).leftAssoc { (l, _, r) -> l.divide(r) }.with(cache)
    private val multiply = inOrder(ref { expr }, token("*"), ref { expr }).leftAssoc { (l, _, r) -> l * r }.with(cache)
    private val minus = inOrder(ref { expr }, token("-"), ref { expr }).leftAssoc { (l, _, r) -> l - r }.with(cache)
    private val plus = inOrder(ref { expr }, token("+"), ref { expr }).leftAssoc { (l, _, r) -> l + r }.with(cache)

    private val expr: Parser<BigDecimal> = oneOfWithPrecedence(
        oneOf(plus, minus),
        oneOf(multiply, divide),
        power,
        paren.nestedPrecedence(),
        number
    ).reset(cache)

    fun evaluate(s: String) = s.parseWith(expr)
}
```
