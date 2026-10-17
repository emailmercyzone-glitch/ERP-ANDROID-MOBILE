package com.example.domain.cash

/**
 * Implements cash payment rounding to nearest Rp 500 (half-up rounding)
 * as specified in CSH-R05, SAL-R08, and D-013.
 * The difference is booked to the cash rounding difference journal account (5-1510).
 */
object CashRounding {

    /**
     * Rounds [amount] to the nearest multiple of 500 using half-up convention.
     * @return Pair of (roundedAmount, diff) where diff = roundedAmount - amount.
     */
    fun round500(amount: Long): Pair<Long, Long> {
        if (amount <= 0L) return Pair(0L, 0L)
        val remainder = amount % 500L
        val roundedAmount = if (remainder >= 250L) {
            amount + (500L - remainder)
        } else {
            amount - remainder
        }
        val diff = roundedAmount - amount
        return Pair(roundedAmount, diff)
    }
}
