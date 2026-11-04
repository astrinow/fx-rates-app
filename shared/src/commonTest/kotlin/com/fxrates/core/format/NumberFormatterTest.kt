package com.fxrates.core.format

import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe

class NumberFormatterTest : FunSpec({

    context("formatRate") {
        withTests(
            "157.9" to "157.9000",
            "0.891583" to "0.8916",
            "0.00005" to "0.0001",
            "1" to "1.0000",
        ) { (input, expected) ->
            NumberFormatter.formatRate(input.toBigDecimal()) shouldBe expected
        }
    }

    context("formatSignedPercent") {
        withTests(
            1.234 to "+1.23%",
            -0.5 to "-0.50%",
            0.0 to "0.00%",
            0.004 to "0.00%",
            -0.004 to "0.00%",
        ) { (input, expected) ->
            NumberFormatter.formatSignedPercent(input) shouldBe expected
        }
    }

    context("formatSignedRate") {
        withTests(
            "0.0123" to "+0.0123",
            "-1.5" to "-1.5000",
            "0" to "0.0000",
        ) { (input, expected) ->
            NumberFormatter.formatSignedRate(input.toBigDecimal()) shouldBe expected
        }
    }

})
