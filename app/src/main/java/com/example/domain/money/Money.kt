package com.example.domain.money

import java.text.NumberFormat
import java.util.Locale

/**
 * Strict exact integer arithmetic for Indonesian Rupiah as mandated by PRIN-MONEY-01 & PRIN-MONEY-02.
 * All monetary amounts MUST be Long (exact full Rupiah, never Float or Double).
 * Operations fail-hard on overflow.
 */
object Money {

    fun add(a: Long, b: Long): Long {
        return Math.addExact(a, b)
    }

    fun subtract(a: Long, b: Long): Long {
        return Math.subtractExact(a, b)
    }

    fun multiply(a: Long, b: Long): Long {
        return Math.multiplyExact(a, b)
    }

    /**
     * Percentage calculation using basis points (1 bps = 0.01% = 0.0001).
     * e.g., 30 bps = 0.3% MDR.
     */
    fun calculateBps(amount: Long, bps: Long): Long {
        val multiplied = Math.multiplyExact(amount, bps)
        return multiplied / 10000L
    }

    /**
     * Percentage calculation where percent is integer 0-100.
     */
    fun calculatePercentage(amount: Long, percent: Int): Long {
        require(percent in 0..100) { "Percent must be between 0 and 100, got: $percent" }
        val multiplied = Math.multiplyExact(amount, percent.toLong())
        return multiplied / 100L
    }

    private val rupiahFormat by lazy {
        val localeId = Locale("in", "ID")
        NumberFormat.getNumberInstance(localeId).apply {
            maximumFractionDigits = 0
            isGroupingUsed = true
        }
    }

    fun formatRupiah(amount: Long): String {
        return "Rp " + rupiahFormat.format(amount)
    }
}
