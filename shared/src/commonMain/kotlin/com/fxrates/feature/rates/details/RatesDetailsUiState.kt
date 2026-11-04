package com.fxrates.feature.rates.details

import androidx.compose.runtime.Immutable
import com.fxrates.core.utils.Empty
import com.fxrates.feature.rates.details.model.RateHistoryItem
import com.fxrates.feature.rates.details.model.RateHistoryTrend

@Immutable
internal data class RatesDetailsUiState(
    val title: String = String.Empty,
    val subtitle: String = String.Empty,
    val isRetryEnabled: Boolean = false,
    val content: Content = Content.Loading,
) {

    @Immutable
    sealed class Content {

        data object Loading : Content()

        data object Error : Content()

        data object Empty : Content()

        data class Items(
            val items: List<RateHistoryItem>,
            val currentRate: String,
            val currentDate: String,
            val absoluteChange: String,
            val percentChange: String,
            val trend: RateHistoryTrend,
        ) : Content()

    }

}
