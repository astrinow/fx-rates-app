package com.fxrates.data.repository

import com.fxrates.data.remote.dto.TimeSeriesDto
import com.fxrates.domain.model.CurrencyRate
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class RatesConverterTest : FunSpec({

    val converter = RatesConverter()

    test("the last date is the latest rate and the one before it is the previous rate") {
        val timeSeries = TimeSeriesDto(
            base = "USD",
            startDate = "2026-10-29",
            endDate = "2026-11-02",
            rates = mapOf(
                "2026-11-02" to mapOf("EUR" to 0.89158),
                "2026-10-29" to mapOf("EUR" to 0.89847),
                "2026-10-30" to mapOf("EUR" to 0.89509),
            ),
        )

        val rates = converter.toLatestRates(timeSeries, currencies = mapOf("EUR" to "Euro"))

        rates shouldHaveSize 1
        with(rates.single()) {
            code shouldBe "EUR"
            name shouldBe "Euro"
            date shouldBe LocalDate(2026, 11, 2)
            rate shouldBe "0.89158".toBigDecimal()
            previousRate shouldBe "0.89509".toBigDecimal()
        }
    }

    test("a single date gives no previous rate and an unknown code keeps its code as the name") {
        val timeSeries = TimeSeriesDto(
            base = "USD",
            startDate = "2026-11-02",
            endDate = "2026-11-02",
            rates = mapOf("2026-11-02" to mapOf("XAU" to 0.0003)),
        )

        val rate = converter.toLatestRates(timeSeries, currencies = emptyMap()).single()

        rate.name shouldBe "XAU"
        rate.previousRate shouldBe null
    }

    test("the rates are sorted by code") {
        val timeSeries = TimeSeriesDto(
            base = "USD",
            startDate = "2026-11-02",
            endDate = "2026-11-02",
            rates = mapOf("2026-11-02" to mapOf("JPY" to 157.9, "CHF" to 0.8, "EUR" to 0.89)),
        )

        val codes = converter.toLatestRates(timeSeries, currencies = emptyMap()).map(CurrencyRate::code)

        codes shouldBe listOf("CHF", "EUR", "JPY")
    }

    test("an empty time series gives no rates") {
        val timeSeries = TimeSeriesDto(base = "USD", startDate = "", endDate = "", rates = emptyMap())

        converter.toLatestRates(timeSeries, currencies = emptyMap()).shouldBeEmpty()
    }

})
