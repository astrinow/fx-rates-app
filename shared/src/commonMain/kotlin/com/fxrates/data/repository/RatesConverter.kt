package com.fxrates.data.repository

import com.fxrates.data.remote.dto.TimeSeriesDto
import com.fxrates.domain.model.CurrencyRate
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.datetime.LocalDate
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

    // Going through the string keeps the decimal digits the API sent: 0.89158 stays 0.89158
    // instead of the nearest binary double.
    private fun Double.toDecimal(): BigDecimal = toString().toBigDecimal()

}
