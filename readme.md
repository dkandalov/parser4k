# Parser4k

[![Kotlin](https://img.shields.io/badge/kotlin-1.3-blue.svg)](http://kotlinlang.org)
[![Build Status](https://travis-ci.org/dkandalov/parser4k.svg?branch=master)](https://travis-ci.org/dkandalov/parser4k)

THIS IS CURRENTLY WORK-IN-PROGRESS 🍼👶

Parser4k is a recursive descent parser combinator library for Kotlin.

It aims to be:
 - **simple** - very few core concepts, no magic execution workflow or DSL
 - **easy to use** - you can quickly figure out how to write a parser for a small language
 - **production-ready** - not a toy project, good-enough performance to be embedded into real applications

You can get Parser4k from bintray, e.g. in Gradle:
```
repositories {
    maven { setUrl("https://dl.bintray.com/dkandalov/maven") }
}
dependencies {
    implementation "parser4k:parser4k:0.01"
}
```
 
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

### Parsers
In Parser4k `Parser` is an object which takes an `Input` and returns an `Output` if it was able to consume at least part of the input or `null` otherwise.
Where, `Input` is a string and an offset to indicate how much of the string has been consumed. 
And `Output` is payload (i.e. useful data extracted from input) plus the input with shifted offset which can be used by the next `Parser`.
Parser payload can be "mapped" with `.map()` function similar to how List items are mapped.

There are few built-in Parsers which can be created with the following functions:
 - `str()` - parse input from the current offset if it's equal to the string
 - `regex()` - parse input from the current offset as long as it matches the regex
 - `repeat()`, `zeroOrMore()`, `oneOrMore()`, `optional()` - keep parsing input specified amount times
 - `oneOf()` - find and use the first of the specified parsers which was able consume part of the input
 - `inOrder()` - parse input using each of the specified parsers sequentially
 - `ref { parser }`, `::parser.ref()` - workaround for Kotlin to have forward reference to a property
 - `nonRecRef { parser }` - reference to a parser which avoid left-recursive stack overflow (not required when using `OutputCache`)
 - `oneOfWithPrecedence()` - use the first matching parser with arguments order as precedence (lowest to highest), 
   where higher precedence means that parser will attempt to consume input at an "inner level" and will produce payload before lower precedence parsers.
 - `.nestedPrecedence()` - can be used in combination with `oneOfWithPrecedence()` on parsers which have "nested precedence", e.g. parens
 - `.leftAssoc()` - can be used with `inOrder()` to produce left-associative payload (note that it only makes to use when the last `inOrder` parser is a recursive reference)
 - `.with(outputCache)` - cache parsers output (should be used with any non-toy parser combinator to avoid exponential performance issue)
 - `.reset(outputCache)` - should be used on the main parser to reset output cache after processing each input
 - `.with("parserId", parsingLog)` - wrap parser with a logger (can be useful for debugging/understanding what parser is doing, e.g. see `LogTests.kt`) 
 
There are some non-core parsers which are available in `parser4k.commonparsers` package:
 - `.joinedWith()` - wrap parser so that it matches multiple times with specified separator (e.g. a list of arguments to a function separated by commas)
 - `inOrder(...).mapAsBinary{}`, `.leftAssocAsBinary{}` - `map` or `leftAssoc` skipping payload from the parser in the middle
 - `inOrder(...).skipFirst()`, `.skipLast()`, `.skipWrapper()` - skip first, last or both first and last items from payload
 - `commonparsers.Tokens` - parsers for basic bits of input (usually called "tokens"), e.g. `whitespace` or `number`
