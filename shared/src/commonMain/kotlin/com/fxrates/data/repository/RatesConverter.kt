package com.fxrates.data.repository

import com.fxrates.data.remote.dto.TimeSeriesDto
import com.fxrates.domain.model.CurrencyRate
import com.fxrates.domain.model.CurrencyRateHistory
import com.fxrates.domain.model.RateHistoryPoint
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import me.tatarka.inject.annotations.Inject

@Inject
class RatesConverter {

    /**
     * Takes the last two dates of [timeSeries] as "latest" and "previous". A currency with no
     * name in [currencies] is still shown, under its code.
     */
    fun toLatestRates(timeSeries: TimeSeriesDto, currencies: Map<String, String>): List<CurrencyRate> {
        val datesDescending = timeSeries.rates.keys.sortedDescending()
        val latestDate = datesDescending.firstOrNull() ?: return emptyList()
        val latest = timeSeries.rates.getValue(latestDate)
        val previous = datesDescending.getOrNull(1)?.let(timeSeries.rates::getValue).orEmpty()
        return latest
            .map { (code, rate) ->
                CurrencyRate(
                    code = code,
                    name = currencies[code] ?: code,
                    date = LocalDate.parse(latestDate),
                    rate = rate.toDecimal(),
                    previousRate = previous[code]?.toDecimal(),
                )
            }
            .sortedBy(CurrencyRate::code)
    }

    fun toRateHistory(
        timeSeries: TimeSeriesDto,
        base: String,
        code: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): CurrencyRateHistory {
        val points = timeSeries.rates.mapNotNull { (date, rates) ->
            val rate = rates[code] ?: return@mapNotNull null
            val quoteDate = LocalDate.parse(date)
            if (quoteDate !in startDate..endDate) return@mapNotNull null
            RateHistoryPoint(date = quoteDate, rate = rate.toDecimal())
        }.sortedBy(RateHistoryPoint::date)
        return CurrencyRateHistory(
            base = base,
            code = code,
            startDate = startDate,
            endDate = endDate,
            points = points,
        )
    }

    private fun JsonPrimitive.toDecimal(): BigDecimal {
        require(!isString && this != JsonNull) { "Expected a numeric rate" }
        return content.toBigDecimal()
    }

}
