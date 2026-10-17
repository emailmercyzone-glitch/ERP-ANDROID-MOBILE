package com.example.domain.repository

import com.example.domain.model.B2bContract
import com.example.domain.model.CartItem
import com.example.domain.model.CashierShift
import com.example.domain.model.Contact
import com.example.domain.model.DashboardKpis
import com.example.domain.model.Expense
import com.example.domain.model.JournalEntry
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Product
import com.example.domain.model.ProductPrice
import com.example.domain.model.ProductUom
import com.example.domain.model.Promo
import com.example.domain.model.Role
import com.example.domain.model.Sale
import com.example.domain.model.StockLayer
import com.example.domain.model.StockMovement
import com.example.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    fun getProductById(productId: String): Flow<Product?>
    fun getUomsForProduct(productId: String): Flow<List<ProductUom>>
    fun getPricesForProduct(productId: String): Flow<List<ProductPrice>>
    fun getPromos(): Flow<List<Promo>>
    fun getB2bContracts(): Flow<List<B2bContract>>
    suspend fun insertProduct(product: Product)
    suspend fun updateProduct(product: Product)
    suspend fun insertUom(uom: ProductUom)
}

interface SaleRepository {
    fun getAllSales(): Flow<List<Sale>>
    fun getSaleById(saleId: String): Flow<Sale?>
    suspend fun executeSaleCheckout(
        shiftId: String,
        cashierId: String,
        customerId: String?,
        cartItems: List<CartItem>,
        paidCash: Long,
        paidQris: Long,
        paidTransfer: Long,
        packagingFee: Long,
        deliveryFee: Long,
        roundingDiff: Long,
        isCreditSale: Boolean,
        checkoutRequestId: String
    ): Result<Sale>
    suspend fun voidSale(saleId: String, reason: String, ownerId: String): Result<Unit>
}

interface InventoryRepository {
    fun getStockLayers(productId: String): Flow<List<StockLayer>>
    fun getStockMovements(productId: String): Flow<List<StockMovement>>
    fun getAllMovements(): Flow<List<StockMovement>>
    suspend fun recordStockAdjustment(
        productId: String,
        actualPhysicalQty: Long,
        reason: String,
        actorId: String
    ): Result<Unit>
    suspend fun recordGoodsReceipt(
        supplierId: String,
        productId: String,
        qty: Long,
        unitCost: Long,
        isPaidCash: Boolean,
        actorId: String
    ): Result<Unit>
}

interface AccountingRepository {
    fun getAllJournalEntries(): Flow<List<JournalEntry>>
    fun getJournalEntryById(journalId: String): Flow<JournalEntry?>
    suspend fun recordManualPrive(amount: Long, note: String, actorId: String): Result<JournalEntry>
    suspend fun calculateKpis(forRole: Role): DashboardKpis
}

interface ShiftRepository {
    fun getActiveShift(cashierId: String): Flow<CashierShift?>
    fun getAllShifts(): Flow<List<CashierShift>>
    suspend fun openShift(cashierId: String, cashierName: String, startingCash: Long, deviceId: String): Result<CashierShift>
    suspend fun closeShift(shiftId: String, actualCash: Long, varianceNote: String?): Result<CashierShift>
}

interface ContactRepository {
    fun getAllContacts(): Flow<List<Contact>>
    fun getCustomers(): Flow<List<Contact>>
    fun getSuppliers(): Flow<List<Contact>>
    suspend fun insertContact(contact: Contact)
    suspend fun recordReceivablePayment(customerId: String, amount: Long, method: PaymentMethod, note: String?): Result<Unit>
    suspend fun recordPayablePayment(supplierId: String, amount: Long, method: PaymentMethod, isConsignment: Boolean, note: String?): Result<Unit>
}

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<Expense>>
    suspend fun recordExpense(expense: Expense): Result<Unit>
}

interface ConfigRepository {
    fun getConfigValue(key: String): Flow<String?>
    suspend fun setConfigValue(key: String, value: String, reason: String, updatedBy: String): Result<Unit>
}

interface AuthRepository {
    fun getAllUsers(): Flow<List<User>>
    fun getUserById(userId: String): Flow<User?>
    suspend fun authenticatePin(userId: String, pin: String): Result<Boolean>
    suspend fun recordFailedAttempt(userId: String): Int
    suspend fun resetFailedAttempts(userId: String)
}
