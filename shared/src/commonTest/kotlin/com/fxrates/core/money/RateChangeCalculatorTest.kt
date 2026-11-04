package com.fxrates.core.money

import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class RateChangeCalculatorTest : FunSpec({

    test("an unchanged rate gives a zero change") {
        val change = RateChangeCalculator.calculate(previous = "0.9".toBigDecimal(), current = "0.9".toBigDecimal())

        change.absolute.isZero() shouldBe true
        change.percent shouldBe "0".toBigDecimal()
    }

    test("the absolute change is the difference between the rates") {
        val change = RateChangeCalculator.calculate(
            previous = "157.25".toBigDecimal(),
            current = "157.9".toBigDecimal(),
        )

        change.absolute shouldBe "0.65".toBigDecimal()
    }

    test("the percent uses the previous rate as its denominator") {
        val change = RateChangeCalculator.calculate(previous = "2".toBigDecimal(), current = "3".toBigDecimal())

        change.absolute shouldBe "1".toBigDecimal()
        change.percent shouldBe "50".toBigDecimal()
    }

    test("a falling rate gives the exact negative percent") {
        val change = RateChangeCalculator.calculate(previous = "2".toBigDecimal(), current = "1.5".toBigDecimal())

        change.absolute shouldBe "-0.5".toBigDecimal()
        change.percent shouldBe "-25".toBigDecimal()
    }

    test("a zero previous rate still gives an absolute change but no percent") {
        val change = RateChangeCalculator.calculate(previous = "0".toBigDecimal(), current = "1".toBigDecimal())

        change.absolute shouldBe "1".toBigDecimal()
        change.percent.shouldBeNull()
    }

    test("two zero rates give a zero absolute change and no percent") {
        val change = RateChangeCalculator.calculate(previous = "0".toBigDecimal(), current = "0".toBigDecimal())

        change.absolute.isZero() shouldBe true
        change.percent.shouldBeNull()
    }

    test("a zero current rate is a fall of one hundred percent") {
        val change = RateChangeCalculator.calculate(previous = "2".toBigDecimal(), current = "0".toBigDecimal())

        change.absolute shouldBe "-2".toBigDecimal()
        change.percent shouldBe "-100".toBigDecimal()
    }

    test("a repeating percent is rounded to 34 significant decimal digits") {
        val change = RateChangeCalculator.calculate(previous = "3".toBigDecimal(), current = "4".toBigDecimal())

        change.percent shouldBe "33.33333333333333333333333333333333".toBigDecimal()
    }

    test("a difference beyond double precision is preserved") {
        val change = RateChangeCalculator.calculate(
            previous = "1000000000000000000000000000000".toBigDecimal(),
            current = "1000000000000000000000000000000.0001".toBigDecimal(),
        )

        change.absolute shouldBe "0.0001".toBigDecimal()
        change.percent shouldBe "1e-32".toBigDecimal()
    }

    test("very small rates retain their relative change") {
        val change = RateChangeCalculator.calculate(previous = "1e-30".toBigDecimal(), current = "2e-30".toBigDecimal())

        change.absolute shouldBe "1e-30".toBigDecimal()
        change.percent shouldBe "100".toBigDecimal()
    }

})
