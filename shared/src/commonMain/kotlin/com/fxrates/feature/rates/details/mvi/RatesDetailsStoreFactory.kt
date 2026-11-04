package com.fxrates.feature.rates.details.mvi

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.fxrates.domain.repository.RatesRepository
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore.Action
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore.State
import me.tatarka.inject.annotations.Inject

@Inject
internal class RatesDetailsStoreFactory(
    private val storeFactory: StoreFactory,
    private val ratesRepository: RatesRepository,
) {

    fun createStore(base: String, code: String): RatesDetailsStore {
        val store = storeFactory.create(
            name = "RatesDetailsStore",
            initialState = State(base = base, code = code),
            bootstrapper = SimpleBootstrapper(Action.LoadHistory(base = base, code = code)),
            executorFactory = { RatesDetailsExecutor(ratesRepository = ratesRepository) },
            reducer = { message -> reduce(message) },
        )
        return RatesDetailsStore(store)
    }

}
