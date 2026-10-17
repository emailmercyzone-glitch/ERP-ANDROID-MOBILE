package com.example.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavTab(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    JUAL("jual", "JUAL", Icons.Filled.ShoppingCart, "Kasir POS, Scan, & Penjualan"),
    BELI("beli", "BELI", Icons.Filled.Assignment, "Pengadaan & Gudang GRN"),
    BAYAR("bayar", "BAYAR", Icons.Filled.Payments, "Piutang, Hutang, & Kas"),
    CETAK("cetak", "CETAK", Icons.Filled.Print, "Arsip Nota & Dokumen"),
    LAINNYA("lainnya", "LAINNYA", Icons.Filled.MoreHoriz, "Shift, Akuntansi, & Pengaturan")
}
