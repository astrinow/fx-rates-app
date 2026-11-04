package com.fxrates.feature.rates.details.mvi

import com.fxrates.feature.rates.details.mvi.RatesDetailsStore.Message
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore.State

internal fun State.reduce(message: Message): State = when (message) {
    is Message.UpdateHistory -> copy(history = message.history)
}
