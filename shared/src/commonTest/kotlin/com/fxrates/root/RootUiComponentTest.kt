package com.fxrates.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.fxrates.feature.rates.details.RatesDetailsUiComponentDefault
import com.fxrates.feature.rates.details.RatesDetailsUiState
import com.fxrates.feature.rates.details.RatesDetailsUiStateConverter
import com.fxrates.feature.rates.details.mvi.RatesDetailsStoreFactory
import com.fxrates.feature.rates.list.RatesListUiComponentDefault
import com.fxrates.feature.rates.list.RatesListUiState
import com.fxrates.feature.rates.list.RatesListUiStateConverter
import com.fxrates.feature.rates.list.mvi.RatesListStoreFactory
import com.fxrates.testing.FakeRatesRepository
import com.fxrates.testing.currencyRate
import com.fxrates.testing.rateHistory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RootUiComponentTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
        isAssertOnMainThreadEnabled = true
    }

    test("selecting a currency pushes details with both identifiers and loads its history") {
        runTest(testDispatcher) {
            NavigationFixture(testDispatcher).use { fixture ->
                runCurrent()
                val list = fixture.root.stack.value.active.instance
                    .shouldBeInstanceOf<RootUiComponent.Child.RatesList>()

                list.component.onCurrencyClick("JPY")
                runCurrent()

                val stack = fixture.root.stack.value
                stack.active.configuration shouldBe
                    RootUiComponentDefault.Config.RatesDetails(base = "USD", code = "JPY")
                stack.backStack.single().instance shouldBe list
                val details = stack.active.instance.shouldBeInstanceOf<RootUiComponent.Child.RatesDetails>().component
                details.state.value.title shouldBe "USD / JPY"
                fixture.repository.requestedPairs shouldBe listOf("USD" to "JPY")
            }
        }
    }

    test("back returns to the same list with its query and data without reloading") {
        runTest(testDispatcher) {
            NavigationFixture(testDispatcher).use { fixture ->
                runCurrent()
                val list = fixture.root.stack.value.active.instance
                    .shouldBeInstanceOf<RootUiComponent.Child.RatesList>()
                list.component.onQueryChange("jp")
                runCurrent()
                val stateBeforeOpening = list.component.state.value
                list.component.onCurrencyClick("JPY")
                runCurrent()
                fixture.repository.latestRates = Result.failure(IllegalStateException("must not reload"))
                val details = fixture.root.stack.value.active.instance
                    .shouldBeInstanceOf<RootUiComponent.Child.RatesDetails>().component

                details.onBack()
                runCurrent()

                fixture.root.stack.value.active.instance shouldBe list
                fixture.root.stack.value.backStack shouldBe emptyList()
                list.component.state.value shouldBe stateBeforeOpening
                list.component.state.value.query shouldBe "jp"
                val content = list.component.state.value.content.shouldBeInstanceOf<RatesListUiState.Content.Items>()
                content.items.single().code shouldBe "JPY"
                fixture.repository.requestedBases shouldBe listOf("USD")
            }
        }
    }

    test("system back pops details through the existing Decompose stack") {
        runTest(testDispatcher) {
            NavigationFixture(testDispatcher).use { fixture ->
                runCurrent()
                val list = fixture.root.stack.value.active.instance
                    .shouldBeInstanceOf<RootUiComponent.Child.RatesList>()
                list.component.onCurrencyClick("EUR")
                runCurrent()

                fixture.backDispatcher.back() shouldBe true
                runCurrent()

                fixture.root.stack.value.active.instance shouldBe list
                fixture.backDispatcher.isEnabled shouldBe false
            }
        }
    }

    test("a rapid double click does not create duplicate detail screens or requests") {
        runTest(testDispatcher) {
            NavigationFixture(testDispatcher).use { fixture ->
                runCurrent()
                val list = fixture.root.stack.value.active.instance
                    .shouldBeInstanceOf<RootUiComponent.Child.RatesList>()

                repeat(2) { _ -> list.component.onCurrencyClick("EUR") }
                runCurrent()

                fixture.root.stack.value.backStack.size shouldBe 1
                fixture.repository.requestedPairs shouldBe listOf("USD" to "EUR")
            }
        }
    }

    test("popping details cancels its active history request") {
        runTest(testDispatcher) {
            NavigationFixture(testDispatcher).use { fixture ->
                var cancelled = false
                fixture.repository.beforeHistoryLoad = {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled = true
                    }
                }
                runCurrent()
                val list = fixture.root.stack.value.active.instance
                    .shouldBeInstanceOf<RootUiComponent.Child.RatesList>()
                list.component.onCurrencyClick("EUR")
                runCurrent()
                val details = fixture.root.stack.value.active.instance
                    .shouldBeInstanceOf<RootUiComponent.Child.RatesDetails>().component
                details.state.value.content shouldBe RatesDetailsUiState.Content.Loading

                details.onBack()
                runCurrent()

                cancelled shouldBe true
                fixture.root.stack.value.active.instance shouldBe list
            }
        }
    }

    test("restoring the stack retains the selected base and currency identifiers") {
        runTest(testDispatcher) {
            val savedState = NavigationFixture(testDispatcher).use { fixture ->
                runCurrent()
                val list = fixture.root.stack.value.active.instance
                    .shouldBeInstanceOf<RootUiComponent.Child.RatesList>()
                list.component.onCurrencyClick("JPY")
                runCurrent()
                fixture.stateKeeper.save()
            }

            NavigationFixture(testDispatcher, stateKeeper = StateKeeperDispatcher(savedState)).use { fixture ->
                runCurrent()

                val stack = fixture.root.stack.value
                stack.active.configuration shouldBe
                    RootUiComponentDefault.Config.RatesDetails(base = "USD", code = "JPY")
                stack.active.instance.shouldBeInstanceOf<RootUiComponent.Child.RatesDetails>()
                    .component.state.value.title shouldBe "USD / JPY"
                stack.backStack.single().instance.shouldBeInstanceOf<RootUiComponent.Child.RatesList>()
                fixture.repository.requestedPairs shouldBe listOf("USD" to "JPY")
            }
        }
    }

})

