package com.fxrates.feature.rates.list

import com.fxrates.core.utils.Empty
import com.fxrates.core.utils.Resource
import com.fxrates.feature.rates.list.RatesListUiState.Content
import com.fxrates.feature.rates.list.model.Trend
import com.fxrates.feature.rates.list.mvi.RatesListStore
import com.fxrates.testing.currencyRate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf

class RatesListUiStateConverterTest : FunSpec({

    val converter = RatesListUiStateConverter()

    val rates = listOf(
        currencyRate(code = "EUR", name = "Euro", rate = "0.89", previousRate = "0.88"),
        currencyRate(code = "JPY", name = "Japanese Yen", rate = "157.9", previousRate = "158.1"),
        currencyRate(code = "CHF", name = "Swiss Franc", rate = "0.8", previousRate = null),
    )

    fun state(query: String = String.Empty): RatesListStore.State {
        return RatesListStore.State(base = "USD", rates = Resource.Data(rates), query = query)
    }

    test("every rate becomes a row with a formatted rate") {
        val content = converter.createUiState(state()).content.shouldBeInstanceOf<Content.Items>()

        content.items.map { item -> item.code } shouldBe listOf("EUR", "JPY", "CHF")
        content.items.first().rate shouldBe "0.8900"
    }

    test("the trend follows the direction of the change") {
        val items = converter.createUiState(state()).content.shouldBeInstanceOf<Content.Items>().items

        items.map { item -> item.trend } shouldBe listOf(Trend.Up, Trend.Down, Trend.Flat)
        items[0].change?.shouldStartWith("+")
        items[1].change?.shouldStartWith("-")
        items[2].change shouldBe null
    }

    test("the query filters by code and by name, ignoring case") {
        val byCode = converter.createUiState(state(query = "jp")).content.shouldBeInstanceOf<Content.Items>()
        val byName = converter.createUiState(state(query = "swiss")).content.shouldBeInstanceOf<Content.Items>()

        byCode.items.map { item -> item.code } shouldBe listOf("JPY")
        byName.items.map { item -> item.code } shouldBe listOf("CHF")
    }

    test("a query that matches nothing gives the nothing-found state") {
        converter.createUiState(state(query = "xyz")).content shouldBe Content.NothingFound
    }

    test("the subtitle names the base currency and the date of the rates") {
        converter.createUiState(state()).subtitle shouldBe "1 USD · 2026-11-02"
    }

    test("loading disables refresh") {
        val uiState = converter.createUiState(RatesListStore.State(base = "USD"))

        uiState.content shouldBe Content.Loading
        uiState.isRefreshEnabled shouldBe false
    }

})
