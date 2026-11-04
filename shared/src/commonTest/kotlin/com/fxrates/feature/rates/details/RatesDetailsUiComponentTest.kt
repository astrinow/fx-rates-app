package com.fxrates.feature.rates.details

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.fxrates.feature.rates.details.mvi.RatesDetailsStoreFactory
import com.fxrates.testing.FakeRatesRepository
import com.fxrates.testing.rateHistory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RatesDetailsUiComponentTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
        isAssertOnMainThreadEnabled = true
    }

    test("the component maps history into UI state and forwards back labels to callbacks") {
        runTest(testDispatcher) {
            val lifecycle = LifecycleRegistry().apply { create() }
            val repository = FakeRatesRepository().apply { rateHistory = Result.success(rateHistory()) }
            var backCount = 0
            val component = RatesDetailsUiComponentDefault(
                componentContext = DefaultComponentContext(lifecycle),
                base = "USD",
                code = "EUR",
                callbacks = object : RatesDetailsUiComponentDefault.Callbacks {
                    override fun onBack() {
                        backCount += 1
                    }
                },
                storeFactory = RatesDetailsStoreFactory(DefaultStoreFactory(), repository),
                uiStateConverter = RatesDetailsUiStateConverter(),
                computationDispatcher = testDispatcher,
            )
            try {
                runCurrent()

                component.state.value.content shouldBe RatesDetailsUiState.Content.Empty
                component.state.value.title shouldBe "EUR"
                component.onBack()
                runCurrent()
                backCount shouldBe 1
            } finally {
                lifecycle.destroy()
            }
        }
    }

    test("destroying the component cancels the history request owned by its store") {
        runTest(testDispatcher) {
            val lifecycle = LifecycleRegistry().apply { create() }
            var cancelled = false
            val repository = FakeRatesRepository().apply {
                beforeHistoryLoad = {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled = true
                    }
                }
            }
            RatesDetailsUiComponentDefault(
                componentContext = DefaultComponentContext(lifecycle),
                base = "GBP",
                code = "JPY",
                callbacks = object : RatesDetailsUiComponentDefault.Callbacks {
                    override fun onBack() = Unit
                },
                storeFactory = RatesDetailsStoreFactory(DefaultStoreFactory(), repository),
                uiStateConverter = RatesDetailsUiStateConverter(),
                computationDispatcher = testDispatcher,
            )
            try {
                runCurrent()
                repository.requestedPairs shouldBe listOf("GBP" to "JPY")
            } finally {
                lifecycle.destroy()
            }
            runCurrent()

            cancelled shouldBe true
        }
    }

})
