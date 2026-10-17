package com.example.domain.config

/**
 * Business configuration keys and validation contracts as defined in CONFIGURATION_RULES.md.
 * Allows Store Owner to configure business parameters (MDR rate, tax status, limits)
 * without recompiling or altering historical transaction snapshots.
 */
object BusinessConfig {

    const val KEY_STORE_NAME = "STORE_NAME"
    const val KEY_STORE_ADDRESS = "STORE_ADDRESS"
    const val KEY_STORE_PHONE = "STORE_PHONE"

    const val KEY_QRIS_MDR_BPS = "QRIS_MDR_BPS" // Basis points, default 30 (0.3%)
    const val KEY_PPH_FINAL_ACTIVE = "PPH_FINAL_ACTIVE" // Default "false" (D-030 gated)
    const val KEY_PPH_FINAL_RATE_BPS = "PPH_FINAL_RATE_BPS" // Default 50 (0.5%)

    const val KEY_DEFAULT_CREDIT_LIMIT = "DEFAULT_CREDIT_LIMIT" // Rp 500.000
    const val KEY_DEFAULT_CREDIT_TERM_DAYS = "DEFAULT_CREDIT_TERM_DAYS" // 14 days
    const val KEY_MODE_JAGA_LIMIT = "MODE_JAGA_LIMIT" // Rp 2.000.000

    val DEFAULTS = mapOf(
        KEY_STORE_NAME to "Omah Sembako",
        KEY_STORE_ADDRESS to "Jl. Raya Pasar Sembako No. 7",
        KEY_STORE_PHONE to "0812-3456-7890",
        KEY_QRIS_MDR_BPS to "30",
        KEY_PPH_FINAL_ACTIVE to "false",
        KEY_PPH_FINAL_RATE_BPS to "50",
        KEY_DEFAULT_CREDIT_LIMIT to "500000",
        KEY_DEFAULT_CREDIT_TERM_DAYS to "14",
        KEY_MODE_JAGA_LIMIT to "2000000"
    )

    data class ConfigRevision(
        val configKey: String,
        val revision: Long,
        val valueCanonical: String,
        val effectiveFrom: Long,
        val effectiveTo: Long? = null,
        val status: String = "ACTIVE",
        val updatedBy: String = "OWNER",
        val changeReason: String? = null,
        val updatedAt: Long = System.currentTimeMillis()
    )
}
