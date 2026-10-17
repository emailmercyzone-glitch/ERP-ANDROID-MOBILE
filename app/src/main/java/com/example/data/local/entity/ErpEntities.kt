package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val role: String, // OWNER | KASIR
    val pinHash: String,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["barcode"], unique = false)
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val name: String,
    val barcode: String? = null,
    val categoryId: String,
    val productKind: String, // EQT | KNS | AFL | VRT
    val baseUomId: String,
    val consignorId: String? = null,
    val consignorCommissionRateBps: Long = 1000L,
    val drivePhotoId: String? = null,
    val minStockAlert: Long = 5L,
    val isDeleted: Boolean = false,
    val productStatus: String = "ACTIVE",
    val retailPrice: Long = 0L,
    val currentStock: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "product_uoms")
data class ProductUomEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val uomName: String,
    val conversionMultiplier: Long,
    val isSellable: Boolean = true
)

@Entity(tableName = "product_prices")
data class ProductPriceEntity(
    @PrimaryKey val priceId: String,
    val productId: String,
    val uomName: String,
    val minQty: Long,
    val sellPrice: Long,
    val validFrom: Long = 0L,
    val validTo: Long = Long.MAX_VALUE,
    val isActive: Boolean = true
)

@Entity(tableName = "b2b_contracts")
data class B2bContractEntity(
    @PrimaryKey val contractId: String,
    val customerId: String,
    val productId: String,
    val contractPrice: Long,
    val validFrom: Long = 0L,
    val validTo: Long = Long.MAX_VALUE,
    val isActive: Boolean = true
)

@Entity(tableName = "promos")
data class PromoEntity(
    @PrimaryKey val promoId: String,
    val productId: String,
    val promoPrice: Long,
    val minQty: Long = 0L,
    val minSpend: Long = 0L,
    val startAt: Long = 0L,
    val endAt: Long = Long.MAX_VALUE,
    val isActive: Boolean = true
)

@Entity(
    tableName = "stock_layers",
    indices = [Index(value = ["productId", "receivedAt"])]
)
data class StockLayerEntity(
    @PrimaryKey val layerId: String,
    val productId: String,
    val sourceDocumentId: String,
    val receivedAt: Long,
    val initialQty: Long,
    val remainingQty: Long,
    val unitCost: Long,
    val ownershipKind: String = "EQT",
    val expiryDate: Long? = null
)

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["productId", "createdAt"])]
)
data class StockMovementEntity(
    @PrimaryKey val movementId: String,
    val productId: String,
    val movementType: String,
    val qtyChange: Long,
    val balanceAfter: Long,
    val refDocumentId: String,
    val actorId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "fifo_consumptions")
data class FifoConsumptionEntity(
    @PrimaryKey val consumptionId: String,
    val saleId: String,
    val productId: String,
    val layerId: String,
    val qtyTaken: Long,
    val unitCost: Long,
    val cogsAmount: Long
)

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["checkoutRequestId"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class SaleEntity(
    @PrimaryKey val saleId: String,
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
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey val itemId: String,
    val saleId: String,
    val productId: String,
    val productName: String,
    val uomName: String,
    val qty: Long,
    val baseQty: Long,
    val unitPrice: Long,
    val priceSource: String,
    val lineTotal: Long,
    val cogsTotal: Long
)

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["transactionDate"])]
)
data class JournalEntryEntity(
    @PrimaryKey val journalId: String,
    val sourceDocumentId: String,
    val sourceModule: String,
    val transactionDate: Long = System.currentTimeMillis(),
    val memo: String,
    val isReversed: Boolean = false,
    val reversalOfJournalId: String? = null,
    val reversalReason: String? = null
)

@Entity(
    tableName = "journal_lines",
    indices = [Index(value = ["journalId", "accountCode"])]
)
data class JournalLineEntity(
    @PrimaryKey val lineId: String,
    val journalId: String,
    val accountCode: String,
    val accountName: String,
    val debit: Long = 0L,
    val credit: Long = 0L
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val contactId: String,
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

@Entity(tableName = "cashier_shifts")
data class CashierShiftEntity(
    @PrimaryKey val shiftId: String,
    val deviceId: String,
    val cashierId: String,
    val cashierName: String,
    val startingCash: Long,
    val expectedCash: Long,
    val actualCash: Long? = null,
    val varianceNote: String? = null,
    val shiftStatus: String = "OPEN",
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val expenseId: String,
    val category: String,
    val amount: Long,
    val merchantName: String? = null,
    val note: String? = null,
    val receiptPhotoId: String? = null,
    val expenseStatus: String = "APPROVED",
    val approvedBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "business_configurations",
    indices = [Index(value = ["configKey", "revision"], unique = true)]
)
data class BusinessConfigEntity(
    @PrimaryKey val configurationId: String,
    val configKey: String,
    val valueCanonical: String,
    val revision: Long,
    val effectiveFrom: Long,
    val effectiveTo: Long? = null,
    val status: String = "ACTIVE",
    val updatedBy: String = "OWNER",
    val changeReason: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "number_sequences",
    primaryKeys = ["deviceId", "docType", "sequenceDate"]
)
data class NumberSequenceEntity(
    val deviceId: String,
    val docType: String,
    val sequenceDate: String, // YYMMDD
    val lastCounter: Long
)

@Entity(tableName = "system_events")
data class SystemEventEntity(
    @PrimaryKey val eventId: String,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
    val actorId: String,
    val deviceId: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val payloadJson: String = "{}"
)
