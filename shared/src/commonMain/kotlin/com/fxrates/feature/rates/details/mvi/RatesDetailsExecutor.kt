package com.fxrates.feature.rates.details.mvi

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.fxrates.core.utils.resourceFlow
import com.fxrates.domain.repository.RatesRepository
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore.Action
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore.Intent
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore.Label
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore.Message
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore.State
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class RatesDetailsExecutor(
    private val ratesRepository: RatesRepository,
) : CoroutineExecutor<Intent, Action, State, Message, Label>() {

    private var loadingJob: Job? = null

    override fun executeAction(action: Action) = when (action) {
        is Action.LoadHistory -> loadHistory(action)
    }

    override fun executeIntent(intent: Intent) = when (intent) {
        is Intent.OnRetry -> forward(Action.LoadHistory(base = state().base, code = state().code))
        is Intent.OnBack -> publish(Label.Back)
    }

    private fun loadHistory(action: Action.LoadHistory) {
        if (loadingJob?.isActive == true) return
        loadingJob = resourceFlow { ratesRepository.getRateHistory(base = action.base, code = action.code) }
            .onEach { history -> dispatch(Message.UpdateHistory(history)) }
            .launchIn(scope)
    }

}
