package com.example.presentation.purchasing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ErpRepositoryImpl
import com.example.domain.model.Product
import com.example.domain.money.Money
import com.example.presentation.theme.Amber600
import com.example.presentation.theme.Emerald600
import com.example.presentation.theme.ErrorRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchasingScreen(
    repository: ErpRepositoryImpl,
    cashierId: String,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val products by repository.getAllProducts().collectAsState(initial = emptyList())
    val suppliers by repository.getSuppliers().collectAsState(initial = emptyList())
    val movements by repository.getAllMovements().collectAsState(initial = emptyList())

    var showGrnDialog by remember { mutableStateOf(false) }
    var grnSuccessMessage by remember { mutableStateOf<String?>(null) }
    var grnErrorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showGrnDialog = true },
                containerColor = Emerald600,
                modifier = Modifier.testTag("tambah_grn_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Terima Barang", tint = androidx.compose.ui.graphics.Color.White)
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Pengadaan & Gudang (GRN)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Penerimaan kulakan sembako membentuk lapisan stok FIFO otomatis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            if (grnSuccessMessage != null) {
                Surface(
                    color = Emerald600.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = grnSuccessMessage!!,
                        color = Emerald600,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Riwayat Mutasi Stok Terbaru",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (movements.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Filled.Inventory, contentDescription = null, tint = Emerald600)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada mutasi stok.", fontWeight = FontWeight.SemiBold)
                        Text("Tekan tombol + di bawah untuk input terima barang (GRN).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(movements) { mov ->
                        val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.US).format(Date(mov.createdAt))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${mov.movementType} (${mov.refDocumentId})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (mov.qtyChange > 0) Emerald600 else Amber600
                                    )
                                    Text(
                                        text = "Produk ID: ${mov.productId} • $dateStr",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (mov.qtyChange > 0) "+${mov.qtyChange}" else "${mov.qtyChange}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (mov.qtyChange > 0) Emerald600 else ErrorRed
                                    )
                                    Text(
                                        text = "Sisa: ${mov.balanceAfter}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Input GRN Dialog
    if (showGrnDialog) {
        var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.contactId ?: "SUPP-01") }
        var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: "PRD-01") }
        var qtyText by remember { mutableStateOf("") }
        var unitCostText by remember { mutableStateOf("") }
        var isPaidCash by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = {
                showGrnDialog = false
                grnErrorMessage = null
            },
            title = { Text("Input Penerimaan Barang (GRN)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Pilih Supplier:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row {
                        suppliers.forEach { s ->
                            FilterChip(
                                selected = selectedSupplierId == s.contactId,
                                onClick = { selectedSupplierId = s.contactId },
                                label = { Text(s.displayName.take(12), fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pilih Produk:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row {
                        products.take(4).forEach { p ->
                            FilterChip(
                                selected = selectedProductId == p.id,
                                onClick = { selectedProductId = p.id },
                                label = { Text(p.name.take(10), fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) qtyText = it },
                        label = { Text("Jumlah Masuk (Kuantitas Dasar)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = unitCostText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) unitCostText = it },
                        label = { Text("Harga Modal / Beli per Unit (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = isPaidCash,
                            onClick = { isPaidCash = true },
                            label = { Text("Lunas Tunai", fontSize = 11.sp) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = !isPaidCash,
                            onClick = { isPaidCash = false },
                            label = { Text("Tempo (Hutang)", fontSize = 11.sp) }
                        )
                    }
                    if (grnErrorMessage != null) {
                        Text(text = grnErrorMessage!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = qtyText.toLongOrNull() ?: 0L
                        val cost = unitCostText.toLongOrNull() ?: 0L
                        if (qty <= 0L || cost <= 0L) {
                            grnErrorMessage = "Jumlah dan modal harus > 0!"
                            return@Button
                        }
                        scope.launch {
                            val res = repository.recordGoodsReceipt(
                                supplierId = selectedSupplierId,
                                productId = selectedProductId,
                                qty = qty,
                                unitCost = cost,
                                isPaidCash = isPaidCash,
                                actorId = cashierId
                            )
                            res.onSuccess {
                                showGrnDialog = false
                                grnSuccessMessage = "Berhasil mencatat GRN & membuat lapisan FIFO stok baru!"
                            }.onFailure {
                                grnErrorMessage = it.message ?: "Gagal mencatat penerimaan!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Simpan GRN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGrnDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
