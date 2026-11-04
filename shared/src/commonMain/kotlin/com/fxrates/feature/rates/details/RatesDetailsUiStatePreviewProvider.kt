package com.fxrates.feature.rates.details

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.fxrates.feature.rates.details.RatesDetailsUiState.Content
import com.fxrates.feature.rates.details.model.RateHistoryItem
import com.fxrates.feature.rates.details.model.RateHistoryTrend
import com.fxrates.ui.Strings

internal class RatesDetailsUiStatePreviewProvider : PreviewParameterProvider<RatesDetailsUiState> {

    private val items = Content.Items(
        items = listOf(
            RateHistoryItem(date = "2026-11-03", rate = "0.8945"),
            RateHistoryItem(date = "2026-11-02", rate = "0.8916"),
            RateHistoryItem(date = "2026-10-30", rate = "0.8822"),
        ),
        currentRate = "0.8945",
        currentDate = "2026-11-03",
        absoluteChange = "+0.0123",
        percentChange = "+1.39%",
        trend = RateHistoryTrend.Up,
    )

    private val content = RatesDetailsUiState(
        title = Strings.currencyPair(base = "USD", code = "EUR"),
        subtitle = Strings.rateHistorySubtitle(base = "USD", startDate = "2026-10-06", endDate = "2026-11-04"),
        isRetryEnabled = true,
        content = items,
    )

    override val values: Sequence<RatesDetailsUiState> = sequenceOf(
        content,
        content.copy(isRetryEnabled = false, content = Content.Loading),
        content.copy(content = Content.Error),
        content.copy(content = Content.Empty),
        content.copy(
            content = items.copy(
                items = items.items.take(1),
                absoluteChange = Strings.UNAVAILABLE_VALUE,
                percentChange = Strings.UNAVAILABLE_VALUE,
                trend = RateHistoryTrend.Flat,
            ),
        ),
        content.copy(
            content = items.copy(absoluteChange = "-0.0123", percentChange = "-1.39%", trend = RateHistoryTrend.Down),
        ),
        content.copy(
            content = items.copy(absoluteChange = "0.0000", percentChange = "0.00%", trend = RateHistoryTrend.Flat),
        ),
    )

}
