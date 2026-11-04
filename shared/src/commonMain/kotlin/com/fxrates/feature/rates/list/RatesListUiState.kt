package com.fxrates.feature.rates.list

import androidx.compose.runtime.Immutable
import com.fxrates.core.utils.Empty
import com.fxrates.feature.rates.list.model.RateItem

@Immutable
internal data class RatesListUiState(
    val subtitle: String = String.Empty,
    val query: String = String.Empty,
    val isRefreshEnabled: Boolean = false,
    val content: Content = Content.Loading,
) {

    @Immutable
    sealed class Content {

        data object Loading : Content()

        data object Error : Content()

        data object NothingFound : Content()

        data class Items(val items: List<RateItem>) : Content()

    }

}