private class NavigationFixture(
    dispatcher: CoroutineDispatcher,
    val stateKeeper: StateKeeperDispatcher = StateKeeperDispatcher(),
) : AutoCloseable {

    private val lifecycle = LifecycleRegistry().apply { resume() }

    val backDispatcher = BackDispatcher()

    val repository = FakeRatesRepository().apply {
        latestRates = Result.success(listOf(currencyRate(), currencyRate(code = "JPY", name = "Japanese Yen")))
        rateHistory = Result.success(rateHistory())
    }

    val root = RootUiComponentDefault(
        componentContext = DefaultComponentContext(lifecycle, stateKeeper = stateKeeper, backHandler = backDispatcher),
        ratesListFactory = object : RatesListUiComponentDefault.Factory {
            override fun create(
                componentContext: ComponentContext,
                base: String,
                callbacks: RatesListUiComponentDefault.Callbacks,
            ): RatesListUiComponentDefault {
                return RatesListUiComponentDefault(
                    componentContext = componentContext,
                    base = base,
                    callbacks = callbacks,
                    storeFactory = RatesListStoreFactory(DefaultStoreFactory(), repository),
                    uiStateConverter = RatesListUiStateConverter(),
                    computationDispatcher = dispatcher,
                )
            }
        },
        ratesDetailsFactory = object : RatesDetailsUiComponentDefault.Factory {
            override fun create(
                componentContext: ComponentContext,
                base: String,
                code: String,
                callbacks: RatesDetailsUiComponentDefault.Callbacks,
            ): RatesDetailsUiComponentDefault {
                return RatesDetailsUiComponentDefault(
                    componentContext = componentContext,
                    base = base,
                    code = code,
                    callbacks = callbacks,
                    storeFactory = RatesDetailsStoreFactory(DefaultStoreFactory(), repository),
                    uiStateConverter = RatesDetailsUiStateConverter(),
                    computationDispatcher = dispatcher,
                )
            }
        },
    )

    override fun close() {
        lifecycle.destroy()
    }

}
