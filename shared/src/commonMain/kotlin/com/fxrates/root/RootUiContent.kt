package com.fxrates.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.fxrates.feature.rates.list.RatesListUiContent
import com.fxrates.ui.theme.AppTheme

@Composable
fun RootUiContent(
    component: RootUiComponent,
    modifier: Modifier = Modifier,
) = AppTheme {
    Children(
        stack = component.stack,
        modifier = modifier,
        animation = stackAnimation(slide()),
    ) { child ->
        when (val instance = child.instance) {
            is RootUiComponent.Child.RatesList -> RatesListUiContent(component = instance.component)
        }
    }
}
