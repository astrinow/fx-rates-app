package com.fxrates.feature.rates.list

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.fxrates.feature.rates.list.RatesListUiState.Content
import com.fxrates.feature.rates.list.model.RateItem
import com.fxrates.feature.rates.list.model.Trend

internal class RatesListUiStatePreviewProvider : PreviewParameterProvider<RatesListUiState> {

    override val values: Sequence<RatesListUiState> = sequenceOf(
        RatesListUiState(
            subtitle = "1 USD · 2026-11-02",
            isRefreshEnabled = true,
            content = Content.Items(
                listOf(
                    RateItem(code = "EUR", name = "Euro", rate = "0.8916", change = "-0.39%", trend = Trend.Down),
                    RateItem(
                        code = "JPY",
                        name = "Japanese Yen",
                        rate = "157.9000",
                        change = "+0.41%",
                        trend = Trend.Up,
                    ),
                    RateItem(code = "CHF", name = "Swiss Franc", rate = "0.8012", change = null, trend = Trend.Flat),
                ),
            ),
        ),
        RatesListUiState(content = Content.Loading),
        RatesListUiState(isRefreshEnabled = true, content = Content.Error),
        RatesListUiState(query = "XYZ", isRefreshEnabled = true, content = Content.NothingFound),
    )

}
