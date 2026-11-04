package com.fxrates.feature.rates.list.mvi

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.fxrates.core.utils.resourceFlow
import com.fxrates.domain.repository.RatesRepository
import com.fxrates.feature.rates.list.mvi.RatesListStore.Action
import com.fxrates.feature.rates.list.mvi.RatesListStore.Intent
import com.fxrates.feature.rates.list.mvi.RatesListStore.Label
import com.fxrates.feature.rates.list.mvi.RatesListStore.Message
import com.fxrates.feature.rates.list.mvi.RatesListStore.State
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class RatesListExecutor(
    private val repository: RatesRepository,
) : CoroutineExecutor<Intent, Action, State, Message, Label>() {

    override fun executeAction(action: Action) = when (action) {
        is Action.LoadRates -> loadRates(action)
    }

    override fun executeIntent(intent: Intent) = when (intent) {
        is Intent.OnRefresh -> forward(Action.LoadRates(base = state().base))
        is Intent.OnQueryChange -> dispatch(Message.UpdateQuery(intent.query))
        is Intent.OnCurrencyClick -> publish(Label.OpenCurrency(base = state().base, code = intent.code))
    }

    private fun loadRates(action: Action.LoadRates) {
        resourceFlow { repository.getLatestRates(base = action.base) }
            .onEach { rates -> dispatch(Message.UpdateRates(rates)) }
            .launchIn(scope)
    }

}
