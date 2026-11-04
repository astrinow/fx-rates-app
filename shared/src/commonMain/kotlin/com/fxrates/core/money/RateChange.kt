package com.fxrates.core.money

import com.ionspin.kotlin.bignum.decimal.BigDecimal

/**
 * The change between two rates of the same currency pair.
 *
 * @property absolute `current - previous`, in units of the quote currency.
 * @property percent the change relative to the previous rate; `1.5` means +1.5%.
 */
data class RateChange(
    val absolute: BigDecimal,
    val percent: Double,
)

object RateChangeCalculator {

    /** Returns `null` when [previous] is zero and the relative change is undefined. */
    fun calculate(previous: BigDecimal, current: BigDecimal): RateChange? {
        if (previous.isZero()) return null
        val absolute = current - previous
        val percent = absolute.doubleValue(exactRequired = false) / current.doubleValue(exactRequired = false) * 100
        return RateChange(absolute = absolute, percent = percent)
    }

}
