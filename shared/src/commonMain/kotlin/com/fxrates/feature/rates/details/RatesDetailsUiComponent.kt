package com.fxrates.feature.rates.details

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.fxrates.feature.rates.details.mvi.RatesDetailsStore
import com.fxrates.feature.rates.details.mvi.RatesDetailsStoreFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.AssistedFactory
import me.tatarka.inject.annotations.Inject

abstract class RatesDetailsUiComponent {

    internal abstract val state: StateFlow<RatesDetailsUiState>

    internal abstract fun onRetry()

    internal abstract fun onBack()

}

@Inject
internal class RatesDetailsUiComponentDefault(
    @Assisted componentContext: ComponentContext,
    @Assisted base: String,
    @Assisted code: String,
    @Assisted private val callbacks: Callbacks,
    storeFactory: RatesDetailsStoreFactory,
    uiStateConverter: RatesDetailsUiStateConverter,
    computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RatesDetailsUiComponent(), ComponentContext by componentContext {

    @AssistedFactory
    interface Factory {

        fun create(
            componentContext: ComponentContext,
            base: String,
            code: String,
            callbacks: Callbacks,
        ): RatesDetailsUiComponentDefault

    }

    interface Callbacks {

        fun onBack()

    }

    private val scope = coroutineScope()

    private val store = storeFactory.createStore(base = base, code = code)

    override val state: StateFlow<RatesDetailsUiState> = store.states
        .map(uiStateConverter::createUiState)
        .flowOn(computationDispatcher)
        .stateIn(scope, SharingStarted.Eagerly, RatesDetailsUiState())

    init {
        lifecycle.doOnDestroy(store::dispose)
        store.labels
            .onEach(::handleLabel)
            .launchIn(scope)
    }

    private fun handleLabel(label: RatesDetailsStore.Label) {
        when (label) {
            is RatesDetailsStore.Label.Back -> callbacks.onBack()
        }
    }

    override fun onRetry() {
        store.accept(RatesDetailsStore.Intent.OnRetry)
    }

    override fun onBack() {
        store.accept(RatesDetailsStore.Intent.OnBack)
    }

}
