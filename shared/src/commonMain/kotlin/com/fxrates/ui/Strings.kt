package com.fxrates.ui

/**
 * Every user-facing string of the app. The app is English-only, so a constant is enough; a
 * string is never written inline in a composable.
 */
object Strings {

    const val RATES_TITLE = "Rates"
    const val RATES_SEARCH_HINT = "Search by code or name"
    const val RATES_NOTHING_FOUND = "Nothing found"
    const val ERROR_TITLE = "Could not load the rates"
    const val RETRY = "Retry"
    const val REFRESH = "Refresh"
    const val UNAVAILABLE_VALUE = "—"
    const val BACK = "Back"
    const val CURRENT_RATE = "Latest available rate"
    const val ABSOLUTE_CHANGE = "Absolute change"
    const val PERCENT_CHANGE = "Percent change"
    const val RATE_HISTORY = "Rate history"
    const val HISTORY_DATE = "Date"
    const val HISTORY_RATE = "Rate"
    const val HISTORY_EMPTY = "No rates available for this period"
    const val HISTORY_ERROR = "Could not load the rate history"
    const val HISTORY_LOADING = "Loading rate history"

    fun ratesSubtitle(base: String, date: String): String = "1 $base · $date"

    fun currencyPair(base: String, code: String): String = "$base / $code"

    fun rateHistorySubtitle(base: String, startDate: String, endDate: String): String {
        return "1 $base · $startDate — $endDate"
    }

}
