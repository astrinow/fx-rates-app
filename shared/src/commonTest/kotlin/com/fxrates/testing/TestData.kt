package com.fxrates.testing

import com.fxrates.domain.model.CurrencyRate
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.datetime.LocalDate

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
