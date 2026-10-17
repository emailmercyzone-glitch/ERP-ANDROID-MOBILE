package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import com.example.data.local.entity.PromoEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.StockLayerEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.SystemEventEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id AND isDeleted = 0")
    fun getProductById(id: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductByIdSync(id: String): ProductEntity?

    @Query("SELECT * FROM product_uoms WHERE productId = :productId")
    fun getUomsForProduct(productId: String): Flow<List<ProductUomEntity>>

    @Query("SELECT * FROM product_uoms WHERE productId = :productId")
    suspend fun getUomsForProductSync(productId: String): List<ProductUomEntity>

    @Query("SELECT * FROM product_prices WHERE productId = :productId AND isActive = 1")
    fun getPricesForProduct(productId: String): Flow<List<ProductPriceEntity>>

    @Query("SELECT * FROM product_prices WHERE productId = :productId AND isActive = 1")
    suspend fun getPricesForProductSync(productId: String): List<ProductPriceEntity>

    @Query("SELECT * FROM promos WHERE isActive = 1")
    fun getActivePromos(): Flow<List<PromoEntity>>

    @Query("SELECT * FROM promos WHERE isActive = 1")
    suspend fun getActivePromosSync(): List<PromoEntity>

    @Query("SELECT * FROM b2b_contracts WHERE isActive = 1")
    fun getActiveB2bContracts(): Flow<List<B2bContractEntity>>

    @Query("SELECT * FROM b2b_contracts WHERE isActive = 1")
    suspend fun getActiveB2bContractsSync(): List<B2bContractEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUom(uom: ProductUomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrice(price: ProductPriceEntity)
}

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_layers WHERE productId = :productId AND remainingQty > 0 ORDER BY receivedAt ASC")
    fun getActiveLayersForProduct(productId: String): Flow<List<StockLayerEntity>>

    @Query("SELECT * FROM stock_layers WHERE productId = :productId AND remainingQty > 0 ORDER BY receivedAt ASC")
    suspend fun getActiveLayersForProductSync(productId: String): List<StockLayerEntity>

    @Query("SELECT * FROM stock_layers WHERE remainingQty > 0")
    suspend fun getAllActiveLayersSync(): List<StockLayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayer(layer: StockLayerEntity)

    @Update
    suspend fun updateLayer(layer: StockLayerEntity)

    @Update
    suspend fun updateLayers(layers: List<StockLayerEntity>)

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC")
    fun getMovementsForProduct(productId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC LIMIT 100")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumption(consumption: FifoConsumptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumptions(consumptions: List<FifoConsumptionEntity>)

    @Query("SELECT * FROM fifo_consumptions WHERE saleId = :saleId")
    suspend fun getConsumptionsForSale(saleId: String): List<FifoConsumptionEntity>
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE saleId = :saleId")
    fun getSaleById(saleId: String): Flow<SaleEntity?>

    @Query("SELECT * FROM sales WHERE checkoutRequestId = :requestId LIMIT 1")
    suspend fun getSaleByCheckoutRequestId(requestId: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE saleId = :saleId")
    suspend fun getSaleByIdSync(saleId: String): SaleEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(sale: SaleEntity)

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsForSale(saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getSaleItemsForSaleFlow(saleId: String): Flow<List<SaleItemEntity>>
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY transactionDate DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE journalId = :journalId")
    fun getJournalEntryById(journalId: String): Flow<JournalEntryEntity?>

    @Query("SELECT * FROM journal_entries WHERE journalId = :journalId")
    suspend fun getJournalEntryByIdSync(journalId: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalEntry(entry: JournalEntryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalLines(lines: List<JournalLineEntity>)

    @Query("SELECT * FROM journal_lines WHERE journalId = :journalId")
    suspend fun getJournalLinesForEntry(journalId: String): List<JournalLineEntity>

    @Query("SELECT * FROM journal_lines")
    suspend fun getAllLinesSync(): List<JournalLineEntity>
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM cashier_shifts WHERE cashierId = :cashierId AND shiftStatus = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun getActiveShift(cashierId: String): Flow<CashierShiftEntity?>

    @Query("SELECT * FROM cashier_shifts WHERE cashierId = :cashierId AND shiftStatus = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getActiveShiftSync(cashierId: String): CashierShiftEntity?

    @Query("SELECT * FROM cashier_shifts ORDER BY openedAt DESC")
    fun getAllShifts(): Flow<List<CashierShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashierShiftEntity)

    @Update
    suspend fun updateShift(shift: CashierShiftEntity)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE isDeleted = 0 ORDER BY displayName ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE contactType = :type AND isDeleted = 0 ORDER BY displayName ASC")
    fun getContactsByType(type: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE contactId = :id")
    suspend fun getContactByIdSync(id: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)
}

@Dao
interface ConfigDao {
    @Query("SELECT valueCanonical FROM business_configurations WHERE configKey = :key AND status = 'ACTIVE' ORDER BY revision DESC LIMIT 1")
    fun getConfigValue(key: String): Flow<String?>

    @Query("SELECT * FROM business_configurations WHERE status = 'ACTIVE'")
    fun getAllActiveConfigs(): Flow<List<BusinessConfigEntity>>

    @Query("SELECT MAX(revision) FROM business_configurations WHERE configKey = :key")
    suspend fun getLatestRevision(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: BusinessConfigEntity)
}

@Dao
interface NumberSequenceDao {
    @Query("SELECT * FROM number_sequences WHERE deviceId = :deviceId AND docType = :docType AND sequenceDate = :date")
    suspend fun getSequence(deviceId: String, docType: String, date: String): NumberSequenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(sequence: NumberSequenceEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY displayName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserByIdSync(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)
}

@Dao
interface SystemEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SystemEventEntity)

    @Query("SELECT * FROM system_events ORDER BY occurredAt DESC LIMIT 50")
    fun getRecentEvents(): Flow<List<SystemEventEntity>>
}
