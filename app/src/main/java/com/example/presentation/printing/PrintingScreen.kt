package com.example.presentation.printing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ErpRepositoryImpl
import com.example.domain.model.Sale
import com.example.domain.money.Money
import com.example.integration.printing.PrintService
import com.example.presentation.theme.Emerald600
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrintingScreen(
    repository: ErpRepositoryImpl,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sales by repository.getAllSales().collectAsState(initial = emptyList())
    var selectedSaleForPreview by remember { mutableStateOf<Sale?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Arsip Dokumen & Cetak (CETAK)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Cetak ulang nota kasir 58mm/80mm atau bagikan struk via WhatsApp.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (sales.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Filled.Receipt, contentDescription = null, tint = Emerald600)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada arsip nota transaksi.", fontWeight = FontWeight.SemiBold)
                    Text("Lakukan transaksi di tab JUAL untuk membuat nota baru.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(sales) { sale ->
                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(sale.createdAt))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSaleForPreview = sale }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sale.saleId,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "$dateStr • Kasir: ${sale.cashierId}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = if (sale.isCreditSale) "Status: TEMPO" else "Status: ${sale.saleStatus}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (sale.saleStatus == "VOIDED") MaterialTheme.colorScheme.error else Emerald600
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Money.formatRupiah(sale.grandTotal),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Emerald600
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { selectedSaleForPreview = sale },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lihat", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Receipt Preview Dialog
    if (selectedSaleForPreview != null) {
        val sale = selectedSaleForPreview!!
        val receiptText = PrintService.formatReceipt58mm(sale, "Omah Sembako", "Jl. Raya Pasar Sembako No. 7", "0812-3456-7890")

        AlertDialog(
            onDismissRequest = { selectedSaleForPreview = null },
            title = { Text("Pratinjau Struk Thermal 58mm", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    text = receiptText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PrintService.shareViaWhatsApp(context, receiptText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kirim WA")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSaleForPreview = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}
