package com.fxrates.feature.rates.details

import com.fxrates.core.utils.Empty
import com.fxrates.core.utils.Resource
import com.fxrates.domain.model.CurrencyRateHistory
import com.fxrates.domain.model.RateHistoryPoint
import com.fxrates.feature.rates.details.RatesDetailsUiState.Content
import com.fxrates.feature.rates.details.model.RateHistoryItem
import com.fxrates.feature.rates.details.model.RateHistoryTrend
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore
import com.fxrates.testing.rateHistory
import com.fxrates.ui.Strings
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.LocalDate

class RatesDetailsUiStateConverterTest : FunSpec({

    val converter = RatesDetailsUiStateConverter()

    fun state(history: CurrencyRateHistory): RatesDetailsStore.State {
        return RatesDetailsStore.State(base = history.base, code = history.code, history = Resource.Data(history))
    }

    fun history(previous: String, current: String): CurrencyRateHistory {
        return rateHistory(
            points = listOf(
                RateHistoryPoint(LocalDate(2026, 10, 30), previous.toBigDecimal()),
                RateHistoryPoint(LocalDate(2026, 11, 4), current.toBigDecimal()),
            ),
        )
    }

    test("an empty history is successful empty content with its requested period") {
        val uiState = converter.createUiState(state(rateHistory()))

        uiState.content shouldBe Content.Empty
        uiState.title shouldBe EXPECTED_PAIR
        uiState.subtitle shouldBe "1 USD · 2026-10-06 — 2026-11-04"
        uiState.isRetryEnabled shouldBe true
    }

    test("one available point has a formatted rate and no change") {
        val history = rateHistory(
            points = listOf(RateHistoryPoint(LocalDate(2026, 11, 2), "0.891583".toBigDecimal())),
        )

        val content = converter.createUiState(state(history)).content
            .shouldBeInstanceOf<Content.Items>()

        content.items shouldBe listOf(RateHistoryItem(date = SINGLE_QUOTE_DATE, rate = FORMATTED_SINGLE_RATE))
        content.currentRate shouldBe FORMATTED_SINGLE_RATE
        content.currentDate shouldBe SINGLE_QUOTE_DATE
        content.trend shouldBe RateHistoryTrend.Flat
        content.absoluteChange shouldBe Strings.UNAVAILABLE_VALUE
        content.percentChange shouldBe Strings.UNAVAILABLE_VALUE
    }

    test("an increase has signed absolute and percentage changes") {
        val content = converter.createUiState(state(history(RATE_TWO, RATE_THREE))).content
            .shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe EXPECTED_INCREASE_ABSOLUTE
        content.percentChange shouldBe EXPECTED_INCREASE_PERCENT
        content.trend shouldBe RateHistoryTrend.Up
    }

    test("a decrease has negative absolute and percentage changes") {
        val content = converter.createUiState(state(history("4", RATE_THREE))).content
            .shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe "-1.0000"
        content.percentChange shouldBe "-25.00%"
        content.trend shouldBe RateHistoryTrend.Down
    }

    test("equal rates have zero change without a plus sign") {
        val content = converter.createUiState(state(history(RATE_TWO, RATE_TWO))).content
            .shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe FORMATTED_ZERO_RATE
        content.percentChange shouldBe FORMATTED_ZERO_PERCENT
        content.trend shouldBe RateHistoryTrend.Flat
    }

    test("a zero initial rate preserves absolute change and leaves percentage unavailable") {
        val content = converter.createUiState(state(history(ZERO_RATE, RATE_THREE))).content
            .shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe "+3.0000"
        content.percentChange shouldBe Strings.UNAVAILABLE_VALUE
        content.trend shouldBe RateHistoryTrend.Up
    }

    test("two zero rates have zero absolute change and no percentage") {
        val content = converter.createUiState(state(history(ZERO_RATE, ZERO_RATE))).content
            .shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe FORMATTED_ZERO_RATE
        content.percentChange shouldBe Strings.UNAVAILABLE_VALUE
    }

    test("changes that round to zero have no sign") {
        val content = converter.createUiState(state(history(RATE_TWO, "1.999999"))).content
            .shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe FORMATTED_ZERO_RATE
        content.percentChange shouldBe FORMATTED_ZERO_PERCENT
    }

    test("history is reversed for display while changes compare its oldest and newest available points") {
        val history = rateHistory(
            points = listOf(
                RateHistoryPoint(LocalDate(2026, 10, 30), RATE_TWO.toBigDecimal()),
                RateHistoryPoint(LocalDate(2026, 11, 2), "10".toBigDecimal()),
                RateHistoryPoint(LocalDate(2026, 11, 3), RATE_THREE.toBigDecimal()),
            ),
        )

        val content = converter.createUiState(state(history)).content
            .shouldBeInstanceOf<Content.Items>()

        content.items shouldBe listOf(
            RateHistoryItem(date = LATEST_QUOTE_DATE, rate = FORMATTED_LATEST_RATE),
            RateHistoryItem(date = SINGLE_QUOTE_DATE, rate = "10.0000"),
            RateHistoryItem(date = "2026-10-30", rate = "2.0000"),
        )
        content.absoluteChange shouldBe EXPECTED_INCREASE_ABSOLUTE
        content.percentChange shouldBe EXPECTED_INCREASE_PERCENT
        content.currentRate shouldBe FORMATTED_LATEST_RATE
        content.currentDate shouldBe LATEST_QUOTE_DATE
        history.points.first().date shouldBe LocalDate(2026, 10, 30)
    }

    test("loading disables retry and does not display a period") {
        val uiState = converter.createUiState(RatesDetailsStore.State(base = BASE_CURRENCY, code = QUOTE_CURRENCY))

        uiState.content shouldBe Content.Loading
        uiState.title shouldBe EXPECTED_PAIR
        uiState.subtitle shouldBe String.Empty
        uiState.isRetryEnabled shouldBe false
    }

    test("an error enables retry and does not display a period") {
        val state = RatesDetailsStore.State(
            base = BASE_CURRENCY,
            code = QUOTE_CURRENCY,
            history = Resource.Error(IllegalStateException("offline")),
        )

        val uiState = converter.createUiState(state)

        uiState.content shouldBe Content.Error
        uiState.title shouldBe EXPECTED_PAIR
        uiState.subtitle shouldBe String.Empty
        uiState.isRetryEnabled shouldBe true
    }

}) {

    private companion object {

        const val EXPECTED_PAIR = "USD / EUR"
        const val SINGLE_QUOTE_DATE = "2026-11-02"
        const val FORMATTED_SINGLE_RATE = "0.8916"
        const val RATE_TWO = "2"
        const val RATE_THREE = "3"
        const val EXPECTED_INCREASE_ABSOLUTE = "+1.0000"
        const val EXPECTED_INCREASE_PERCENT = "+50.00%"
        const val FORMATTED_ZERO_RATE = "0.0000"
        const val FORMATTED_ZERO_PERCENT = "0.00%"
        const val ZERO_RATE = "0"
        const val LATEST_QUOTE_DATE = "2026-11-03"
        const val FORMATTED_LATEST_RATE = "3.0000"
        const val BASE_CURRENCY = "USD"
        const val QUOTE_CURRENCY = "EUR"

    }

}
