package com.fxrates.feature.rates.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.fxrates.feature.rates.list.RatesListUiState.Content
import com.fxrates.feature.rates.list.model.RateItem
import com.fxrates.feature.rates.list.model.Trend
import com.fxrates.ui.Strings
import com.fxrates.ui.theme.AppTheme

@Composable
fun RatesListUiContent(
    component: RatesListUiComponent,
    modifier: Modifier = Modifier,
) {
    val state by component.state.collectAsState()
    RatesListUiContent(
        state = state,
        onRefresh = component::onRefresh,
        onQueryChange = component::onQueryChange,
        onCurrencyClick = component::onCurrencyClick,
        modifier = modifier,
    )
}

@Composable
private fun RatesListUiContent(
    state: RatesListUiState,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCurrencyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .fillMaxSize()
        .safeDrawingPadding(),
) {
    Header(
        subtitle = state.subtitle,
        isRefreshEnabled = state.isRefreshEnabled,
        onRefresh = onRefresh,
    )
    OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChange,
        placeholder = { Text(Strings.RATES_SEARCH_HINT) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.m),
    )
    when (val content = state.content) {
        is Content.Loading -> {
            Centered { CircularProgressIndicator() }
        }
        is Content.Error -> {
            ErrorState(onRetry = onRefresh)
        }
        is Content.NothingFound -> {
            Centered { Text(Strings.RATES_NOTHING_FOUND, style = MaterialTheme.typography.bodyLarge) }
        }
        is Content.Items -> {
            RateList(items = content.items, onCurrencyClick = onCurrencyClick)
        }
    }
}

@Composable
private fun Header(
    subtitle: String,
    isRefreshEnabled: Boolean,
    onRefresh: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(Strings.RATES_TITLE, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.neutral)
    }
    TextButton(onClick = onRefresh, enabled = isRefreshEnabled) {
        Text(Strings.REFRESH)
    }
}

@Composable
private fun RateList(
    items: List<RateItem>,
    onCurrencyClick: (String) -> Unit,
) = LazyColumn(modifier = Modifier.fillMaxSize()) {
    items(items = items, key = RateItem::code) { item ->
        RateRow(item = item, onClick = { onCurrencyClick.invoke(item.code) })
        HorizontalDivider()
    }
}

@Composable
private fun RateRow(
    item: RateItem,
    onClick: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(item.code, style = MaterialTheme.typography.titleMedium)
        Text(item.name, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.neutral)
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(item.rate, style = MaterialTheme.typography.titleMedium)
        item.change?.let { change ->
            Text(change, style = MaterialTheme.typography.bodySmall, color = item.trend.color())
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) = Centered {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
    ) {
        Text(Strings.ERROR_TITLE, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) {
            Text(Strings.RETRY)
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
) {
    content.invoke()
}

@Composable
private fun Trend.color() = when (this) {
    Trend.Up -> AppTheme.colors.positive
    Trend.Down -> AppTheme.colors.negative
    Trend.Flat -> AppTheme.colors.neutral
}

@Preview
@Composable
private fun RatesListUiContentPreview(
    @PreviewParameter(RatesListUiStatePreviewProvider::class) state: RatesListUiState,
) = AppTheme {
    RatesListUiContent(
        state = state,
        onRefresh = {},
        onQueryChange = {},
        onCurrencyClick = {},
    )
}
