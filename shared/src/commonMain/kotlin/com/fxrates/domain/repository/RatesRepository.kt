package com.fxrates.domain.repository

import com.fxrates.domain.model.CurrencyRate

interface RatesRepository {

    /** The latest rates of every supported currency against [base], sorted by code. */
    suspend fun getLatestRates(base: String): List<CurrencyRate>

}
