package com.fxrates.domain.model

import kotlinx.datetime.LocalDate

/**
 * Available quotes within the inclusive requested window, sorted by ascending date.
 * An empty [points] list is a successful result; missing dates are not filled in.
 */
data class CurrencyRateHistory(
    val base: String,
    val code: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val points: List<RateHistoryPoint>,
)
