package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.ErpDatabase
import com.example.data.local.entity.B2bContractEntity
import com.example.data.local.entity.BusinessConfigEntity
import com.example.data.local.entity.CashierShiftEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.FifoConsumptionEntity
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.local.entity.JournalLineEntity
import com.example.data.local.entity.NumberSequenceEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProductPriceEntity
import com.example.data.local.entity.ProductUomEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.StockLayerEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.SystemEventEntity
import com.example.data.local.entity.UserEntity
import com.example.domain.accounting.AccountingEngine
import com.example.domain.auth.AuthPolicy
import com.example.domain.inventory.FifoEngine
import com.example.domain.model.B2bContract
import com.example.domain.model.CartItem
import com.example.domain.model.CashierShift
import com.example.domain.model.Contact
import com.example.domain.model.DashboardKpis
import com.example.domain.model.Expense
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.FifoConsumption
import com.example.domain.model.JournalEntry
import com.example.domain.model.JournalLine
import com.example.domain.model.MovementType
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PriceSource
import com.example.domain.model.Product
import com.example.domain.model.ProductKind
import com.example.domain.model.ProductPrice
import com.example.domain.model.ProductStatus
import com.example.domain.model.ProductUom
import com.example.domain.model.Promo
import com.example.domain.model.Role
import com.example.domain.model.Sale
import com.example.domain.model.SaleItem
import com.example.domain.model.ShiftStatus
import com.example.domain.model.StockLayer
import com.example.domain.model.StockMovement
import com.example.domain.model.User
import com.example.domain.repository.AccountingRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ConfigRepository
import com.example.domain.repository.ContactRepository
import com.example.domain.repository.ExpenseRepository
import com.example.domain.repository.InventoryRepository
import com.example.domain.repository.ProductRepository
import com.example.domain.repository.SaleRepository
import com.example.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ErpRepositoryImpl(private val db: ErpDatabase) :
    ProductRepository,
    SaleRepository,
    InventoryRepository,
    AccountingRepository,
    ShiftRepository,
    ContactRepository,
    ExpenseRepository,
    ConfigRepository,
    AuthRepository {

    // --- PRODUCT REPOSITORY ---
    override fun getAllProducts(): Flow<List<Product>> {
        return db.productDao().getAllProducts().map { list -> list.map { it.toProductDomain() } }
    }

    override fun getProductById(productId: String): Flow<Product?> {
        return db.productDao().getProductById(productId).map { it?.toProductDomain() }
    }

    override fun getUomsForProduct(productId: String): Flow<List<ProductUom>> {
        return db.productDao().getUomsForProduct(productId).map { list ->
            list.map { ProductUom(it.id, it.productId, it.uomName, it.conversionMultiplier, it.isSellable) }
        }
    }

    override fun getPricesForProduct(productId: String): Flow<List<ProductPrice>> {
        return db.productDao().getPricesForProduct(productId).map { list ->
            list.map { ProductPrice(it.priceId, it.productId, it.uomName, it.minQty, it.sellPrice, it.validFrom, it.validTo, it.isActive) }
        }
    }

    override fun getPromos(): Flow<List<Promo>> {
        return db.productDao().getActivePromos().map { list ->
            list.map { Promo(it.promoId, it.productId, it.promoPrice, it.minQty, it.minSpend, it.startAt, it.endAt, it.isActive) }
        }
    }

    override fun getB2bContracts(): Flow<List<B2bContract>> {
        return db.productDao().getActiveB2bContracts().map { list ->
            list.map { B2bContract(it.contractId, it.customerId, it.productId, it.contractPrice, it.validFrom, it.validTo, it.isActive) }
        }
    }

    override suspend fun insertProduct(product: Product) {
        db.productDao().insertProduct(product.toProductEntity())
    }

    override suspend fun updateProduct(product: Product) {
        db.productDao().updateProduct(product.toProductEntity())
    }

    override suspend fun insertUom(uom: ProductUom) {
        db.productDao().insertUom(ProductUomEntity(uom.id, uom.productId, uom.uomName, uom.conversionMultiplier, uom.isSellable))
    }

    // --- SALE REPOSITORY ---
    override fun getAllSales(): Flow<List<Sale>> {
        return db.saleDao().getAllSales().map { list -> list.map { it.toSaleDomain() } }
    }

    override fun getSaleById(saleId: String): Flow<Sale?> {
        return db.saleDao().getSaleById(saleId).map { it?.toSaleDomain() }
    }

    private suspend fun generateSequenceNumber(deviceId: String, docType: String): String {
        val dateStr = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val existing = db.numberSequenceDao().getSequence(deviceId, docType, dateStr)
        val nextCounter = (existing?.lastCounter ?: 0L) + 1L
        db.numberSequenceDao().insertOrUpdate(NumberSequenceEntity(deviceId, docType, dateStr, nextCounter))
        val urut = "%04d".format(nextCounter)
        return "$deviceId-$docType-$dateStr-$urut"
    }

    override suspend fun executeSaleCheckout(
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
    ): Result<Sale> = runCatching {
        val existing = db.saleDao().getSaleByCheckoutRequestId(checkoutRequestId)
        if (existing != null) {
            val items = db.saleDao().getSaleItemsForSale(existing.saleId).map { it.toSaleItemDomain() }
            return@runCatching existing.toSaleDomain().copy(items = items)
        }

        require(cartItems.isNotEmpty()) { "Keranjang belanja tidak boleh kosong!" }

        db.withTransaction {
            val now = System.currentTimeMillis()
            val saleId = generateSequenceNumber("K1", "JUAL")
            val journalId = generateSequenceNumber("K1", "JRN")

            var totalCogs = 0L
            var equitySalesRevenue = 0L
            var consignorPayable = 0L
            var consignmentCommission = 0L
            val saleItemsToInsert = mutableListOf<SaleItemEntity>()

            for (item in cartItems) {
                val product = item.product
                val baseQty = item.baseQty
                val activeLayers = db.stockDao().getActiveLayersForProductSync(product.id).map { it.toStockLayerDomain() }

                val fifoResult = FifoEngine.consume(
                    saleId = saleId,
                    productId = product.id,
                    productKind = product.productKind,
                    productName = product.name,
                    requestedBaseQty = baseQty,
                    availableLayers = activeLayers,
                    currentTime = now
                )

                fifoResult.updatedLayers.forEach {
                    db.stockDao().updateLayer(it.toStockLayerEntity())
                }

                db.stockDao().insertConsumptions(fifoResult.consumptions.map { it.toFifoConsumptionEntity() })

                val newStock = product.currentStock - baseQty
                db.stockDao().insertMovement(
                    StockMovementEntity(
                        movementId = "MOV-K1-${now}-${product.id.takeLast(4)}",
                        productId = product.id,
                        movementType = MovementType.OUT_SALE.name,
                        qtyChange = -baseQty,
                        balanceAfter = newStock,
                        refDocumentId = saleId,
                        actorId = cashierId,
                        createdAt = now
                    )
                )

                db.productDao().updateProduct(product.toProductEntity().copy(currentStock = newStock))

                if (product.productKind == ProductKind.EQT) {
                    totalCogs += fifoResult.totalCogs
                    equitySalesRevenue += item.lineTotal
                } else if (product.productKind == ProductKind.KNS) {
                    val commission = (item.lineTotal * product.consignorCommissionRateBps) / 10000L
                    consignmentCommission += commission
                    consignorPayable += (item.lineTotal - commission)
                }

                saleItemsToInsert.add(
                    SaleItemEntity(
                        itemId = "ITM-${saleId}-${item.product.id}",
                        saleId = saleId,
                        productId = product.id,
                        productName = product.name,
                        uomName = item.uomName,
                        qty = item.qty,
                        baseQty = baseQty,
                        unitPrice = item.unitPrice,
                        priceSource = item.priceSource.name,
                        lineTotal = item.lineTotal,
                        cogsTotal = fifoResult.totalCogs
                    )
                )
            }

            val subtotal = cartItems.sumOf { it.lineTotal }
            val extraFees = packagingFee + deliveryFee
            val grandTotal = subtotal + extraFees + roundingDiff
            val receivableAmount = if (isCreditSale) grandTotal else 0L

            val journal = AccountingEngine.createSaleJournal(
                journalId = journalId,
                saleId = saleId,
                paidCash = paidCash,
                paidQris = paidQris,
                paidTransfer = paidTransfer,
                receivableAmount = receivableAmount,
                cogsTotal = totalCogs,
                equitySalesRevenue = equitySalesRevenue,
                consignorPayable = consignorPayable,
                consignmentCommission = consignmentCommission,
                packagingAndDeliveryFee = extraFees,
                roundingDiff = roundingDiff,
                timestamp = now
            )

            db.journalDao().insertJournalEntry(
                JournalEntryEntity(
                    journalId = journal.journalId,
                    sourceDocumentId = journal.sourceDocumentId,
                    sourceModule = journal.sourceModule,
                    transactionDate = journal.transactionDate,
                    memo = journal.memo
                )
            )
            db.journalDao().insertJournalLines(
                journal.lines.map {
                    JournalLineEntity(
                        lineId = "LNE-${journal.journalId}-${it.accountCode}",
                        journalId = journal.journalId,
                        accountCode = it.accountCode,
                        accountName = it.accountName,
                        debit = it.debit,
                        credit = it.credit
                    )
                }
            )

            val changeReturned = if (paidCash > grandTotal && !isCreditSale) paidCash - grandTotal else 0L
            val saleEntity = SaleEntity(
                saleId = saleId,
                shiftId = shiftId,
                cashierId = cashierId,
                customerId = customerId,
                subtotal = subtotal,
                packagingFee = packagingFee,
                deliveryFee = deliveryFee,
                roundingDiff = roundingDiff,
                grandTotal = grandTotal,
                paidCash = paidCash,
                paidNonCash = paidQris + paidTransfer,
                changeReturned = changeReturned,
                isCreditSale = isCreditSale,
                saleStatus = "COMPLETED",
                checkoutRequestId = checkoutRequestId,
                createdAt = now
            )
            db.saleDao().insertSale(saleEntity)
            db.saleDao().insertSaleItems(saleItemsToInsert)

            val activeShift = db.shiftDao().getActiveShiftSync(cashierId)
            if (activeShift != null && paidCash > 0L) {
                val netCashFromSale = if (paidCash > grandTotal) grandTotal else paidCash
                db.shiftDao().updateShift(activeShift.copy(expectedCash = activeShift.expectedCash + netCashFromSale))
            }

            if (isCreditSale && customerId != null) {
                val contact = db.contactDao().getContactByIdSync(customerId)
                if (contact != null) {
                    db.contactDao().updateContact(contact.copy(currentBalance = contact.currentBalance + grandTotal))
                }
            }

            db.systemEventDao().insertEvent(
                SystemEventEntity(
                    eventId = "EVT-${now}-SALE",
                    eventType = "SALE_COMMITTED",
                    aggregateType = "SALE",
                    aggregateId = saleId,
                    actorId = cashierId,
                    deviceId = "K1",
                    occurredAt = now,
                    payloadJson = "{\"grandTotal\":$grandTotal,\"itemsCount\":${cartItems.size}}"
                )
            )

            saleEntity.toSaleDomain().copy(items = saleItemsToInsert.map { it.toSaleItemDomain() })
        }
    }

    override suspend fun voidSale(saleId: String, reason: String, ownerId: String): Result<Unit> = runCatching {
        db.withTransaction {
            val sale = db.saleDao().getSaleByIdSync(saleId) ?: error("Sale #$saleId not found")
            require(sale.saleStatus != "VOIDED") { "Nota sudah dibatalkan sebelumnya!" }

            val now = System.currentTimeMillis()
            val reversalJournalId = generateSequenceNumber("K1", "JRN")

            db.saleDao().updateSale(sale.copy(saleStatus = "VOIDED", voidReason = reason, voidedBy = ownerId))

            val consumptions = db.stockDao().getConsumptionsForSale(saleId)
            for (c in consumptions) {
                val layers = db.stockDao().getActiveLayersForProductSync(c.productId)
                val targetLayer = layers.firstOrNull { it.layerId == c.layerId }
                if (targetLayer != null) {
                    db.stockDao().updateLayer(targetLayer.copy(remainingQty = targetLayer.remainingQty + c.qtyTaken))
                }
                val product = db.productDao().getProductByIdSync(c.productId)
                if (product != null) {
                    val restoredStock = product.currentStock + c.qtyTaken
                    db.productDao().updateProduct(product.copy(currentStock = restoredStock))
                    db.stockDao().insertMovement(
                        StockMovementEntity(
                            movementId = "MOV-K1-${now}-VOID",
                            productId = c.productId,
                            movementType = MovementType.IN_RETURN_SALE.name,
                            qtyChange = c.qtyTaken,
                            balanceAfter = restoredStock,
                            refDocumentId = saleId,
                            actorId = ownerId,
                            createdAt = now
                        )
                    )
                }
            }

            val reversalLines = listOf(
                JournalLineEntity("LNE-REV-1", reversalJournalId, AccountingEngine.ACC_PENDAPATAN_JUAL, AccountingEngine.accountName(AccountingEngine.ACC_PENDAPATAN_JUAL), debit = sale.grandTotal, credit = 0L),
                JournalLineEntity("LNE-REV-2", reversalJournalId, AccountingEngine.ACC_KAS_TOKO, AccountingEngine.accountName(AccountingEngine.ACC_KAS_TOKO), debit = 0L, credit = sale.grandTotal)
            )
            db.journalDao().insertJournalEntry(
                JournalEntryEntity(
                    journalId = reversalJournalId,
                    sourceDocumentId = saleId,
                    sourceModule = "SALES",
                    transactionDate = now,
                    memo = "Void Nota #$saleId ($reason)",
                    isReversed = true,
                    reversalOfJournalId = saleId,
                    reversalReason = reason
                )
            )
            db.journalDao().insertJournalLines(reversalLines)
        }
    }

    // --- INVENTORY REPOSITORY ---
    override fun getStockLayers(productId: String): Flow<List<StockLayer>> {
        return db.stockDao().getActiveLayersForProduct(productId).map { list -> list.map { it.toStockLayerDomain() } }
    }

    override fun getStockMovements(productId: String): Flow<List<StockMovement>> {
        return db.stockDao().getMovementsForProduct(productId).map { list -> list.map { it.toStockMovementDomain() } }
    }

    override fun getAllMovements(): Flow<List<StockMovement>> {
        return db.stockDao().getAllMovements().map { list -> list.map { it.toStockMovementDomain() } }
    }

    override suspend fun recordStockAdjustment(
        productId: String,
        actualPhysicalQty: Long,
        reason: String,
        actorId: String
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val product = db.productDao().getProductByIdSync(productId) ?: error("Product not found")
            val diff = actualPhysicalQty - product.currentStock
            if (diff == 0L) return@withTransaction

            val now = System.currentTimeMillis()
            db.productDao().updateProduct(product.copy(currentStock = actualPhysicalQty))
            db.stockDao().insertMovement(
                StockMovementEntity(
                    movementId = "MOV-ADJ-${now}",
                    productId = productId,
                    movementType = MovementType.ADJUST_COUNT.name,
                    qtyChange = diff,
                    balanceAfter = actualPhysicalQty,
                    refDocumentId = "ADJUST-$now",
                    actorId = actorId,
                    createdAt = now
                )
            )
        }
    }

    override suspend fun recordGoodsReceipt(
        supplierId: String,
        productId: String,
        qty: Long,
        unitCost: Long,
        isPaidCash: Boolean,
        actorId: String
    ): Result<Unit> = runCatching {
        require(qty > 0L) { "Kuantitas barang masuk harus > 0" }
        require(unitCost > 0L) { "Harga beli modal harus > 0" }

        db.withTransaction {
            val now = System.currentTimeMillis()
            val grnId = generateSequenceNumber("BO", "GRN")
            val journalId = generateSequenceNumber("BO", "JRN")
            val product = db.productDao().getProductByIdSync(productId) ?: error("Product not found")

            val layerId = "L-${productId}-${now}"
            db.stockDao().insertLayer(
                StockLayerEntity(
                    layerId = layerId,
                    productId = productId,
                    sourceDocumentId = grnId,
                    receivedAt = now,
                    initialQty = qty,
                    remainingQty = qty,
                    unitCost = unitCost,
                    ownershipKind = product.productKind
                )
            )

            val newStock = product.currentStock + qty
            db.productDao().updateProduct(product.copy(currentStock = newStock))

            db.stockDao().insertMovement(
                StockMovementEntity(
                    movementId = "MOV-BO-${now}",
                    productId = productId,
                    movementType = MovementType.IN_RECEIPT.name,
                    qtyChange = qty,
                    balanceAfter = newStock,
                    refDocumentId = grnId,
                    actorId = actorId,
                    createdAt = now
                )
            )

            val totalCost = Math.multiplyExact(qty, unitCost)
            val journal = AccountingEngine.createGrnJournal(
                journalId = journalId,
                grnId = grnId,
                totalInventoryCost = totalCost,
                isPaidCash = isPaidCash,
                timestamp = now
            )
            db.journalDao().insertJournalEntry(
                JournalEntryEntity(
                    journalId = journal.journalId,
                    sourceDocumentId = grnId,
                    sourceModule = "PURCHASING",
                    transactionDate = now,
                    memo = journal.memo
                )
            )
            db.journalDao().insertJournalLines(
                journal.lines.map {
                    JournalLineEntity(
                        lineId = "LNE-${journal.journalId}-${it.accountCode}",
                        journalId = journal.journalId,
                        accountCode = it.accountCode,
                        accountName = it.accountName,
                        debit = it.debit,
                        credit = it.credit
                    )
                }
            )

            if (!isPaidCash) {
                val supplier = db.contactDao().getContactByIdSync(supplierId)
                if (supplier != null) {
                    db.contactDao().updateContact(supplier.copy(currentBalance = supplier.currentBalance + totalCost))
                }
            }
        }
    }

    // --- ACCOUNTING REPOSITORY ---
    override fun getAllJournalEntries(): Flow<List<JournalEntry>> {
        return db.journalDao().getAllJournalEntries().map { entries: List<JournalEntryEntity> ->
            entries.map { entry: JournalEntryEntity ->
                val lines = db.journalDao().getJournalLinesForEntry(entry.journalId).map { it.toJournalLineDomain() }
                entry.toJournalEntryDomain().copy(lines = lines)
            }
        }
    }

    override fun getJournalEntryById(journalId: String): Flow<JournalEntry?> {
        return db.journalDao().getJournalEntryById(journalId).map { entry: JournalEntryEntity? ->
            if (entry == null) null else {
                val lines = db.journalDao().getJournalLinesForEntry(entry.journalId).map { it.toJournalLineDomain() }
                entry.toJournalEntryDomain().copy(lines = lines)
            }
        }
    }

    override suspend fun recordManualPrive(amount: Long, note: String, actorId: String): Result<JournalEntry> = runCatching {
        require(amount > 0L) { "Nominal prive harus > 0!" }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val journalId = generateSequenceNumber("BO", "JRN")
            val journal = AccountingEngine.createPriveJournal(journalId, amount, note, now)

            db.journalDao().insertJournalEntry(
                JournalEntryEntity(
                    journalId = journal.journalId,
                    sourceDocumentId = "PRIVE-$now",
                    sourceModule = "MANUAL",
                    transactionDate = now,
                    memo = journal.memo
                )
            )
            db.journalDao().insertJournalLines(
                journal.lines.map {
                    JournalLineEntity("LNE-${journal.journalId}-${it.accountCode}", journal.journalId, it.accountCode, it.accountName, it.debit, it.credit)
                }
            )
            journal
        }
    }

    override suspend fun calculateKpis(forRole: Role): DashboardKpis {
        val lines = db.journalDao().getAllLinesSync()

        val omzet = lines.filter { it.accountCode == AccountingEngine.ACC_PENDAPATAN_JUAL }.sumOf { it.credit }
        val hpp = lines.filter { it.accountCode == AccountingEngine.ACC_HPP }.sumOf { it.debit }
        val labaKotor = omzet - hpp
        val marginPercent = if (omzet > 0L) (labaKotor.toDouble() / omzet.toDouble()) * 100.0 else 0.0

        val kasToko = lines.filter { it.accountCode == AccountingEngine.ACC_KAS_TOKO }.sumOf { it.debit - it.credit }
        val bankToko = lines.filter { it.accountCode == AccountingEngine.ACC_BANK_TOKO }.sumOf { it.debit - it.credit }
        val totalKasBank = kasToko + bankToko

        val piutang = lines.filter { it.accountCode == AccountingEngine.ACC_PIUTANG_USAHA }.sumOf { it.debit - it.credit }
        val hutang = lines.filter { it.accountCode == AccountingEngine.ACC_HUTANG_SUPPLIER || it.accountCode == AccountingEngine.ACC_HUTANG_KONSINYASI }.sumOf { it.credit - it.debit }

        val activeLayers = db.stockDao().getAllActiveLayersSync()
        val nilaiPersediaan = activeLayers.filter { it.ownershipKind == "EQT" }.sumOf { it.remainingQty * it.unitCost }

        val isOwner = forRole == Role.OWNER

        return DashboardKpis(
            omzet = omzet,
            labaKotor = if (isOwner) labaKotor else null,
            marginPercent = if (isOwner) marginPercent else null,
            totalTransactions = 1L,
            averageBasket = omzet,
            kasBank = if (isOwner) totalKasBank else null,
            piutangBerjalan = if (isOwner) piutang else null,
            hutangBerjalan = if (isOwner) hutang else null,
            nilaiPersediaan = if (isOwner) nilaiPersediaan else null,
            criticalStockCount = 0
        )
    }

    // --- SHIFT REPOSITORY ---
    override fun getActiveShift(cashierId: String): Flow<CashierShift?> {
        return db.shiftDao().getActiveShift(cashierId).map { it?.toCashierShiftDomain() }
    }

    override fun getAllShifts(): Flow<List<CashierShift>> {
        return db.shiftDao().getAllShifts().map { list -> list.map { it.toCashierShiftDomain() } }
    }

    override suspend fun openShift(
        cashierId: String,
        cashierName: String,
        startingCash: Long,
        deviceId: String
    ): Result<CashierShift> = runCatching {
        val existing = db.shiftDao().getActiveShiftSync(cashierId)
        if (existing != null) return@runCatching existing.toCashierShiftDomain()

        val shiftId = "SHF-${deviceId}-${System.currentTimeMillis()}"
        val shift = CashierShiftEntity(
            shiftId = shiftId,
            deviceId = deviceId,
            cashierId = cashierId,
            cashierName = cashierName,
            startingCash = startingCash,
            expectedCash = startingCash,
            shiftStatus = "OPEN",
            openedAt = System.currentTimeMillis()
        )
        db.shiftDao().insertShift(shift)
        shift.toCashierShiftDomain()
    }

    override suspend fun closeShift(
        shiftId: String,
        actualCash: Long,
        varianceNote: String?
    ): Result<CashierShift> = runCatching {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val shift = CashierShiftEntity(
                shiftId = shiftId,
                deviceId = "K1",
                cashierId = "KSR-01",
                cashierName = "Kasir",
                startingCash = 0L,
                expectedCash = 0L,
                actualCash = actualCash,
                varianceNote = varianceNote,
                shiftStatus = "CLOSED",
                closedAt = now
            )
            db.shiftDao().updateShift(shift)
            shift.toCashierShiftDomain()
        }
    }

    // --- CONTACT REPOSITORY ---
    override fun getAllContacts(): Flow<List<Contact>> {
        return db.contactDao().getAllContacts().map { list -> list.map { it.toContactDomain() } }
    }

    override fun getCustomers(): Flow<List<Contact>> {
        return db.contactDao().getContactsByType("CUSTOMER").map { list -> list.map { it.toContactDomain() } }
    }

    override fun getSuppliers(): Flow<List<Contact>> {
        return db.contactDao().getContactsByType("SUPPLIER").map { list -> list.map { it.toContactDomain() } }
    }

    override suspend fun insertContact(contact: Contact) {
        db.contactDao().insertContact(contact.toContactEntity())
    }

    override suspend fun recordReceivablePayment(
        customerId: String,
        amount: Long,
        method: PaymentMethod,
        note: String?
    ): Result<Unit> = runCatching {
        require(amount > 0L) { "Nominal bayar harus > 0" }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val customer = db.contactDao().getContactByIdSync(customerId) ?: error("Customer not found")
            val paymentId = generateSequenceNumber("K1", "KWT")
            val journalId = generateSequenceNumber("K1", "JRN")

            val journal = AccountingEngine.createReceivablePaymentJournal(
                journalId = journalId,
                paymentId = paymentId,
                amount = amount,
                isBankTransfer = method == PaymentMethod.TRANSFER,
                timestamp = now
            )
            db.journalDao().insertJournalEntry(JournalEntryEntity(journal.journalId, paymentId, "CASH", now, journal.memo))
            db.journalDao().insertJournalLines(journal.lines.map { JournalLineEntity("LNE-${journal.journalId}-${it.accountCode}", journal.journalId, it.accountCode, it.accountName, it.debit, it.credit) })

            val newBalance = maxOf(0L, customer.currentBalance - amount)
            db.contactDao().updateContact(customer.copy(currentBalance = newBalance))
        }
    }

    override suspend fun recordPayablePayment(
        supplierId: String,
        amount: Long,
        method: PaymentMethod,
        isConsignment: Boolean,
        note: String?
    ): Result<Unit> = runCatching {
        require(amount > 0L) { "Nominal bayar harus > 0" }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val supplier = db.contactDao().getContactByIdSync(supplierId) ?: error("Supplier not found")
            val paymentId = generateSequenceNumber("BO", "BYR")
            val journalId = generateSequenceNumber("BO", "JRN")

            val journal = AccountingEngine.createPayablePaymentJournal(
                journalId = journalId,
                paymentId = paymentId,
                amount = amount,
                isBankTransfer = method == PaymentMethod.TRANSFER,
                isConsignment = isConsignment,
                timestamp = now
            )
            db.journalDao().insertJournalEntry(JournalEntryEntity(journal.journalId, paymentId, "CASH", now, journal.memo))
            db.journalDao().insertJournalLines(journal.lines.map { JournalLineEntity("LNE-${journal.journalId}-${it.accountCode}", journal.journalId, it.accountCode, it.accountName, it.debit, it.credit) })

            val newBalance = maxOf(0L, supplier.currentBalance - amount)
            db.contactDao().updateContact(supplier.copy(currentBalance = newBalance))
        }
    }

    // --- EXPENSE REPOSITORY ---
    override fun getAllExpenses(): Flow<List<Expense>> {
        return db.expenseDao().getAllExpenses().map { list -> list.map { it.toExpenseDomain() } }
    }

    override suspend fun recordExpense(expense: Expense): Result<Unit> = runCatching {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val journalId = generateSequenceNumber("BO", "JRN")

            db.expenseDao().insertExpense(expense.toExpenseEntity())

            val journal = AccountingEngine.createExpenseJournal(
                journalId = journalId,
                expenseId = expense.expenseId,
                category = expense.category.displayName,
                accountCode = expense.category.accountCode,
                amount = expense.amount,
                timestamp = now
            )
            db.journalDao().insertJournalEntry(JournalEntryEntity(journal.journalId, expense.expenseId, "CASH", now, journal.memo))
            db.journalDao().insertJournalLines(journal.lines.map { JournalLineEntity("LNE-${journal.journalId}-${it.accountCode}", journal.journalId, it.accountCode, it.accountName, it.debit, it.credit) })
        }
    }

    // --- CONFIG REPOSITORY ---
    override fun getConfigValue(key: String): Flow<String?> {
        return db.configDao().getConfigValue(key)
    }

    override suspend fun setConfigValue(key: String, value: String, reason: String, updatedBy: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val latestRevision = (db.configDao().getLatestRevision(key) ?: 0L) + 1L
        db.configDao().insertConfig(
            BusinessConfigEntity(
                configurationId = "CFG-${key}-${latestRevision}",
                configKey = key,
                valueCanonical = value,
                revision = latestRevision,
                effectiveFrom = now,
                status = "ACTIVE",
                updatedBy = updatedBy,
                changeReason = reason,
                updatedAt = now
            )
        )
    }

    // --- AUTH REPOSITORY ---
    override fun getAllUsers(): Flow<List<User>> {
        return db.userDao().getAllUsers().map { list -> list.map { it.toUserDomain() } }
    }

    override fun getUserById(userId: String): Flow<User?> {
        return db.userDao().getUserById(userId).map { it?.toUserDomain() }
    }

    override suspend fun authenticatePin(userId: String, pin: String): Result<Boolean> = runCatching {
        val user = db.userDao().getUserByIdSync(userId) ?: return@runCatching false
        AuthPolicy.verifyPin(pin, user.pinHash)
    }

    override suspend fun recordFailedAttempt(userId: String): Int = 1

    override suspend fun resetFailedAttempts(userId: String) {}

    // --- EXPLICIT DOMAIN <-> ENTITY MAPPERS ---
    private fun ProductEntity.toProductDomain() = Product(
        id = id,
        sku = sku,
        name = name,
        barcode = barcode,
        categoryId = categoryId,
        productKind = ProductKind.valueOf(productKind),
        baseUomId = baseUomId,
        consignorId = consignorId,
        consignorCommissionRateBps = consignorCommissionRateBps,
        drivePhotoId = drivePhotoId,
        minStockAlert = minStockAlert,
        isDeleted = isDeleted,
        productStatus = ProductStatus.valueOf(productStatus),
        retailPrice = retailPrice,
        currentStock = currentStock,
        updatedAt = updatedAt
    )

    private fun Product.toProductEntity() = ProductEntity(
        id = id,
        sku = sku,
        name = name,
        barcode = barcode,
        categoryId = categoryId,
        productKind = productKind.name,
        baseUomId = baseUomId,
        consignorId = consignorId,
        consignorCommissionRateBps = consignorCommissionRateBps,
        drivePhotoId = drivePhotoId,
        minStockAlert = minStockAlert,
        isDeleted = isDeleted,
        productStatus = productStatus.name,
        retailPrice = retailPrice,
        currentStock = currentStock,
        updatedAt = updatedAt
    )

    private fun StockLayerEntity.toStockLayerDomain() = StockLayer(
        layerId = layerId,
        productId = productId,
        sourceDocumentId = sourceDocumentId,
        receivedAt = receivedAt,
        initialQty = initialQty,
        remainingQty = remainingQty,
        unitCost = unitCost,
        ownershipKind = ProductKind.valueOf(ownershipKind),
        expiryDate = expiryDate
    )

    private fun StockLayer.toStockLayerEntity() = StockLayerEntity(
        layerId = layerId,
        productId = productId,
        sourceDocumentId = sourceDocumentId,
        receivedAt = receivedAt,
        initialQty = initialQty,
        remainingQty = remainingQty,
        unitCost = unitCost,
        ownershipKind = ownershipKind.name,
        expiryDate = expiryDate
    )

    private fun StockMovementEntity.toStockMovementDomain() = StockMovement(
        movementId = movementId,
        productId = productId,
        movementType = MovementType.valueOf(movementType),
        qtyChange = qtyChange,
        balanceAfter = balanceAfter,
        refDocumentId = refDocumentId,
        actorId = actorId,
        createdAt = createdAt
    )

    private fun FifoConsumption.toFifoConsumptionEntity() = FifoConsumptionEntity(
        consumptionId = consumptionId,
        saleId = saleId,
        productId = productId,
        layerId = layerId,
        qtyTaken = qtyTaken,
        unitCost = unitCost,
        cogsAmount = cogsAmount
    )

    private fun SaleEntity.toSaleDomain() = Sale(
        saleId = saleId,
        shiftId = shiftId,
        cashierId = cashierId,
        customerId = customerId,
        subtotal = subtotal,
        packagingFee = packagingFee,
        deliveryFee = deliveryFee,
        roundingDiff = roundingDiff,
        grandTotal = grandTotal,
        paidCash = paidCash,
        paidNonCash = paidNonCash,
        changeReturned = changeReturned,
        isCreditSale = isCreditSale,
        saleStatus = saleStatus,
        checkoutRequestId = checkoutRequestId,
        voidReason = voidReason,
        voidedBy = voidedBy,
        createdAt = createdAt
    )

    private fun SaleItemEntity.toSaleItemDomain() = SaleItem(
        itemId = itemId,
        saleId = saleId,
        productId = productId,
        productName = productName,
        uomName = uomName,
        qty = qty,
        baseQty = baseQty,
        unitPrice = unitPrice,
        priceSource = PriceSource.valueOf(priceSource),
        lineTotal = lineTotal,
        cogsTotal = cogsTotal
    )

    private fun JournalEntryEntity.toJournalEntryDomain() = JournalEntry(
        journalId = journalId,
        sourceDocumentId = sourceDocumentId,
        sourceModule = sourceModule,
        transactionDate = transactionDate,
        memo = memo,
        isReversed = isReversed,
        reversalOfJournalId = reversalOfJournalId,
        reversalReason = reversalReason
    )

    private fun JournalLineEntity.toJournalLineDomain() = JournalLine(
        accountCode = accountCode,
        accountName = accountName,
        debit = debit,
        credit = credit
    )

    private fun ContactEntity.toContactDomain() = Contact(
        contactId = contactId,
        displayName = displayName,
        phoneNote = phoneNote,
        contactType = contactType,
        isCreditEligible = isCreditEligible,
        creditLimit = creditLimit,
        creditTermDays = creditTermDays,
        currentBalance = currentBalance,
        isDeleted = isDeleted,
        updatedAt = updatedAt
    )

    private fun Contact.toContactEntity() = ContactEntity(
        contactId = contactId,
        displayName = displayName,
        phoneNote = phoneNote,
        contactType = contactType,
        isCreditEligible = isCreditEligible,
        creditLimit = creditLimit,
        creditTermDays = creditTermDays,
        currentBalance = currentBalance,
        isDeleted = isDeleted,
        updatedAt = updatedAt
    )

    private fun CashierShiftEntity.toCashierShiftDomain() = CashierShift(
        shiftId = shiftId,
        deviceId = deviceId,
        cashierId = cashierId,
        cashierName = cashierName,
        startingCash = startingCash,
        expectedCash = expectedCash,
        actualCash = actualCash,
        varianceNote = varianceNote,
        shiftStatus = ShiftStatus.valueOf(shiftStatus),
        openedAt = openedAt,
        closedAt = closedAt
    )

    private fun ExpenseEntity.toExpenseDomain() = Expense(
        expenseId = expenseId,
        category = ExpenseCategory.valueOf(category),
        amount = amount,
        merchantName = merchantName,
        note = note,
        receiptPhotoId = receiptPhotoId,
        expenseStatus = expenseStatus,
        approvedBy = approvedBy,
        createdAt = createdAt
    )

    private fun Expense.toExpenseEntity() = ExpenseEntity(
        expenseId = expenseId,
        category = category.name,
        amount = amount,
        merchantName = merchantName,
        note = note,
        receiptPhotoId = receiptPhotoId,
        expenseStatus = expenseStatus,
        approvedBy = approvedBy,
        createdAt = createdAt
    )

    private fun UserEntity.toUserDomain() = User(
        userId = userId,
        displayName = displayName,
        role = Role.valueOf(role),
        pinHash = pinHash,
        isActive = isActive,
        updatedAt = updatedAt
    )
}
