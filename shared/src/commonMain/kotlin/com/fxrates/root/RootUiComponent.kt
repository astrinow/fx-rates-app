package com.fxrates.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.fxrates.feature.rates.details.RatesDetailsUiComponent
import com.fxrates.feature.rates.details.RatesDetailsUiComponentDefault
import com.fxrates.feature.rates.list.RatesListUiComponent
import com.fxrates.feature.rates.list.RatesListUiComponentDefault
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.AssistedFactory
import me.tatarka.inject.annotations.Inject

abstract class RootUiComponent {

    internal abstract val stack: Value<ChildStack<*, Child>>

    internal sealed class Child {

        class RatesList(val component: RatesListUiComponent) : Child()

        class RatesDetails(val component: RatesDetailsUiComponent) : Child()

    }

}

@Inject
internal class RootUiComponentDefault(
    @Assisted componentContext: ComponentContext,
    private val ratesListFactory: RatesListUiComponentDefault.Factory,
    private val ratesDetailsFactory: RatesDetailsUiComponentDefault.Factory,
) : RootUiComponent(), ComponentContext by componentContext {

    @AssistedFactory
    interface Factory {

        fun create(componentContext: ComponentContext): RootUiComponentDefault

    }

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.RatesList,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Child = when (config) {
        is Config.RatesList -> {
            Child.RatesList(
                ratesListFactory.create(
                    componentContext = componentContext,
                    base = DEFAULT_BASE_CURRENCY,
                    callbacks = RatesListCallbacks(),
                ),
            )
        }
        is Config.RatesDetails -> {
            Child.RatesDetails(
                ratesDetailsFactory.create(
                    componentContext = componentContext,
                    base = config.base,
                    code = config.code,
                    callbacks = RatesDetailsCallbacks(),
                ),
            )
        }
    }

    private inner class RatesListCallbacks : RatesListUiComponentDefault.Callbacks {

        override fun onOpenCurrency(base: String, code: String) {
            navigation.pushNew(Config.RatesDetails(base = base, code = code))
        }

    }

    private inner class RatesDetailsCallbacks : RatesDetailsUiComponentDefault.Callbacks {

        override fun onBack() {
            navigation.pop()
        }

    }

    @Serializable
    sealed class Config {

        @Serializable
        data object RatesList : Config()

        @Serializable
        data class RatesDetails(val base: String, val code: String) : Config()

    }

    private companion object {

        const val DEFAULT_BASE_CURRENCY = "USD"

    }

}
