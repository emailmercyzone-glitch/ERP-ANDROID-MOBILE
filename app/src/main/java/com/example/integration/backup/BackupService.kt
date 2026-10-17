package com.example.integration.backup

/**
 * Encrypted JSON backup contract as mandated by DEC-BKP-001, D-025, and STORAGE_BACKUP_ARCHITECTURE_BRIEF.md.
 * Ensures local operational database snapshot is serialized to versioned JSON
 * before encryption with AES-256-GCM.
 */
object BackupService {

    data class BackupMetadata(
        val formatIdentifier: String = "OMAH_ERP_BACKUP",
        val formatVersion: Int = 1,
        val schemaVersion: Int = 1,
        val backupId: String,
        val deviceId: String,
        val appVersion: String = "1.0.0",
        val createdAt: Long = System.currentTimeMillis(),
        val checksumSha256: String
    )

    data class BackupEnvelope(
        val metadata: BackupMetadata,
        val payloadCiphertextBase64: String,
        val saltBase64: String,
        val ivBase64: String
    )
}
