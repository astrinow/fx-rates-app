package com.fxrates.feature.rates.details.mvi

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.fxrates.core.utils.Resource
import com.fxrates.testing.FakeRatesRepository
import com.fxrates.testing.rateHistory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RatesDetailsStoreTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()
    val history = rateHistory().copy(base = BASE_CURRENCY, code = QUOTE_CURRENCY)

    beforeSpec {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
        isAssertOnMainThreadEnabled = true
    }

    fun createStore(repository: FakeRatesRepository): RatesDetailsStore {
        val factory = RatesDetailsStoreFactory(DefaultStoreFactory(), ratesRepository = repository)
        return factory.createStore(base = BASE_CURRENCY, code = QUOTE_CURRENCY)
    }

    test("the bootstrapper loads history for the supplied currency pair without an intent") {
        runTest(testDispatcher) {
            val repository = FakeRatesRepository().apply { rateHistory = Result.success(history) }
            val store = createStore(repository)
            try {
                store.state.history shouldBe Resource.Loading
                runCurrent()

                repository.requestedPairs shouldBe listOf(BASE_CURRENCY to QUOTE_CURRENCY)
                repository.requestedBases shouldBe emptyList()
                store.state.base shouldBe BASE_CURRENCY
                store.state.code shouldBe QUOTE_CURRENCY
                store.state.history shouldBe Resource.Data(history)
            } finally {
                store.dispose()
            }
        }
    }

    test("a successful load emits loading followed by data") {
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository = FakeRatesRepository().apply {
                rateHistory = Result.success(history)
                beforeHistoryLoad = { gate.await() }
            }
            val store = createStore(repository)
            try {
                val states = mutableListOf<RatesDetailsStore.State>()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { store.states.toList(states) }
                runCurrent()
                store.state.history shouldBe Resource.Loading

                gate.complete(Unit)
                runCurrent()

                val historyStates = states.map { state -> state.history }.distinct()
                historyStates shouldBe listOf(Resource.Loading, Resource.Data(history))
            } finally {
                store.dispose()
            }
        }
    }

    test("a failed load emits loading followed by the original error") {
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val failure = IllegalStateException(OFFLINE_MESSAGE)
            val repository = FakeRatesRepository().apply {
                rateHistory = Result.failure(failure)
                beforeHistoryLoad = { gate.await() }
            }
            val store = createStore(repository)
            try {
                runCurrent()
                store.state.history shouldBe Resource.Loading

                gate.complete(Unit)
                runCurrent()

                store.state.history.shouldBeInstanceOf<Resource.Error>().throwable shouldBe failure
            } finally {
                store.dispose()
            }
        }
    }

    test("retry preserves both parameters and ignores repeated intents before and during the load") {
        runTest(testDispatcher) {
            val repository = FakeRatesRepository().apply {
                rateHistory = Result.failure(IllegalStateException(OFFLINE_MESSAGE))
            }
            val store = createStore(repository)
            try {
                runCurrent()
                store.state.history.shouldBeInstanceOf<Resource.Error>()
                val gate = CompletableDeferred<Unit>()
                repository.rateHistory = Result.success(history)
                repository.beforeHistoryLoad = { gate.await() }

                repeat(3) { _ -> store.accept(RatesDetailsStore.Intent.OnRetry) }
                runCurrent()
                store.state.history shouldBe Resource.Loading
                repeat(3) { _ -> store.accept(RatesDetailsStore.Intent.OnRetry) }
                runCurrent()
                repository.requestedPairs shouldBe listOf(
                    BASE_CURRENCY to QUOTE_CURRENCY,
                    BASE_CURRENCY to QUOTE_CURRENCY,
                )

                gate.complete(Unit)
                runCurrent()

                store.state.history shouldBe Resource.Data(history)
            } finally {
                store.dispose()
            }
        }
    }

    test("retry cannot duplicate the bootstrapper load") {
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository = FakeRatesRepository().apply {
                rateHistory = Result.success(history)
                beforeHistoryLoad = { gate.await() }
            }
            val store = createStore(repository)
            try {
                store.accept(RatesDetailsStore.Intent.OnRetry)
                runCurrent()
                store.accept(RatesDetailsStore.Intent.OnRetry)
                runCurrent()

                repository.requestedPairs shouldBe listOf(BASE_CURRENCY to QUOTE_CURRENCY)

                gate.complete(Unit)
                runCurrent()
                store.state.history shouldBe Resource.Data(history)
            } finally {
                store.dispose()
            }
        }
    }

    test("a completed load allows a subsequent retry") {
        runTest(testDispatcher) {
            val repository = FakeRatesRepository().apply { rateHistory = Result.success(history) }
            val store = createStore(repository)
            try {
                runCurrent()
                store.accept(RatesDetailsStore.Intent.OnRetry)
                runCurrent()

                repository.requestedPairs shouldBe listOf(
                    BASE_CURRENCY to QUOTE_CURRENCY,
                    BASE_CURRENCY to QUOTE_CURRENCY,
                )
                store.state.history shouldBe Resource.Data(history)
            } finally {
                store.dispose()
            }
        }
    }

    test("back publishes a label even while history is loading") {
        runTest(testDispatcher) {
            val repository = FakeRatesRepository().apply { beforeHistoryLoad = { awaitCancellation() } }
            val store = createStore(repository)
            try {
                val label = async(UnconfinedTestDispatcher(testScheduler)) { store.labels.first() }

                store.accept(RatesDetailsStore.Intent.OnBack)

                label.await() shouldBe RatesDetailsStore.Label.Back
                store.state.history shouldBe Resource.Loading
            } finally {
                store.dispose()
            }
        }
    }

    test("disposing the store cancels an active repository request") {
        runTest(testDispatcher) {
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
            val store = createStore(repository)
            try {
                runCurrent()
                repository.requestedPairs shouldBe listOf(BASE_CURRENCY to QUOTE_CURRENCY)
            } finally {
                store.dispose()
            }
            runCurrent()

            cancelled shouldBe true
            store.isDisposed shouldBe true
        }
    }

}) {

    private companion object {

        const val BASE_CURRENCY = "GBP"
        const val QUOTE_CURRENCY = "JPY"
        const val OFFLINE_MESSAGE = "offline"

    }

}
