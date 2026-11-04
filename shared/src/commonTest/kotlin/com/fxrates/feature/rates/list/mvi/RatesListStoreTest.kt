package com.fxrates.feature.rates.list.mvi

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.fxrates.core.utils.Resource
import com.fxrates.testing.FakeRatesRepository
import com.fxrates.testing.currencyRate
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RatesListStoreTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    beforeSpec {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
        isAssertOnMainThreadEnabled = true
    }

    fun createStore(repository: FakeRatesRepository): RatesListStore {
        val factory = RatesListStoreFactory(storeFactory = DefaultStoreFactory(), repository = repository)
        return factory.createStore(base = "USD")
    }

    test("the rates are loaded for the base currency on start") {
        val repository = FakeRatesRepository().apply { latestRates = Result.success(listOf(currencyRate())) }

        val store = createStore(repository)

        repository.requestedBases shouldBe listOf("USD")
        store.state.rates shouldBe Resource.Data(listOf(currencyRate()))
    }

    test("a failed load ends in an error and a refresh loads again") {
        val repository = FakeRatesRepository().apply { latestRates = Result.failure(IllegalStateException("offline")) }
        val store = createStore(repository)
        store.state.rates.shouldBeInstanceOf<Resource.Error>()

        repository.latestRates = Result.success(listOf(currencyRate()))
        store.accept(RatesListStore.Intent.OnRefresh)

        repository.requestedBases shouldBe listOf("USD", "USD")
        store.state.rates shouldBe Resource.Data(listOf(currencyRate()))
    }

    test("a query change is kept in the state") {
        val store = createStore(FakeRatesRepository())

        store.accept(RatesListStore.Intent.OnQueryChange("eu"))

        store.state.query shouldBe "eu"
    }

    test("a currency click opens the currency with the current base") {
        runTest(testDispatcher) {
            val store = createStore(FakeRatesRepository())
            var label: RatesListStore.Label? = null
            val job = launch { label = store.labels.first() }

            store.accept(RatesListStore.Intent.OnCurrencyClick("EUR"))
            job.join()

            label shouldBe RatesListStore.Label.OpenCurrency(base = "USD", code = "EUR")
        }
    }

})
