package com.fxrates.data.repository

import com.fxrates.data.remote.dto.TimeSeriesDto
import com.fxrates.domain.model.CurrencyRate
import com.fxrates.domain.model.CurrencyRateHistory
import com.fxrates.domain.model.RateHistoryPoint
import com.fxrates.testing.timeSeries
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

class RatesConverterTest : FunSpec({

    val converter = RatesConverter()

    test("the last date is the latest rate and the one before it is the previous rate") {
        val timeSeries = timeSeries(
            base = "USD",
            startDate = "2026-10-29",
            endDate = "2026-11-02",
            rates = mapOf(
                "2026-11-02" to mapOf("EUR" to "0.89158"),
                "2026-10-29" to mapOf("EUR" to "0.89847"),
                "2026-10-30" to mapOf("EUR" to "0.89509"),
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
        val timeSeries = timeSeries(
            base = "USD",
            startDate = "2026-11-02",
            endDate = "2026-11-02",
            rates = mapOf("2026-11-02" to mapOf("XAU" to "0.0003")),
        )

        val rate = converter.toLatestRates(timeSeries, currencies = emptyMap()).single()

        rate.name shouldBe "XAU"
        rate.previousRate shouldBe null
    }

    test("the rates are sorted by code") {
        val timeSeries = timeSeries(
            base = "USD",
            startDate = "2026-11-02",
            endDate = "2026-11-02",
            rates = mapOf("2026-11-02" to mapOf("JPY" to "157.9", "CHF" to "0.8", "EUR" to "0.89")),
        )

        val codes = converter.toLatestRates(timeSeries, currencies = emptyMap()).map(CurrencyRate::code)

        codes shouldBe listOf("CHF", "EUR", "JPY")
    }

    test("an empty time series gives no rates") {
        val timeSeries = TimeSeriesDto(base = "USD", startDate = "", endDate = "", rates = emptyMap())

        converter.toLatestRates(timeSeries, currencies = emptyMap()).shouldBeEmpty()
    }

    val startDate = LocalDate(2026, 10, 6)
    val endDate = LocalDate(2026, 11, 4)

    fun history(timeSeries: TimeSeriesDto): CurrencyRateHistory {
        return converter.toRateHistory(
            timeSeries = timeSeries,
            base = "USD",
            code = "EUR",
            startDate = startDate,
            endDate = endDate,
        )
    }

    test("history selects the currency and sorts quotes by ascending date") {
        val timeSeries = timeSeries(
            rates = mapOf(
                "2026-11-04" to mapOf("EUR" to "0.91", "JPY" to "160"),
                "2026-10-06" to mapOf("EUR" to "0.89", "JPY" to "157"),
                "2026-10-20" to mapOf("EUR" to "0.90"),
            ),
        )

        val result = history(timeSeries)

        result.base shouldBe "USD"
        result.code shouldBe "EUR"
        result.startDate shouldBe startDate
        result.endDate shouldBe endDate
        result.points shouldBe listOf(
            RateHistoryPoint(startDate, "0.89".toBigDecimal()),
            RateHistoryPoint(LocalDate(2026, 10, 20), "0.90".toBigDecimal()),
            RateHistoryPoint(endDate, "0.91".toBigDecimal()),
        )
    }

    test("both window boundaries are included and quotes outside the window are excluded") {
        val timeSeries = timeSeries(
            rates = mapOf(
                "2026-10-05" to mapOf("EUR" to "0.88"),
                "2026-10-06" to mapOf("EUR" to "0.89"),
                "2026-11-04" to mapOf("EUR" to "0.91"),
                "2026-11-05" to mapOf("EUR" to "0.92"),
            ),
        )

        history(timeSeries).points.map(RateHistoryPoint::date) shouldBe listOf(startDate, endDate)
    }

    test("missing business days and a missing currency do not create quotes") {
        val timeSeries = timeSeries(
            rates = mapOf(
                "2026-10-30" to mapOf("EUR" to "0.89"),
                "2026-11-02" to mapOf("JPY" to "158"),
                "2026-11-03" to emptyMap(),
                "2026-11-04" to mapOf("EUR" to "0.91"),
            ),
        )

        history(timeSeries).points.map(RateHistoryPoint::date) shouldBe listOf(
            LocalDate(2026, 10, 30),
            endDate,
        )
    }

    test("an empty history keeps the requested window instead of the response dates") {
        val result = history(timeSeries(rates = emptyMap(), startDate = "2026-10-07", endDate = "2026-11-03"))

        result.points.shouldBeEmpty()
        result.startDate shouldBe startDate
        result.endDate shouldBe endDate
    }

    test("a currency absent from every date gives an empty history") {
        val timeSeries = timeSeries(rates = mapOf("2026-11-04" to mapOf("JPY" to "158")))

        history(timeSeries).points.shouldBeEmpty()
    }

    test("a history with only out-of-window quotes is empty") {
        val timeSeries = timeSeries(rates = mapOf("2026-10-05" to mapOf("EUR" to "0.89")))

        history(timeSeries).points.shouldBeEmpty()
    }

    test("a single quote remains a single quote without an invented comparison point") {
        val timeSeries = timeSeries(rates = mapOf("2026-11-02" to mapOf("EUR" to "0.89")))

        history(timeSeries).points shouldBe listOf(
            RateHistoryPoint(LocalDate(2026, 11, 2), "0.89".toBigDecimal()),
        )
    }

    test("history and latest rates preserve digits beyond double precision") {
        val decimal = "0.123456789012345678901234567890123456789"
        val timeSeries = timeSeries(rates = mapOf("2026-11-04" to mapOf("EUR" to decimal)))

        history(timeSeries).points.single().rate shouldBe decimal.toBigDecimal()
        converter.toLatestRates(timeSeries, emptyMap()).single().rate shouldBe decimal.toBigDecimal()
    }

    test("the previous rate also retains its original decimal precision") {
        val previous = "0.12345678901234567890123456789"
        val timeSeries = timeSeries(
            rates = mapOf(
                "2026-11-03" to mapOf("EUR" to previous),
                "2026-11-04" to mapOf("EUR" to "0.9"),
            ),
        )

        converter.toLatestRates(timeSeries, emptyMap()).single().previousRate shouldBe previous.toBigDecimal()
    }

    test("scientific notation and zero are valid decimal quotes") {
        val timeSeries = timeSeries(
            rates = mapOf(
                "2026-11-03" to mapOf("EUR" to "1.234567890123456789e-20"),
                "2026-11-04" to mapOf("EUR" to "0"),
            ),
        )

        history(timeSeries).points.map(RateHistoryPoint::rate) shouldBe listOf(
            "1.234567890123456789e-20".toBigDecimal(),
            "0".toBigDecimal(),
        )
    }

    test("a quoted string or null quote fails instead of becoming zero or missing data") {
        listOf(JsonPrimitive("0.89"), JsonNull).forEach { invalid ->
            val timeSeries = TimeSeriesDto(
                base = "USD",
                startDate = "2026-10-06",
                endDate = "2026-11-04",
                rates = mapOf("2026-11-04" to mapOf("EUR" to invalid)),
            )

            shouldThrow<IllegalArgumentException> { history(timeSeries) }
            shouldThrow<IllegalArgumentException> { converter.toLatestRates(timeSeries, emptyMap()) }
        }
    }

    test("an invalid date fails instead of being hidden as an empty history") {
        val timeSeries = timeSeries(rates = mapOf("invalid" to mapOf("EUR" to "0.89")))

        shouldThrow<IllegalArgumentException> { history(timeSeries) }
    }

})
