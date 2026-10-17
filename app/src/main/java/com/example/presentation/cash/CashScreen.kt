package com.example.presentation.cash

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.model.Contact
import com.example.domain.model.Expense
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Role
import com.example.domain.money.Money
import com.example.presentation.theme.Amber600
import com.example.presentation.theme.Emerald600
import com.example.presentation.theme.ErrorRed
import kotlinx.coroutines.launch

@Composable
fun CashScreen(
    repository: ErpRepositoryImpl,
    currentRole: Role,
    isModeJagaActive: Boolean,
    cashierId: String,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Piutang, 1 = Hutang, 2 = Beban

    val customers by repository.getCustomers().collectAsState(initial = emptyList())
    val suppliers by repository.getSuppliers().collectAsState(initial = emptyList())
    val expenses by repository.getAllExpenses().collectAsState(initial = emptyList())

    var showPayPiutangDialog by remember { mutableStateOf<Contact?>(null) }
    var showPayHutangDialog by remember { mutableStateOf<Contact?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    var actionMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Manajemen Kas, Piutang & Hutang",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Piutang Pelanggan", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Hutang Supplier", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Beban Operasional", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        if (actionMessage != null) {
            Surface(
                color = Emerald600.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(
                    text = actionMessage!!,
                    color = Emerald600,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedSubTab) {
            0 -> {
                // Piutang Pelanggan
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    item {
                        Text(
                            text = "Daftar Langganan & Tagihan Bon",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(customers) { cust ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cust.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Telp: ${cust.phoneNote ?: "-"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("Plafon: ${Money.formatRupiah(cust.creditLimit)} • Tempo: ${cust.creditTermDays} hari", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text(
                                        "Sisa Bon: ${Money.formatRupiah(cust.currentBalance)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (cust.currentBalance > 0L) ErrorRed else Emerald600,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Button(
                                    onClick = { showPayPiutangDialog = cust },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                                ) {
                                    Text("Bayar", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Hutang Supplier (Role Protected: Owner or Mode Jaga)
                val isAllowed = currentRole == Role.OWNER || isModeJagaActive
                if (!isAllowed) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = Amber600)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Akses Terkunci untuk Kasir", fontWeight = FontWeight.Bold)
                            Text("Buku hutang supplier khusus wewenang Owner (atau Kasir saat Mode Jaga aktif).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        item {
                            Text(
                                text = "Daftar Supplier & Hutang Kulakan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(suppliers) { supp ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(supp.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("Telp: ${supp.phoneNote ?: "-"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Text(
                                            "Hutang Berjalan: ${Money.formatRupiah(supp.currentBalance)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (supp.currentBalance > 0L) ErrorRed else Emerald600,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Button(
                                        onClick = { showPayHutangDialog = supp },
                                        colors = ButtonDefaults.buttonColors(containerColor = Amber600)
                                    ) {
                                        Text("Lunasi", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Beban Operasional
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Riwayat Beban Toko",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = { showAddExpenseDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Catat Beban", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (expenses.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text("Belum ada pencatatan beban operasional.", fontWeight = FontWeight.SemiBold)
                                Text("Catat pengeluaran listrik, gaji, BBM, atau perlengkapan toko.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(expenses) { exp ->
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
                                            Text(exp.category.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (exp.note != null) {
                                                Text(exp.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                            }
                                        }
                                        Text(
                                            Money.formatRupiah(exp.amount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ErrorRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pay Customer Piutang Dialog
    if (showPayPiutangDialog != null) {
        val cust = showPayPiutangDialog!!
        var amountText by remember { mutableStateOf("") }
        var payMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }

        AlertDialog(
            onDismissRequest = { showPayPiutangDialog = null },
            title = { Text("Pelunasan Piutang: ${cust.displayName}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Sisa Bon: ${Money.formatRupiah(cust.currentBalance)}", color = ErrorRed, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) amountText = it },
                        label = { Text("Nominal Cicilan / Pelunasan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        FilterChip(
                            selected = payMethod == PaymentMethod.TUNAI,
                            onClick = { payMethod = PaymentMethod.TUNAI },
                            label = { Text("Tunai") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = payMethod == PaymentMethod.TRANSFER,
                            onClick = { payMethod = PaymentMethod.TRANSFER },
                            label = { Text("Transfer Bank") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toLongOrNull() ?: 0L
                        if (amount > 0L) {
                            scope.launch {
                                repository.recordReceivablePayment(cust.contactId, amount, payMethod, null)
                                showPayPiutangDialog = null
                                actionMessage = "Berhasil mencatat kwitansi pelunasan cicilan tertua!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Simpan Kwitansi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayPiutangDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Pay Supplier Hutang Dialog
    if (showPayHutangDialog != null) {
        val supp = showPayHutangDialog!!
        var amountText by remember { mutableStateOf("") }
        var payMethod by remember { mutableStateOf(PaymentMethod.TRANSFER) }

        AlertDialog(
            onDismissRequest = { showPayHutangDialog = null },
            title = { Text("Bayar Hutang Supplier: ${supp.displayName}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Hutang Berjalan: ${Money.formatRupiah(supp.currentBalance)}", color = ErrorRed, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) amountText = it },
                        label = { Text("Nominal Pembayaran (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                repository.recordPayablePayment(supp.contactId, amount, payMethod, false, null)
                                showPayHutangDialog = null
                                actionMessage = "Berhasil mencatat pembayaran hutang supplier!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber600)
                ) {
                    Text("Bayar Hutang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayHutangDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        var selectedCategory by remember { mutableStateOf(ExpenseCategory.LISTRIK) }
        var amountText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Catat Beban Operasional Toko", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Kategori Beban:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row {
                        ExpenseCategory.values().take(3).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName.take(10), fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) amountText = it },
                        label = { Text("Nominal Pengeluaran (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Keterangan / Nomor Token / Nota") },
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
                                val expense = Expense(
                                    expenseId = "EXP-${System.currentTimeMillis()}",
                                    category = selectedCategory,
                                    amount = amount,
                                    note = noteText.ifBlank { null },
                                    approvedBy = cashierId
                                )
                                repository.recordExpense(expense)
                                showAddExpenseDialog = false
                                actionMessage = "Berhasil mencatat beban operasional!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Simpan Beban")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
