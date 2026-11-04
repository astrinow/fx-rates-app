package com.fxrates.feature.rates.details.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.fxrates.core.utils.Resource
import com.fxrates.domain.model.CurrencyRateHistory

internal class RatesDetailsStore(
    store: Store<Intent, State, Label>,
) : Store<RatesDetailsStore.Intent, RatesDetailsStore.State, RatesDetailsStore.Label> by store {

    sealed class Intent {

        data object OnRetry : Intent()

        data object OnBack : Intent()

    }

    sealed class Action {

        data class LoadHistory(val base: String, val code: String) : Action()

    }

    sealed class Message {

        data class UpdateHistory(val history: Resource<CurrencyRateHistory>) : Message()

    }

    sealed class Label {

        data object Back : Label()

    }

    data class State(
        val base: String,
        val code: String,
        val history: Resource<CurrencyRateHistory> = Resource.Loading,
    )

}
