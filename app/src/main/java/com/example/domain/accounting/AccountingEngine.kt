package com.example.domain.accounting

import com.example.domain.model.JournalEntry
import com.example.domain.model.JournalLine

/**
 * Double-Entry Accounting Engine compliant with SAK EMKM standards as specified in
 * PRIN-ACC-01..03, ACC-R01..R05, and POSTING_MODEL.md.
 * Invariant: Every transaction generates balanced JournalLines where Total Debit == Total Credit.
 */
object AccountingEngine {

    // Standard COA Account Codes
    const val ACC_KAS_TOKO = "1-1100"
    const val ACC_BANK_TOKO = "1-1110"
    const val ACC_PIUTANG_QRIS = "1-1120"
    const val ACC_PIUTANG_USAHA = "1-1200"
    const val ACC_PERSEDIAAN = "1-1300"

    const val ACC_HUTANG_SUPPLIER = "2-1100"
    const val ACC_HUTANG_KONSINYASI = "2-1200"
    const val ACC_HUTANG_PAJAK = "2-1300"

    const val ACC_MODAL_PEMILIK = "3-1100"
    const val ACC_PRIVE_PEMILIK = "3-1200"
    const val ACC_LABA_DITAHAN = "3-1300"

    const val ACC_PENDAPATAN_JUAL = "4-1100"
    const val ACC_PENDAPATAN_KOMISI_KNS = "4-1200"
    const val ACC_PENDAPATAN_JASA_ONGKIR = "4-1300"

    const val ACC_HPP = "5-1100"
    const val ACC_BEBAN_GAJI = "5-1200"
    const val ACC_BEBAN_MDR_QRIS = "5-1300"
    const val ACC_BEBAN_OPERASIONAL = "5-1400"
    const val ACC_BEBAN_SELISIH_STOK = "5-1450"
    const val ACC_BEBAN_SELISIH_KAS = "5-1500"
    const val ACC_BEBAN_SELISIH_PEMBULATAN = "5-1510"
    const val ACC_BEBAN_PAJAK = "5-1600"

    val COA_MAP = mapOf(
        ACC_KAS_TOKO to "Kas Toko (Laci & Brankas)",
        ACC_BANK_TOKO to "Rekening Bank Toko",
        ACC_PIUTANG_QRIS to "Piutang Settlement QRIS",
        ACC_PIUTANG_USAHA to "Piutang Usaha Pelanggan",
        ACC_PERSEDIAAN to "Persediaan Barang Dagang Sembako",
        ACC_HUTANG_SUPPLIER to "Hutang Usaha Supplier Kulakan",
        ACC_HUTANG_KONSINYASI to "Hutang Konsinyasi Penitip",
        ACC_HUTANG_PAJAK to "Hutang Pajak Usaha",
        ACC_MODAL_PEMILIK to "Modal Disetor Pemilik",
        ACC_PRIVE_PEMILIK to "Prive Pemilik Toko",
        ACC_LABA_DITAHAN to "Laba Ditahan / Akumulasi",
        ACC_PENDAPATAN_JUAL to "Pendapatan Penjualan Barang",
        ACC_PENDAPATAN_KOMISI_KNS to "Pendapatan Komisi Titipan Konsinyasi",
        ACC_PENDAPATAN_JASA_ONGKIR to "Pendapatan Jasa & Ongkir",
        ACC_HPP to "Beban Pokok Penjualan (HPP / COGS)",
        ACC_BEBAN_GAJI to "Beban Gaji Karyawan Toko",
        ACC_BEBAN_MDR_QRIS to "Beban MDR Transaksi QRIS",
        ACC_BEBAN_OPERASIONAL to "Beban Operasional Toko",
        ACC_BEBAN_SELISIH_STOK to "Beban Kerusakan / Selisih Stok",
        ACC_BEBAN_SELISIH_KAS to "Beban Selisih Kasir (Short/Over)",
        ACC_BEBAN_SELISIH_PEMBULATAN to "Beban Selisih Pembulatan Kas",
        ACC_BEBAN_PAJAK to "Beban Pajak PPh Usaha"
    )

