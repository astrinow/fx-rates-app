package com.fxrates.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate

/**
 * The latest rate of [code] against the base currency.
 *
 * @property rate how many units of [code] one unit of the base currency buys.
 * @property previousRate the rate on the previous business day; `null` when the API has no
 * earlier quote in the requested window.
 */
data class CurrencyRate(
    val code: String,
    val name: String,
    val date: LocalDate,
    val rate: BigDecimal,
    val previousRate: BigDecimal?,
)
