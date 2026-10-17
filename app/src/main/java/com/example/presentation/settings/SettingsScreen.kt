package com.example.presentation.settings

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ErpRepositoryImpl
import com.example.domain.model.CashierShift
import com.example.domain.model.DashboardKpis
import com.example.domain.model.Role
import com.example.domain.money.Money
import com.example.presentation.components.KpiSummaryCard
import com.example.presentation.theme.Amber600
import com.example.presentation.theme.Emerald600
import com.example.presentation.theme.ErrorRed
import com.example.presentation.theme.ThemePackOption
import com.example.presentation.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: ErpRepositoryImpl,
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val currentRole by appViewModel.currentRole.collectAsState()
    val activeShift by appViewModel.activeShift.collectAsState()
    val themePack by appViewModel.themePack.collectAsState()
    val isModeJagaActive by appViewModel.isModeJagaActive.collectAsState()

    var kpis by remember { mutableStateOf<DashboardKpis?>(null) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }
    var showPriveDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentRole) {
        kpis = repository.calculateKpis(currentRole)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Pengaturan & Operasional (LAINNYA)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manajemen shift kasir, akuntansi SAK EMKM, delegasi Mode Jaga, dan konfigurasi toko.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (statusMessage != null) {
            item {
                Surface(
                    color = Emerald600.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage!!,
                        color = Emerald600,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // --- 1. SHIFT KASIR ---
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PointOfSale, contentDescription = null, tint = Emerald600)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buku Shift Kasir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            color = if (activeShift != null) Emerald600.copy(alpha = 0.15f) else Amber600.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (activeShift != null) "SHIFT AKTIF" else "SHIFT TERTUTUP",
                                color = if (activeShift != null) Emerald600 else Amber600,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    if (activeShift != null) {
                        val shift = activeShift!!
                        Text("Kasir: ${shift.cashierName} • Perangkat: ${shift.deviceId}", fontSize = 12.sp)
                        Text("Modal Awal: ${Money.formatRupiah(shift.startingCash)}", fontSize = 12.sp)
                        Text(
                            "Kas Harapan di Laci: ${Money.formatRupiah(shift.expectedCash)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald600,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showCloseShiftDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Amber600),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tutup Shift & Rekonsiliasi Kas")
                        }
                    } else {
                        Button(
                            onClick = { appViewModel.openCashierShift(200000L) },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buka Shift Baru (Modal Rp 200.000)")
                        }
                    }
                }
            }
        }

        // --- 2. AKUNTANSI SAK EMKM ---
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = Emerald600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ringkasan Keuangan SAK EMKM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    if (kpis != null) {
                        val k = kpis!!
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KpiSummaryCard(title = "Omzet Penjualan", value = Money.formatRupiah(k.omzet), modifier = Modifier.weight(1f))
                            if (currentRole == Role.OWNER && k.labaKotor != null) {
                                KpiSummaryCard(title = "Laba Kotor", value = Money.formatRupiah(k.labaKotor), badgeText = "${"%.1f".format(k.marginPercent)}%", modifier = Modifier.weight(1f))
                            }
                        }
                        if (currentRole == Role.OWNER && k.kasBank != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                KpiSummaryCard(title = "Kas & Bank", value = Money.formatRupiah(k.kasBank), modifier = Modifier.weight(1f))
                                KpiSummaryCard(title = "Nilai Persediaan", value = Money.formatRupiah(k.nilaiPersediaan ?: 0L), modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (currentRole == Role.OWNER) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showPriveDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tarik Prive Pemilik (Owner Drawing)")
                        }
                    }
                }
            }
        }

        // --- 3. MODE JAGA DELEGASI ---
        if (currentRole == Role.OWNER) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Security, contentDescription = null, tint = Amber600)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Mode Jaga (Delegasi Kasir)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Beri wewenang darurat bayar supplier max Rp 2jt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            Switch(
                                checked = isModeJagaActive,
                                onCheckedChange = { appViewModel.toggleModeJaga(it) }
                            )
                        }
                    }
                }
            }
        }

        // --- 4. THEME PACK ---
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = Emerald600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tema Tampilan (ThemePack)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePackOption.values().forEach { pack ->
                            FilterChip(
                                selected = themePack == pack,
                                onClick = { appViewModel.setThemePack(pack) },
                                label = { Text(pack.title, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Close Shift Dialog
    if (showCloseShiftDialog) {
        var actualCashText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCloseShiftDialog = false },
            title = { Text("Tutup Shift & Rekonsiliasi Kas", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Hitung fisik uang tunai di laci kasir:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = actualCashText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) actualCashText = it },
                        label = { Text("Uang Fisik Aktual di Laci (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Catatan Selisih (Wajib jika ada selisih)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val actual = actualCashText.toLongOrNull() ?: 0L
                        appViewModel.closeCashierShift(actual, noteText.ifBlank { null })
                        showCloseShiftDialog = false
                        statusMessage = "Shift berhasil ditutup dan disegel permanen!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber600)
                ) {
                    Text("Segel Shift")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseShiftDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Prive Dialog
    if (showPriveDialog) {
        var amountText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPriveDialog = false },
            title = { Text("Penarikan Prive Pemilik (Owner)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Penarikan kas untuk keperluan pribadi pemilik (mengurangi modal ekuitas, tidak membebani laba rugi).", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) amountText = it },
                        label = { Text("Nominal Penarikan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Keperluan / Keterangan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toLongOrNull() ?: 0L
                        if (amount > 0L) {
                            scope.launch {
                                repository.recordManualPrive(amount, noteText.ifBlank { "Prive Tunai" }, "OWNER-01")
                                showPriveDialog = false
                                statusMessage = "Prive berhasil dibukukan ke jurnal ekuitas!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Simpan Prive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriveDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