    fun accountName(code: String): String = COA_MAP[code] ?: "Akun $code"

    fun createSaleJournal(
        journalId: String,
        saleId: String,
        paidCash: Long,
        paidQris: Long,
        paidTransfer: Long,
        receivableAmount: Long,
        cogsTotal: Long,
        equitySalesRevenue: Long,
        consignorPayable: Long,
        consignmentCommission: Long,
        packagingAndDeliveryFee: Long,
        roundingDiff: Long,
        timestamp: Long = System.currentTimeMillis()
    ): JournalEntry {
        val lines = mutableListOf<JournalLine>()

        // DEBITS
        if (paidCash > 0L) {
            lines.add(JournalLine(ACC_KAS_TOKO, accountName(ACC_KAS_TOKO), debit = paidCash, credit = 0L))
        }
        if (paidQris > 0L) {
            lines.add(JournalLine(ACC_PIUTANG_QRIS, accountName(ACC_PIUTANG_QRIS), debit = paidQris, credit = 0L))
        }
        if (paidTransfer > 0L) {
            lines.add(JournalLine(ACC_BANK_TOKO, accountName(ACC_BANK_TOKO), debit = paidTransfer, credit = 0L))
        }
        if (receivableAmount > 0L) {
            lines.add(JournalLine(ACC_PIUTANG_USAHA, accountName(ACC_PIUTANG_USAHA), debit = receivableAmount, credit = 0L))
        }
        if (cogsTotal > 0L) {
            lines.add(JournalLine(ACC_HPP, accountName(ACC_HPP), debit = cogsTotal, credit = 0L))
        }
        if (roundingDiff < 0L) {
            val discountExpense = Math.abs(roundingDiff)
            lines.add(JournalLine(ACC_BEBAN_SELISIH_PEMBULATAN, accountName(ACC_BEBAN_SELISIH_PEMBULATAN), debit = discountExpense, credit = 0L))
        }

        // CREDITS
        if (equitySalesRevenue > 0L) {
            lines.add(JournalLine(ACC_PENDAPATAN_JUAL, accountName(ACC_PENDAPATAN_JUAL), debit = 0L, credit = equitySalesRevenue))
        }
        if (consignorPayable > 0L) {
            lines.add(JournalLine(ACC_HUTANG_KONSINYASI, accountName(ACC_HUTANG_KONSINYASI), debit = 0L, credit = consignorPayable))
        }
        if (consignmentCommission > 0L) {
            lines.add(JournalLine(ACC_PENDAPATAN_KOMISI_KNS, accountName(ACC_PENDAPATAN_KOMISI_KNS), debit = 0L, credit = consignmentCommission))
        }
        if (packagingAndDeliveryFee > 0L) {
            lines.add(JournalLine(ACC_PENDAPATAN_JASA_ONGKIR, accountName(ACC_PENDAPATAN_JASA_ONGKIR), debit = 0L, credit = packagingAndDeliveryFee))
        }
        if (cogsTotal > 0L) {
            lines.add(JournalLine(ACC_PERSEDIAAN, accountName(ACC_PERSEDIAAN), debit = 0L, credit = cogsTotal))
        }
        if (roundingDiff > 0L) {
            lines.add(JournalLine(ACC_BEBAN_SELISIH_PEMBULATAN, accountName(ACC_BEBAN_SELISIH_PEMBULATAN), debit = 0L, credit = roundingDiff))
        }

        return JournalEntry(
            journalId = journalId,
            sourceDocumentId = saleId,
            sourceModule = "SALES",
            transactionDate = timestamp,
            memo = "Penjualan Kasir POS Nota #$saleId",
            lines = lines
        )
    }

