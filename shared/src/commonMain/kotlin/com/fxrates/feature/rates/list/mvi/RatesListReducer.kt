package com.fxrates.feature.rates.list.mvi

import com.fxrates.feature.rates.list.mvi.RatesListStore.Message
import com.fxrates.feature.rates.list.mvi.RatesListStore.State

internal fun State.reduce(message: Message): State = when (message) {
    is Message.UpdateRates -> copy(rates = message.rates)
    is Message.UpdateQuery -> copy(query = message.query)
}
