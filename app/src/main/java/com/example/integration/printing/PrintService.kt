package com.example.integration.printing

import android.content.Context
import android.content.Intent
import com.example.domain.money.Money
import com.example.domain.model.Sale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Service for thermal receipt generation (58mm/32-column & 80mm/48-column) and WhatsApp share fallback
 * as specified in PRINTING.md §2 and §4.
 */
object PrintService {

    fun formatReceipt58mm(sale: Sale, storeName: String, storeAddress: String, storePhone: String): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(sale.createdAt))
        val sb = StringBuilder()
        val line = "=".repeat(32)
        val dash = "-".repeat(32)

        sb.appendLine(line)
        sb.appendLine(center(storeName, 32))
        sb.appendLine(center(storeAddress, 32))
        sb.appendLine(center("Telp: $storePhone", 32))
        sb.appendLine(line)
        sb.appendLine("No. Nota : ${sale.saleId}")
        sb.appendLine("Tanggal  : $dateStr")
        sb.appendLine("Kasir    : ${sale.cashierId}")
        if (sale.customerId != null) {
            sb.appendLine("Pelanggan: ${sale.customerId}")
        }
        sb.appendLine(dash)

        for (item in sale.items) {
            sb.appendLine(item.productName)
            val left = "  ${item.qty} x ${Money.formatRupiah(item.unitPrice)}"
            val right = Money.formatRupiah(item.lineTotal)
            sb.appendLine(padBetween(left, right, 32))
        }

        sb.appendLine(dash)
        sb.appendLine(padBetween("Subtotal", Money.formatRupiah(sale.subtotal), 32))
        if (sale.packagingFee > 0L) {
            sb.appendLine(padBetween("Biaya Packaging", Money.formatRupiah(sale.packagingFee), 32))
        }
        if (sale.deliveryFee > 0L) {
            sb.appendLine(padBetween("Ongkos Kirim", Money.formatRupiah(sale.deliveryFee), 32))
        }
        if (sale.roundingDiff != 0L) {
            sb.appendLine(padBetween("Pembulatan Tunai", Money.formatRupiah(sale.roundingDiff), 32))
        }
        sb.appendLine(line)
        sb.appendLine(padBetween("TOTAL AKHIR", Money.formatRupiah(sale.grandTotal), 32))
        if (sale.paidCash > 0L) {
            sb.appendLine(padBetween("Bayar Tunai", Money.formatRupiah(sale.paidCash), 32))
            sb.appendLine(padBetween("Kembalian", Money.formatRupiah(sale.changeReturned), 32))
        }
        if (sale.paidNonCash > 0L) {
            sb.appendLine(padBetween("Bayar Non-Tunai", Money.formatRupiah(sale.paidNonCash), 32))
        }
        if (sale.isCreditSale) {
            sb.appendLine(padBetween("Status", "TEMPO / BON", 32))
        }
        sb.appendLine(line)
        sb.appendLine(center("Terima Kasih Telah Belanja!", 32))
        sb.appendLine(center("Barang cacat dapat ditukar", 32))
        sb.appendLine(center("maksimal 24 jam dengan nota.", 32))
        sb.appendLine(line)

        return sb.toString()
    }

    private fun center(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }

    private fun padBetween(left: String, right: String, width: Int): String {
        val space = width - left.length - right.length
        return if (space > 0) left + " ".repeat(space) + right else "$left $right"
    }

    fun shareViaWhatsApp(context: Context, textReceipt: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textReceipt)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Bagikan Struk Belanja"))
    }
}
