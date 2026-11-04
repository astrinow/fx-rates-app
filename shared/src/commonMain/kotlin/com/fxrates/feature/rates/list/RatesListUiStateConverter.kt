package com.fxrates.feature.rates.list

import com.fxrates.core.format.NumberFormatter
import com.fxrates.core.money.RateChangeCalculator
import com.fxrates.core.utils.Empty
import com.fxrates.core.utils.Resource
import com.fxrates.domain.model.CurrencyRate
import com.fxrates.feature.rates.list.RatesListUiState.Content
import com.fxrates.feature.rates.list.model.RateItem
import com.fxrates.feature.rates.list.model.Trend
import com.fxrates.feature.rates.list.mvi.RatesListStore
import com.fxrates.ui.Strings
import me.tatarka.inject.annotations.Inject

@Inject
internal class RatesListUiStateConverter {

    fun createUiState(state: RatesListStore.State): RatesListUiState {
        val rates = state.rates
        val subtitle = when (rates) {
            is Resource.Data -> {
                rates.value.firstOrNull()?.let { rate -> Strings.ratesSubtitle(state.base, rate.date.toString()) }
                    ?: String.Empty
            }
            is Resource.Loading, is Resource.Error -> {
                String.Empty
            }
        }
        return RatesListUiState(
            subtitle = subtitle,
            query = state.query,
            isRefreshEnabled = rates !is Resource.Loading,
            content = createContent(rates = rates, query = state.query),
        )
    }

    private fun createContent(rates: Resource<List<CurrencyRate>>, query: String): Content = when (rates) {
        is Resource.Loading -> {
            Content.Loading
        }
        is Resource.Error -> {
            Content.Error
        }
        is Resource.Data -> {
            val items = rates.value
                .filter { rate -> rate.matches(query.trim()) }
                .map(::toItem)
            if (items.isEmpty()) Content.NothingFound else Content.Items(items)
        }
    }

    private fun CurrencyRate.matches(query: String): Boolean {
        return query.isEmpty() || code.contains(query, ignoreCase = true) || name.contains(query, ignoreCase = true)
    }

    private fun toItem(rate: CurrencyRate): RateItem {
        val change = rate.previousRate?.let { previous -> RateChangeCalculator.calculate(previous, rate.rate) }
        val trend = when {
            change == null || change.absolute.isZero() -> Trend.Flat
            change.absolute.isPositive -> Trend.Up
            else -> Trend.Down
        }
        return RateItem(
            code = rate.code,
            name = rate.name,
            rate = NumberFormatter.formatRate(rate.rate),
            change = change?.let { value -> NumberFormatter.formatSignedPercent(value.percent) },
            trend = trend,
        )
    }

}
