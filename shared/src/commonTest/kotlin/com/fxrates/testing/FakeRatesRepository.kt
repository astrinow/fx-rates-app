package com.fxrates.testing

import com.fxrates.domain.model.CurrencyRate
import com.fxrates.domain.model.CurrencyRateHistory
import com.fxrates.domain.repository.RatesRepository

class FakeRatesRepository : RatesRepository {

    var latestRates: Result<List<CurrencyRate>> = Result.success(emptyList())

    var rateHistory: Result<CurrencyRateHistory> = Result.failure(IllegalStateException("History is not configured"))

    val requestedBases = mutableListOf<String>()

    val requestedPairs = mutableListOf<Pair<String, String>>()

    override suspend fun getLatestRates(base: String): List<CurrencyRate> {
        requestedBases += base
        return latestRates.getOrThrow()
    }

    override suspend fun getRateHistory(base: String, code: String): CurrencyRateHistory {
        requestedPairs += base to code
        return rateHistory.getOrThrow()
    }

}
