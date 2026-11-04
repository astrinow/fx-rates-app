package com.fxrates.core.format

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.RoundingMode

/** Turns numbers into UI strings. Every number shown on screen goes through this object. */
object NumberFormatter {

    private const val RATE_DECIMALS = 4L
    private const val PERCENT_DECIMALS = 2L

    /** `157.9` → `"157.9000"`, `0.891583` → `"0.8916"`. */
    fun formatRate(rate: BigDecimal): String = rate.round(RATE_DECIMALS).toFixed(RATE_DECIMALS)

    /** `0.0123` → `"+0.0123"`, `-1.5` → `"-1.5000"`; a change that rounds to zero has no sign. */
    fun formatSignedRate(change: BigDecimal): String = change.round(RATE_DECIMALS).toSigned(RATE_DECIMALS)

    /** `1.234` → `"+1.23%"`, `-0.5` → `"-0.50%"`; a change that rounds to zero has no sign. */
    fun formatSignedPercent(percent: Double): String {
        val rounded = BigDecimal.fromDouble(percent).round(PERCENT_DECIMALS)
        return rounded.toSigned(PERCENT_DECIMALS) + "%"
    }

    private fun BigDecimal.round(decimals: Long): BigDecimal {
        return roundToDigitPositionAfterDecimalPoint(decimals, RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)
    }

    private fun BigDecimal.toSigned(decimals: Long): String {
        val sign = if (isPositive) "+" else ""
        return sign + toFixed(decimals)
    }

    private fun BigDecimal.toFixed(decimals: Long): String {
        val sign = if (isNegative) "-" else ""
        val digits = abs().toStringExpanded()
        val integer = digits.substringBefore('.')
        val fraction = digits.substringAfter('.', missingDelimiterValue = "").padEnd(decimals.toInt(), '0')
        return "$sign$integer.$fraction"
    }

}
