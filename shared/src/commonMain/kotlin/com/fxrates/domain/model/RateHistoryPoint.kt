package com.fxrates.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate

/** One available quote: units of the quote currency bought by one unit of the base currency. */
data class RateHistoryPoint(
    val date: LocalDate,
    val rate: BigDecimal,
)
