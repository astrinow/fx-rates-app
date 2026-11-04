package com.fxrates.feature.rates.list

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.fxrates.feature.rates.list.mvi.RatesListStore
import com.fxrates.feature.rates.list.mvi.RatesListStoreFactory
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

abstract class RatesListUiComponent {

    internal abstract val state: StateFlow<RatesListUiState>

    internal abstract fun onRefresh()

    internal abstract fun onQueryChange(query: String)

    internal abstract fun onCurrencyClick(code: String)

}

@Inject
internal class RatesListUiComponentDefault(
    @Assisted componentContext: ComponentContext,
    @Assisted base: String,
    @Assisted private val callbacks: Callbacks,
    storeFactory: RatesListStoreFactory,
    uiStateConverter: RatesListUiStateConverter,
    computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RatesListUiComponent(), ComponentContext by componentContext {

    @AssistedFactory
    interface Factory {

        fun create(
            componentContext: ComponentContext,
            base: String,
            callbacks: Callbacks,
        ): RatesListUiComponentDefault

    }

    interface Callbacks {

        fun onOpenCurrency(base: String, code: String)

    }

    private val scope = coroutineScope()

    private val store = storeFactory.createStore(base = base)

    override val state: StateFlow<RatesListUiState> = store.states
        .map(uiStateConverter::createUiState)
        .flowOn(computationDispatcher)
        .stateIn(scope, SharingStarted.Eagerly, RatesListUiState())

    init {
        store.labels
            .onEach(::handleLabel)
            .launchIn(scope)
    }

    private fun handleLabel(label: RatesListStore.Label) {
        when (label) {
            is RatesListStore.Label.OpenCurrency -> callbacks.onOpenCurrency(base = label.base, code = label.code)
        }
    }

    override fun onRefresh() {
        store.accept(RatesListStore.Intent.OnRefresh)
    }

    override fun onQueryChange(query: String) {
        store.accept(RatesListStore.Intent.OnQueryChange(query))
    }

    override fun onCurrencyClick(code: String) {
        store.accept(RatesListStore.Intent.OnCurrencyClick(code))
    }

}
