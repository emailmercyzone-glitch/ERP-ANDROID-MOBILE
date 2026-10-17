package com.example.presentation.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ErpRepositoryImpl
import com.example.domain.cash.CashRounding
import com.example.domain.model.CartItem
import com.example.domain.model.Contact
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Product
import com.example.domain.model.ProductKind
import com.example.domain.model.Sale
import com.example.domain.money.Money
import com.example.integration.printing.PrintService
import com.example.presentation.theme.Amber600
import com.example.presentation.theme.Emerald50
import com.example.presentation.theme.Emerald600
import com.example.presentation.theme.ErrorRed
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    repository: ErpRepositoryImpl,
    cashierId: String,
    shiftId: String,
    isModeRame: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val products by repository.getAllProducts().collectAsState(initial = emptyList())
    val customers by repository.getCustomers().collectAsState(initial = emptyList())

    val cartItems = remember { mutableStateListOf<CartItem>() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }

    var showCartSheet by remember { mutableStateOf(false) }
    var showCheckoutModal by remember { mutableStateOf(false) }
    var completedSale by remember { mutableStateOf<Sale?>(null) }
    var checkoutErrorMessage by remember { mutableStateOf<String?>(null) }

    val categories = remember(products) {
        listOf("Semua") + products.map { it.categoryId }.distinct()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            (selectedCategory == "Semua" || p.categoryId == selectedCategory) &&
            (searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true) || p.sku.contains(searchQuery, ignoreCase = true))
        }
    }

    val cartItemCount = cartItems.sumOf { it.qty }
    val cartSubtotal = cartItems.sumOf { it.lineTotal }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search and Filter Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari sembako, beras, minyak...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Emerald600) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_search_input")
                )
            }

            // Category Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald600,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Product List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductRowCard(
                        product = product,
                        onAddToCart = {
                            val existingIndex = cartItems.indexOfFirst { it.product.id == product.id }
                            if (existingIndex >= 0) {
                                val current = cartItems[existingIndex]
                                cartItems[existingIndex] = current.copy(qty = current.qty + 1)
                            } else {
                                cartItems.add(CartItem(product = product, qty = 1L))
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(90.dp)) // padding for cart bar
                }
            }
        }

        // Floating Cart Bar
        if (cartItems.isNotEmpty()) {
            Surface(
                color = Emerald600,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clickable { showCartSheet = true }
                    .testTag("bottom_cart_bar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$cartItemCount Barang dalam Keranjang",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = Money.formatRupiah(cartSubtotal),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { showCheckoutModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("bayar_button")
                    ) {
                        Text(
                            text = "BAYAR",
                            color = Emerald600,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Cart Bottom Sheet
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Keranjang Belanja POS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(cartItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "${Money.formatRupiah(item.unitPrice)} / ${item.uomName}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val idx = cartItems.indexOf(item)
                                        if (item.qty > 1) {
                                            cartItems[idx] = item.copy(qty = item.qty - 1)
                                        } else {
                                            cartItems.removeAt(idx)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Kurang")
                                }
                                Text("${item.qty}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(
                                    onClick = {
                                        val idx = cartItems.indexOf(item)
                                        cartItems[idx] = item.copy(qty = item.qty + 1)
                                    }
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Tambah")
                                }
                                IconButton(
                                    onClick = { cartItems.remove(item) }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = ErrorRed)
                                }
                            }
                        }
                        Divider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Belanja:", style = MaterialTheme.typography.titleMedium)
                    Text(Money.formatRupiah(cartSubtotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Emerald600)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        showCartSheet = false
                        showCheckoutModal = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Lanjutkan ke Pembayaran", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Checkout Dialog
    if (showCheckoutModal) {
        CheckoutDialog(
            cartSubtotal = cartSubtotal,
            customers = customers,
            onDismiss = {
                showCheckoutModal = false
                checkoutErrorMessage = null
            },
            onConfirmCheckout = { method, customerId, packagingFee, deliveryFee, cashPaid, isTempo ->
                scope.launch {
                    val rawGrand = cartSubtotal + packagingFee + deliveryFee
                    // Rounding for cash (CSH-R05, D-013)
                    val (roundedGrand, roundingDiff) = if (method == PaymentMethod.TUNAI) {
                        CashRounding.round500(rawGrand)
                    } else {
                        Pair(rawGrand, 0L)
                    }

                    val paidCashAmount = if (method == PaymentMethod.TUNAI && !isTempo) cashPaid else 0L
                    val paidQrisAmount = if (method == PaymentMethod.QRIS && !isTempo) roundedGrand else 0L
                    val paidTransferAmount = if (method == PaymentMethod.TRANSFER && !isTempo) roundedGrand else 0L

                    val checkoutRequestId = UUID.randomUUID().toString()

                    val result = repository.executeSaleCheckout(
                        shiftId = shiftId,
                        cashierId = cashierId,
                        customerId = customerId,
                        cartItems = cartItems.toList(),
                        paidCash = paidCashAmount,
                        paidQris = paidQrisAmount,
                        paidTransfer = paidTransferAmount,
                        packagingFee = packagingFee,
                        deliveryFee = deliveryFee,
                        roundingDiff = roundingDiff,
                        isCreditSale = isTempo,
                        checkoutRequestId = checkoutRequestId
                    )

                    result.onSuccess { sale ->
                        cartItems.clear()
                        showCheckoutModal = false
                        completedSale = sale
                    }.onFailure { err ->
                        checkoutErrorMessage = err.message ?: "Gagal memproses transaksi!"
                    }
                }
            },
            errorMessage = checkoutErrorMessage
        )
    }

    // Success Receipt Dialog
    if (completedSale != null) {
        val sale = completedSale!!
        val textReceipt = PrintService.formatReceipt58mm(sale, "Omah Sembako", "Jl. Raya Pasar Sembako No. 7", "0812-3456-7890")
        AlertDialog(
            onDismissRequest = { completedSale = null },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(48.dp)) },
            title = { Text("Transaksi Berhasil!", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Nota: ${sale.saleId}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Total: ${Money.formatRupiah(sale.grandTotal)}", color = Emerald600, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (sale.changeReturned > 0L) {
                        Text("Kembalian: ${Money.formatRupiah(sale.changeReturned)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            item {
                                Text(
                                    text = textReceipt,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PrintService.shareViaWhatsApp(context, textReceipt)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kirim WA")
                }
            },
            dismissButton = {
                TextButton(onClick = { completedSale = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun ProductRowCard(
    product: Product,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
            .testTag("product_card_${product.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Avatar placeholder
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Emerald50),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.name.take(2).uppercase(),
                    color = Emerald600,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (product.productKind == ProductKind.KNS) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Amber600.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Titipan",
                                color = Amber600,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SKU: ${product.sku} • Stok: ${product.currentStock} ${product.baseUomId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.currentStock <= product.minStockAlert) ErrorRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Money.formatRupiah(product.retailPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Emerald600
                )
            }

            IconButton(
                onClick = onAddToCart,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Emerald600)
                    .size(36.dp)
                    .testTag("add_btn_${product.id}")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CheckoutDialog(
    cartSubtotal: Long,
    customers: List<Contact>,
    onDismiss: () -> Unit,
    onConfirmCheckout: (PaymentMethod, String?, Long, Long, Long, Boolean) -> Unit,
    errorMessage: String? = null
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
    var isTempo by remember { mutableStateOf(false) }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var packagingFeeText by remember { mutableStateOf("") }
    var deliveryFeeText by remember { mutableStateOf("") }
    var cashPaidText by remember { mutableStateOf("") }

    val packagingFee = packagingFeeText.toLongOrNull() ?: 0L
    val deliveryFee = deliveryFeeText.toLongOrNull() ?: 0L
    val rawTotal = cartSubtotal + packagingFee + deliveryFee
    val (roundedTotal, roundingDiff) = if (selectedMethod == PaymentMethod.TUNAI) {
        CashRounding.round500(rawTotal)
    } else {
        Pair(rawTotal, 0L)
    }

    val cashPaid = cashPaidText.toLongOrNull() ?: roundedTotal
    val change = if (cashPaid > roundedTotal && selectedMethod == PaymentMethod.TUNAI && !isTempo) {
        cashPaid - roundedTotal
    } else {
        0L
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Checkout & Pembayaran", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    // Method Selector
                    Text("Metode Pembayaran:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMethod.values().forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method && !isTempo,
                                onClick = {
                                    selectedMethod = method
                                    isTempo = false
                                },
                                label = { Text(method.name, fontSize = 11.sp) }
                            )
                        }
                        FilterChip(
                            selected = isTempo,
                            onClick = { isTempo = true },
                            label = { Text("TEMPO (BON)", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Customer Selection
                    Text("Pelanggan:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = selectedCustomerId == null,
                            onClick = { selectedCustomerId = null },
                            label = { Text("Pelanggan Umum", fontSize = 11.sp) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        customers.forEach { c ->
                            FilterChip(
                                selected = selectedCustomerId == c.contactId,
                                onClick = { selectedCustomerId = c.contactId },
                                label = { Text(c.displayName.take(12), fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fees (packaging / ongkir) as per SAL-R06
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = packagingFeeText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) packagingFeeText = it },
                            label = { Text("Biaya Kemas", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deliveryFeeText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) deliveryFeeText = it },
                            label = { Text("Ongkir", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Total & Rounding Breakdown
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", fontSize = 12.sp)
                                Text(Money.formatRupiah(cartSubtotal), fontSize = 12.sp)
                            }
                            if (roundingDiff != 0L) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Pembulatan Tunai Rp 500:", fontSize = 12.sp)
                                    Text(Money.formatRupiah(roundingDiff), fontSize = 12.sp, color = Amber600)
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Akhir:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(Money.formatRupiah(roundedTotal), fontWeight = FontWeight.Bold, color = Emerald600, fontSize = 15.sp)
                            }
                        }
                    }

                    // Cash Received & Change for Cash
                    if (selectedMethod == PaymentMethod.TUNAI && !isTempo) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = cashPaidText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) cashPaidText = it },
                            label = { Text("Uang Tunai Diterima (Rp)") },
                            placeholder = { Text(Money.formatRupiah(roundedTotal)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Quick tender buttons
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { cashPaidText = "$roundedTotal" }, modifier = Modifier.weight(1f)) {
                                Text("Uang Pas", fontSize = 10.sp)
                            }
                            OutlinedButton(onClick = { cashPaidText = "50000" }, modifier = Modifier.weight(1f)) {
                                Text("50rb", fontSize = 10.sp)
                            }
                            OutlinedButton(onClick = { cashPaidText = "100000" }, modifier = Modifier.weight(1f)) {
                                Text("100rb", fontSize = 10.sp)
                            }
                        }
                        if (change > 0L) {
                            Text(
                                "Kembalian: ${Money.formatRupiah(change)}",
                                fontWeight = FontWeight.Bold,
                                color = Emerald600,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmCheckout(
                        selectedMethod,
                        selectedCustomerId,
                        packagingFee,
                        deliveryFee,
                        cashPaid,
                        isTempo
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                modifier = Modifier.testTag("confirm_checkout_button")
            ) {
                Text("Simpan & Selesai")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
