package com.fxrates.core.money

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode

/**
 * The change between two rates of the same currency pair.
 *
 * @property absolute `current - previous`, in units of the quote currency.
 * @property percent the change relative to the previous rate; `1.5` means +1.5%.
 * `null` when the previous rate is zero.
 */
data class RateChange(
    val absolute: BigDecimal,
    val percent: BigDecimal?,
)

object RateChangeCalculator {

    private val HUNDRED = BigDecimal.fromInt(100)
    private val PERCENT_MODE = DecimalMode(
        decimalPrecision = 34,
        roundingMode = RoundingMode.ROUND_HALF_AWAY_FROM_ZERO,
    )

    /** Division uses 34 significant decimal digits; the absolute change remains exact. */
    fun calculate(previous: BigDecimal, current: BigDecimal): RateChange {
        val absolute = current.subtract(previous, DecimalMode.DEFAULT)
        val percent = if (previous.isZero()) {
            null
        } else {
            absolute.multiply(HUNDRED, DecimalMode.DEFAULT).divide(previous, PERCENT_MODE)
        }
        return RateChange(absolute = absolute, percent = percent)
    }

}
