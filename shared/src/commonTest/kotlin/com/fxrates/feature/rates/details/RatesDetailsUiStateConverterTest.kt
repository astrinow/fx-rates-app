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
        uiState.title shouldBe "USD / EUR"
        uiState.subtitle shouldBe "1 USD · 2026-10-06 — 2026-11-04"
        uiState.isRetryEnabled shouldBe true
    }

    test("one available point has a formatted rate and no change") {
        val history = rateHistory(
            points = listOf(RateHistoryPoint(LocalDate(2026, 11, 2), "0.891583".toBigDecimal())),
        )

        val content = converter.createUiState(state(history)).content.shouldBeInstanceOf<Content.Items>()

        content.items shouldBe listOf(RateHistoryItem(date = "2026-11-02", rate = "0.8916"))
        content.currentRate shouldBe "0.8916"
        content.currentDate shouldBe "2026-11-02"
        content.trend shouldBe RateHistoryTrend.Flat
        content.absoluteChange shouldBe Strings.UNAVAILABLE_VALUE
        content.percentChange shouldBe Strings.UNAVAILABLE_VALUE
    }

    test("an increase has signed absolute and percentage changes") {
        val content = converter.createUiState(state(history("2", "3"))).content.shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe "+1.0000"
        content.percentChange shouldBe "+50.00%"
        content.trend shouldBe RateHistoryTrend.Up
    }

    test("a decrease has negative absolute and percentage changes") {
        val content = converter.createUiState(state(history("4", "3"))).content.shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe "-1.0000"
        content.percentChange shouldBe "-25.00%"
        content.trend shouldBe RateHistoryTrend.Down
    }

    test("equal rates have zero change without a plus sign") {
        val content = converter.createUiState(state(history("2", "2"))).content.shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe "0.0000"
        content.percentChange shouldBe "0.00%"
        content.trend shouldBe RateHistoryTrend.Flat
    }

    test("a zero initial rate preserves absolute change and leaves percentage unavailable") {
        val content = converter.createUiState(state(history("0", "3"))).content.shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe "+3.0000"
        content.percentChange shouldBe Strings.UNAVAILABLE_VALUE
        content.trend shouldBe RateHistoryTrend.Up
    }

    test("two zero rates have zero absolute change and no percentage") {
        val content = converter.createUiState(state(history("0", "0"))).content.shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe "0.0000"
        content.percentChange shouldBe Strings.UNAVAILABLE_VALUE
    }

    test("changes that round to zero have no sign") {
        val content = converter.createUiState(state(history("2", "1.999999"))).content
            .shouldBeInstanceOf<Content.Items>()

        content.absoluteChange shouldBe "0.0000"
        content.percentChange shouldBe "0.00%"
    }

    test("history is reversed for display while changes compare its oldest and newest available points") {
        val history = rateHistory(
            points = listOf(
                RateHistoryPoint(LocalDate(2026, 10, 30), "2".toBigDecimal()),
                RateHistoryPoint(LocalDate(2026, 11, 2), "10".toBigDecimal()),
                RateHistoryPoint(LocalDate(2026, 11, 3), "3".toBigDecimal()),
            ),
        )

        val content = converter.createUiState(state(history)).content.shouldBeInstanceOf<Content.Items>()

        content.items shouldBe listOf(
            RateHistoryItem(date = "2026-11-03", rate = "3.0000"),
            RateHistoryItem(date = "2026-11-02", rate = "10.0000"),
            RateHistoryItem(date = "2026-10-30", rate = "2.0000"),
        )
        content.absoluteChange shouldBe "+1.0000"
        content.percentChange shouldBe "+50.00%"
        content.currentRate shouldBe "3.0000"
        content.currentDate shouldBe "2026-11-03"
        history.points.first().date shouldBe LocalDate(2026, 10, 30)
    }

    test("loading disables retry and does not display a period") {
        val uiState = converter.createUiState(RatesDetailsStore.State(base = "USD", code = "EUR"))

        uiState.content shouldBe Content.Loading
        uiState.title shouldBe "USD / EUR"
        uiState.subtitle shouldBe String.Empty
        uiState.isRetryEnabled shouldBe false
    }

    test("an error enables retry and does not display a period") {
        val state = RatesDetailsStore.State(
            base = "USD",
            code = "EUR",
            history = Resource.Error(IllegalStateException("offline")),
        )

        val uiState = converter.createUiState(state)

        uiState.content shouldBe Content.Error
        uiState.title shouldBe "USD / EUR"
        uiState.subtitle shouldBe String.Empty
        uiState.isRetryEnabled shouldBe true
    }

})