    fun createGrnJournal(
        journalId: String,
        grnId: String,
        totalInventoryCost: Long,
        isPaidCash: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ): JournalEntry {
        val creditAccount = if (isPaidCash) ACC_KAS_TOKO else ACC_HUTANG_SUPPLIER
        val lines = listOf(
            JournalLine(ACC_PERSEDIAAN, accountName(ACC_PERSEDIAAN), debit = totalInventoryCost, credit = 0L),
            JournalLine(creditAccount, accountName(creditAccount), debit = 0L, credit = totalInventoryCost)
        )
        return JournalEntry(
            journalId = journalId,
            sourceDocumentId = grnId,
            sourceModule = "PURCHASING",
            transactionDate = timestamp,
            memo = "Penerimaan Barang Kulakan GRN #$grnId",
            lines = lines
        )
    }

    fun createReceivablePaymentJournal(
        journalId: String,
        paymentId: String,
        amount: Long,
        isBankTransfer: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ): JournalEntry {
        val debitAccount = if (isBankTransfer) ACC_BANK_TOKO else ACC_KAS_TOKO
        val lines = listOf(
            JournalLine(debitAccount, accountName(debitAccount), debit = amount, credit = 0L),
            JournalLine(ACC_PIUTANG_USAHA, accountName(ACC_PIUTANG_USAHA), debit = 0L, credit = amount)
        )
        return JournalEntry(
            journalId = journalId,
            sourceDocumentId = paymentId,
            sourceModule = "CASH",
            transactionDate = timestamp,
            memo = "Pelunasan Piutang Pelanggan KWT #$paymentId",
            lines = lines
        )
    }

    fun createPayablePaymentJournal(
        journalId: String,
        paymentId: String,
        amount: Long,
        isBankTransfer: Boolean,
        isConsignment: Boolean = false,
        timestamp: Long = System.currentTimeMillis()
    ): JournalEntry {
        val creditAccount = if (isBankTransfer) ACC_BANK_TOKO else ACC_KAS_TOKO
        val debitAccount = if (isConsignment) ACC_HUTANG_KONSINYASI else ACC_HUTANG_SUPPLIER
        val lines = listOf(
            JournalLine(debitAccount, accountName(debitAccount), debit = amount, credit = 0L),
            JournalLine(creditAccount, accountName(creditAccount), debit = 0L, credit = amount)
        )
        return JournalEntry(
            journalId = journalId,
            sourceDocumentId = paymentId,
            sourceModule = "CASH",
            transactionDate = timestamp,
            memo = "Pembayaran Hutang BYR #$paymentId",
            lines = lines
        )
    }

    fun createExpenseJournal(
        journalId: String,
        expenseId: String,
        category: String,
        accountCode: String,
        amount: Long,
        timestamp: Long = System.currentTimeMillis()
    ): JournalEntry {
        val lines = listOf(
            JournalLine(accountCode, accountName(accountCode), debit = amount, credit = 0L),
            JournalLine(ACC_KAS_TOKO, accountName(ACC_KAS_TOKO), debit = 0L, credit = amount)
        )
        return JournalEntry(
            journalId = journalId,
            sourceDocumentId = expenseId,
            sourceModule = "CASH",
            transactionDate = timestamp,
            memo = "Biaya Operasional: $category (#$expenseId)",
            lines = lines
        )
    }

    fun createPriveJournal(
        journalId: String,
        amount: Long,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): JournalEntry {
        val lines = listOf(
            JournalLine(ACC_PRIVE_PEMILIK, accountName(ACC_PRIVE_PEMILIK), debit = amount, credit = 0L),
            JournalLine(ACC_KAS_TOKO, accountName(ACC_KAS_TOKO), debit = 0L, credit = amount)
        )
        return JournalEntry(
            journalId = journalId,
            sourceDocumentId = "PRIVE-$timestamp",
            sourceModule = "MANUAL",
            transactionDate = timestamp,
            memo = "Penarikan Prive Pemilik Toko: $note",
            lines = lines
        )
    }
}
