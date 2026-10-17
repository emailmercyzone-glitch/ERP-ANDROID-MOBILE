package com.example.domain.model

data class Product(
    val id: String,
    val sku: String,
    val name: String,
    val barcode: String?,
    val categoryId: String,
    val productKind: ProductKind,
    val baseUomId: String,
    val consignorId: String? = null,
    val consignorCommissionRateBps: Long = 1000L, // 10.00% default
    val drivePhotoId: String? = null,
    val minStockAlert: Long = 5L,
    val isDeleted: Boolean = false,
    val productStatus: ProductStatus = ProductStatus.ACTIVE,
    val retailPrice: Long = 0L,
    val currentStock: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProductUom(
    val id: String,
    val productId: String,
    val uomName: String,
    val conversionMultiplier: Long, // e.g. 1 Dus = 24 Pcs
    val isSellable: Boolean = true
)

data class ProductPrice(
    val priceId: String,
    val productId: String,
    val uomName: String,
    val minQty: Long, // 0 for retail, > 0 for wholesale tier
    val sellPrice: Long,
    val validFrom: Long = 0L,
    val validTo: Long = Long.MAX_VALUE,
    val isActive: Boolean = true
)

data class B2bContract(
    val contractId: String,
    val customerId: String,
    val productId: String,
    val contractPrice: Long,
    val validFrom: Long = 0L,
    val validTo: Long = Long.MAX_VALUE,
    val isActive: Boolean = true
)

data class Promo(
    val promoId: String,
    val productId: String,
    val promoPrice: Long,
    val minQty: Long = 0L,
    val minSpend: Long = 0L,
    val startAt: Long = 0L,
    val endAt: Long = Long.MAX_VALUE,
    val isActive: Boolean = true
)

data class StockLayer(
    val layerId: String,
    val productId: String,
    val sourceDocumentId: String,
    val receivedAt: Long,
    val initialQty: Long,
    val remainingQty: Long,
    val unitCost: Long, // cost per base unit in Rupiah
    val ownershipKind: ProductKind = ProductKind.EQT,
    val expiryDate: Long? = null
)

data class StockMovement(
    val movementId: String,
    val productId: String,
    val movementType: MovementType,
    val qtyChange: Long,
    val balanceAfter: Long,
    val refDocumentId: String,
    val actorId: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class FifoConsumption(
    val consumptionId: String,
    val saleId: String,
    val productId: String,
    val layerId: String,
    val qtyTaken: Long,
    val unitCost: Long,
    val cogsAmount: Long
)

data class CartItem(
    val product: Product,
    val uomName: String = product.baseUomId,
    val multiplier: Long = 1L,
    val qty: Long = 1L, // in selected UoM
    val unitPrice: Long = product.retailPrice,
    val priceSource: PriceSource = PriceSource.RETAIL
) {
    val baseQty: Long get() = Math.multiplyExact(qty, multiplier)
    val lineTotal: Long get() = Math.multiplyExact(qty, unitPrice)
}

data class Sale(
    val saleId: String,
    val shiftId: String,
    val cashierId: String,
    val customerId: String? = null,
    val subtotal: Long,
    val packagingFee: Long = 0L,
    val deliveryFee: Long = 0L,
    val roundingDiff: Long = 0L,
    val grandTotal: Long,
    val paidCash: Long = 0L,
    val paidNonCash: Long = 0L,
    val changeReturned: Long = 0L,
    val isCreditSale: Boolean = false,
    val saleStatus: String = "COMPLETED",
    val checkoutRequestId: String,
    val voidReason: String? = null,
    val voidedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val items: List<SaleItem> = emptyList()
)

data class SaleItem(
    val itemId: String,
    val saleId: String,
    val productId: String,
    val productName: String,
    val uomName: String,
    val qty: Long,
    val baseQty: Long,
    val unitPrice: Long,
    val priceSource: PriceSource,
    val lineTotal: Long,
    val cogsTotal: Long
)

data class JournalLine(
    val accountCode: String,
    val accountName: String,
    val debit: Long = 0L,
    val credit: Long = 0L
)

data class JournalEntry(
    val journalId: String,
    val sourceDocumentId: String,
    val sourceModule: String,
    val transactionDate: Long = System.currentTimeMillis(),
    val memo: String,
    val isReversed: Boolean = false,
    val reversalOfJournalId: String? = null,
    val reversalReason: String? = null,
    val lines: List<JournalLine> = emptyList()
) {
    init {
        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }
        require(totalDebit == totalCredit) {
            "Debit must equal Credit in JournalEntry! Debit=$totalDebit, Credit=$totalCredit (memo: $memo)"
        }
    }
}

data class Contact(
    val contactId: String,
    val displayName: String,
    val phoneNote: String? = null,
    val contactType: String, // CUSTOMER | SUPPLIER | CONSIGNOR
    val isCreditEligible: Boolean = false,
    val creditLimit: Long = 500000L,
    val creditTermDays: Long = 14L,
    val currentBalance: Long = 0L,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class CashierShift(
    val shiftId: String,
    val deviceId: String,
    val cashierId: String,
    val cashierName: String,
    val startingCash: Long,
    val expectedCash: Long,
    val actualCash: Long? = null,
    val varianceNote: String? = null,
    val shiftStatus: ShiftStatus = ShiftStatus.OPEN,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null
)

data class Expense(
    val expenseId: String,
    val category: ExpenseCategory,
    val amount: Long,
    val merchantName: String? = null,
    val note: String? = null,
    val receiptPhotoId: String? = null,
    val expenseStatus: String = "APPROVED",
    val approvedBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class User(
    val userId: String,
    val displayName: String,
    val role: Role,
    val pinHash: String,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DashboardKpis(
    val omzet: Long,
    val labaKotor: Long?, // masked null for Kasir
    val marginPercent: Double?, // masked null for Kasir
    val totalTransactions: Long,
    val averageBasket: Long,
    val kasBank: Long?, // masked null for Kasir
    val piutangBerjalan: Long?, // masked null for Kasir
    val hutangBerjalan: Long?, // masked null for Kasir
    val nilaiPersediaan: Long?, // masked null for Kasir
    val criticalStockCount: Int,
    val topProducts: List<Pair<String, Long>> = emptyList()
)
