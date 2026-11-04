package com.fxrates.feature.rates.list.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.fxrates.core.utils.Empty
import com.fxrates.core.utils.Resource
import com.fxrates.domain.model.CurrencyRate

internal class RatesListStore(
    store: Store<Intent, State, Label>,
) : Store<RatesListStore.Intent, RatesListStore.State, RatesListStore.Label> by store {

    sealed class Intent {

        data object OnRefresh : Intent()

        data class OnQueryChange(val query: String) : Intent()

        data class OnCurrencyClick(val code: String) : Intent()

    }

    sealed class Action {

        data class LoadRates(val base: String) : Action()

    }

    sealed class Message {

        data class UpdateRates(val rates: Resource<List<CurrencyRate>>) : Message()

        data class UpdateQuery(val query: String) : Message()

    }

    sealed class Label {

        data class OpenCurrency(val base: String, val code: String) : Label()

    }

    data class State(
        val base: String,
        val rates: Resource<List<CurrencyRate>> = Resource.Loading,
        val query: String = String.Empty,
    )

}
