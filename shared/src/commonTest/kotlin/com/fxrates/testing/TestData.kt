package com.fxrates.testing

import com.fxrates.data.remote.dto.TimeSeriesDto
import com.fxrates.domain.model.CurrencyRate
import com.fxrates.domain.model.CurrencyRateHistory
import com.fxrates.domain.model.RateHistoryPoint
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

fun rateHistory(points: List<RateHistoryPoint> = emptyList()): CurrencyRateHistory {
    return CurrencyRateHistory(
        base = "USD",
        code = "EUR",
        startDate = LocalDate(2026, 10, 6),
        endDate = LocalDate(2026, 11, 4),
        points = points,
    )
}

fun currencyRate(
    code: String = "EUR",
    name: String = "Euro",
    rate: String = "0.9",
    previousRate: String? = "0.9",
    date: LocalDate = LocalDate(2026, 11, 2),
): CurrencyRate = CurrencyRate(
    code = code,
    name = name,
    date = date,
    rate = rate.toBigDecimal(),
    previousRate = previousRate?.toBigDecimal(),
)

fun timeSeries(
    rates: Map<String, Map<String, String>>,
    base: String = "USD",
    startDate: String = "2026-10-06",
    endDate: String = "2026-11-04",
): TimeSeriesDto {
    return TimeSeriesDto(
        base = base,
        startDate = startDate,
        endDate = endDate,
        rates = rates.mapValues { (_, currencies) ->
            currencies.mapValues { (_, rate) -> Json.parseToJsonElement(rate).jsonPrimitive }
        },
    )
}
