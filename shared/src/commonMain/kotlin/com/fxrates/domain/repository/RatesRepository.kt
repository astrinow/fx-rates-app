package com.fxrates.domain.repository

import com.fxrates.domain.model.CurrencyRate
import com.fxrates.domain.model.CurrencyRateHistory

interface RatesRepository {

    /** The latest rates of every supported currency against [base], sorted by code. */
    suspend fun getLatestRates(base: String): List<CurrencyRate>

    /**
     * Available quotes of [code] against [base] in the 30 calendar days ending today in UTC.
     * Both boundaries are inclusive. Failures are thrown; an empty history is valid.
     */
    suspend fun getRateHistory(base: String, code: String): CurrencyRateHistory

}
