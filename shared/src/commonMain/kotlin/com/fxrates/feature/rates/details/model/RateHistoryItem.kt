package com.fxrates.feature.rates.details.model

import androidx.compose.runtime.Immutable

@Immutable
data class RateHistoryItem(
    val date: String,
    val rate: String,
)
