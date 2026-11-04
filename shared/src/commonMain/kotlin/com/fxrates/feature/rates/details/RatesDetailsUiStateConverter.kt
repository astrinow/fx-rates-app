package com.fxrates.feature.rates.details

import com.fxrates.core.format.NumberFormatter
import com.fxrates.core.money.RateChangeCalculator
import com.fxrates.core.utils.Empty
import com.fxrates.core.utils.Resource
import com.fxrates.domain.model.CurrencyRateHistory
import com.fxrates.feature.rates.details.RatesDetailsUiState.Content
import com.fxrates.feature.rates.details.model.RateHistoryItem
import com.fxrates.feature.rates.details.model.RateHistoryTrend
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore
import com.fxrates.ui.Strings
import me.tatarka.inject.annotations.Inject

@Inject
internal class RatesDetailsUiStateConverter {

    fun createUiState(state: RatesDetailsStore.State): RatesDetailsUiState {
        val history = state.history
        val subtitle = when (history) {
            is Resource.Data -> {
                Strings.rateHistorySubtitle(
                    base = state.base,
                    startDate = history.value.startDate.toString(),
                    endDate = history.value.endDate.toString(),
                )
            }
            is Resource.Loading, is Resource.Error -> {
                String.Empty
            }
        }
        return RatesDetailsUiState(
            title = Strings.currencyPair(base = state.base, code = state.code),
            subtitle = subtitle,
            isRetryEnabled = history !is Resource.Loading,
            content = createContent(history),
        )
    }

    private fun createContent(history: Resource<CurrencyRateHistory>): Content = when (history) {
        is Resource.Loading -> {
            Content.Loading
        }
        is Resource.Error -> {
            Content.Error
        }
        is Resource.Data -> {
            toContent(history.value)
        }
    }

    private fun toContent(history: CurrencyRateHistory): Content {
        val points = history.points
        if (points.isEmpty()) return Content.Empty
        val change = if (points.size > 1) {
            RateChangeCalculator.calculate(previous = points.first().rate, current = points.last().rate)
        } else {
            null
        }
        val trend = when {
            change == null || change.absolute.isZero() -> RateHistoryTrend.Flat
            change.absolute.isPositive -> RateHistoryTrend.Up
            else -> RateHistoryTrend.Down
        }
        return Content.Items(
            items = points.asReversed().map { point ->
                RateHistoryItem(date = point.date.toString(), rate = NumberFormatter.formatRate(point.rate))
            },
            currentRate = NumberFormatter.formatRate(points.last().rate),
            currentDate = points.last().date.toString(),
            absoluteChange = change?.absolute?.let(NumberFormatter::formatSignedRate) ?: Strings.UNAVAILABLE_VALUE,
            percentChange = change?.percent?.let(NumberFormatter::formatSignedPercent) ?: Strings.UNAVAILABLE_VALUE,
            trend = trend,
        )
    }

}
