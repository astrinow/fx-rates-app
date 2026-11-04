package com.fxrates.feature.rates.details

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.fxrates.feature.rates.details.RatesDetailsUiState.Content
import com.fxrates.feature.rates.details.model.RateHistoryItem
import com.fxrates.feature.rates.details.model.RateHistoryTrend
import com.fxrates.ui.Strings
import com.fxrates.ui.theme.AppTheme

@Composable
fun RatesDetailsUiContent(
    component: RatesDetailsUiComponent,
    modifier: Modifier = Modifier,
) {
    val state by component.state.collectAsState()
    RatesDetailsUiContent(
        state = state,
        onRetry = component::onRetry,
        onBack = component::onBack,
        modifier = modifier,
    )
}

@Composable
private fun RatesDetailsUiContent(
    state: RatesDetailsUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.fillMaxSize().safeDrawingPadding(),
) {
    Header(title = state.title, subtitle = state.subtitle, onBack = onBack)
    when (val content = state.content) {
        is Content.Loading -> {
            Centered {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = Strings.HISTORY_LOADING },
                )
            }
        }
        is Content.Error -> {
            Centered {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
                ) {
                    Text(Strings.HISTORY_ERROR, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onRetry, enabled = state.isRetryEnabled) {
                        Text(Strings.RETRY)
                    }
                }
            }
        }
        is Content.Empty -> {
            Centered { Text(Strings.HISTORY_EMPTY, style = MaterialTheme.typography.bodyLarge) }
        }
        is Content.Items -> {
            History(content)
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, onBack: () -> Unit) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.s, vertical = AppTheme.spacing.s),
    verticalAlignment = Alignment.CenterVertically,
) {
    TextButton(onClick = onBack) {
        Text(Strings.BACK)
    }
    Column(modifier = Modifier.weight(1f).padding(horizontal = AppTheme.spacing.s)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.neutral)
        }
    }
}

@Composable
private fun History(content: Content.Items) = LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
        Summary(content)
        Text(
            text = Strings.RATE_HISTORY,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
        ) {
            Text(Strings.HISTORY_DATE, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Text(Strings.HISTORY_RATE, style = MaterialTheme.typography.labelLarge)
        }
        HorizontalDivider()
    }
    items(items = content.items, key = RateHistoryItem::date) { item ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
        ) {
            Text(item.date, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(item.rate, style = MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider()
    }
}

@Composable
private fun Summary(content: Content.Items) = Card(
    modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.m),
) {
    Column(
        modifier = Modifier.padding(AppTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
    ) {
        Text(Strings.CURRENT_RATE, style = MaterialTheme.typography.labelLarge)
        Text(content.currentRate, style = MaterialTheme.typography.headlineLarge)
        Text(content.currentDate, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.neutral)
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.m)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(Strings.ABSOLUTE_CHANGE, style = MaterialTheme.typography.labelLarge)
                Text(content.absoluteChange, style = MaterialTheme.typography.titleLarge, color = content.trend.color())
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(Strings.PERCENT_CHANGE, style = MaterialTheme.typography.labelLarge)
                Text(content.percentChange, style = MaterialTheme.typography.titleLarge, color = content.trend.color())
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) = Box(
    modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.m),
    contentAlignment = Alignment.Center,
) {
    content.invoke()
}

@Composable
private fun RateHistoryTrend.color() = when (this) {
    RateHistoryTrend.Up -> AppTheme.colors.positive
    RateHistoryTrend.Down -> AppTheme.colors.negative
    RateHistoryTrend.Flat -> AppTheme.colors.neutral
}

@Preview
@Composable
private fun RatesDetailsUiContentPreview(
    @PreviewParameter(RatesDetailsUiStatePreviewProvider::class) state: RatesDetailsUiState,
) = AppTheme {
    RatesDetailsUiContent(state = state, onRetry = {}, onBack = {})
}
