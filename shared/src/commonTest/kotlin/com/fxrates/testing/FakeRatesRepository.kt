package com.fxrates.testing

import com.fxrates.domain.model.CurrencyRate
import com.fxrates.domain.repository.RatesRepository

class FakeRatesRepository : RatesRepository {

    var latestRates: Result<List<CurrencyRate>> = Result.success(emptyList())

    val requestedBases = mutableListOf<String>()

    override suspend fun getLatestRates(base: String): List<CurrencyRate> {
        requestedBases += base
        return latestRates.getOrThrow()
    }

}
