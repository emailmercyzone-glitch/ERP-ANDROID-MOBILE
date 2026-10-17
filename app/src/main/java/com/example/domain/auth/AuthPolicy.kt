package com.example.domain.auth

import com.example.domain.model.Role
import java.security.MessageDigest

/**
 * Authentication & Role Enforcement Policy as specified in
 * ROLES_PERMISSIONS.md, D-020, D-021, and SESI-PIN.
 * Enforces 6-digit PIN, 3x failed lockout for 5 minutes, 120s auto-lock, and Mode Jaga delegation.
 */
object AuthPolicy {

    const val PIN_LENGTH = 6
    const val MAX_FAILED_ATTEMPTS = 3
    const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    const val AUTO_LOCK_IDLE_TIMEOUT_MS = 120 * 1000L // 120 seconds
    const val DEFAULT_MODE_JAGA_LIMIT = 2000000L // Rp 2.000.000

    fun hashPin(pin: String): String {
        require(pin.length == PIN_LENGTH && pin.all { it.isDigit() }) {
            "PIN harus tepat $PIN_LENGTH digit angka!"
        }
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String, storedHash: String): Boolean {
        if (enteredPin.length != PIN_LENGTH) return false
        val enteredHash = hashPin(enteredPin)
        return enteredHash == storedHash
    }

    fun isActionAllowed(
        role: Role,
        action: String,
        amount: Long = 0L,
        isModeJagaActive: Boolean = false,
        modeJagaLimit: Long = DEFAULT_MODE_JAGA_LIMIT
    ): Boolean {
        if (role == Role.OWNER) return true

        return when (action) {
            "POS_SALE", "VIEW_STOCK", "CUSTOMER_RECEIVABLE", "REPRINT_RECEIPT", "CYCLE_COUNT_INPUT" -> true
            "SUPPLIER_PAYMENT", "GRN_ACCEPTANCE" -> {
                isModeJagaActive && amount <= modeJagaLimit
            }
            "JOURNAL_ENTRY", "PRIVE", "SETTINGS", "PROFIT_LOSS_VIEW", "TAX_CONFIG" -> false // Hard-Locked!
            else -> false
        }
    }
}
