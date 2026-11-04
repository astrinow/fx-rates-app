package com.fxrates.feature.rates.list.mvi

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.fxrates.domain.repository.RatesRepository
import com.fxrates.feature.rates.list.mvi.RatesListStore.Action
import com.fxrates.feature.rates.list.mvi.RatesListStore.State
import me.tatarka.inject.annotations.Inject

@Inject
internal class RatesListStoreFactory(
    private val storeFactory: StoreFactory,
    private val repository: RatesRepository,
) {

    fun createStore(base: String): RatesListStore {
        val store = storeFactory.create(
            name = "RatesListStore",
            initialState = State(base = base),
            bootstrapper = SimpleBootstrapper(Action.LoadRates(base = base)),
            executorFactory = { RatesListExecutor(repository = repository) },
            reducer = { message -> reduce(message) },
        )
        return RatesListStore(store)
    }

}
