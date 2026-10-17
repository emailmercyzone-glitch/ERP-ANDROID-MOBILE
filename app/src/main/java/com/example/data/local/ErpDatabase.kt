package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ConfigDao
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.JournalDao
import com.example.data.local.dao.NumberSequenceDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.SaleDao
import com.example.data.local.dao.ShiftDao
import com.example.data.local.dao.StockDao
import com.example.data.local.dao.SystemEventDao
import com.example.data.local.dao.UserDao
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
import com.example.domain.auth.AuthPolicy
import com.example.domain.config.BusinessConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ProductEntity::class,
        ProductUomEntity::class,
        ProductPriceEntity::class,
        B2bContractEntity::class,
        PromoEntity::class,
        StockLayerEntity::class,
        StockMovementEntity::class,
        FifoConsumptionEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        JournalEntryEntity::class,
        JournalLineEntity::class,
        ContactEntity::class,
        CashierShiftEntity::class,
        ExpenseEntity::class,
        BusinessConfigEntity::class,
        NumberSequenceEntity::class,
        SystemEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ErpDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun stockDao(): StockDao
    abstract fun saleDao(): SaleDao
    abstract fun journalDao(): JournalDao
    abstract fun shiftDao(): ShiftDao
    abstract fun contactDao(): ContactDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun configDao(): ConfigDao
    abstract fun numberSequenceDao(): NumberSequenceDao
    abstract fun userDao(): UserDao
    abstract fun systemEventDao(): SystemEventDao

    companion object {
        @Volatile
        private var INSTANCE: ErpDatabase? = null

        fun getInstance(context: Context): ErpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ErpDatabase::class.java,
                    "omah_erp.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.seedInitialData()
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedInitialData() {
        val now = System.currentTimeMillis()

        // 1. Seed Users (Owner PIN: 123456, Kasir PIN: 654321)
        val ownerPinHash = AuthPolicy.hashPin("123456")
        val kasirPinHash = AuthPolicy.hashPin("654321")
        userDao().insertUsers(
            listOf(
                UserEntity("OWNER-01", "Pak Budi (Owner)", "OWNER", ownerPinHash, true, now),
                UserEntity("KSR-01", "Siti Aminah (Kasir)", "KASIR", kasirPinHash, true, now)
            )
        )

        // 2. Seed Default Business Configs
        BusinessConfig.DEFAULTS.forEach { (key, value) ->
            configDao().insertConfig(
                BusinessConfigEntity(
                    configurationId = "CFG-$key-1",
                    configKey = key,
                    valueCanonical = value,
                    revision = 1L,
                    effectiveFrom = now,
                    status = "ACTIVE"
                )
            )
        }

        // 3. Seed Realistic Sembako Products
        val products = listOf(
            ProductEntity("PRD-01", "EQT-0001", "Beras Ramos Super 5kg", "8991001", "Beras", "EQT", "sak", null, 1000L, null, 5L, false, "ACTIVE", 68000L, 20L, now),
            ProductEntity("PRD-02", "EQT-0002", "Minyak Goreng Kita 1L", "8991002", "Minyak", "EQT", "btl", null, 1000L, null, 10L, false, "ACTIVE", 16500L, 35L, now),
            ProductEntity("PRD-03", "EQT-0003", "Gula Pasir Gulaku 1kg", "8991003", "Gula", "EQT", "bks", null, 1000L, null, 8L, false, "ACTIVE", 17500L, 25L, now),
            ProductEntity("PRD-04", "EQT-0004", "Telur Ayam Ras 1kg", "8991004", "Telur", "EQT", "kg", null, 1000L, null, 5L, false, "ACTIVE", 28000L, 15L, now),
            ProductEntity("PRD-05", "EQT-0005", "Susu Kental Manis Frisian Flag", "8991005", "Susu", "EQT", "klg", null, 1000L, null, 12L, false, "ACTIVE", 12500L, 40L, now),
            ProductEntity("PRD-06", "EQT-0006", "Tepung Terigu Segitiga Biru 1kg", "8991006", "Tepung", "EQT", "bks", null, 1000L, null, 10L, false, "ACTIVE", 13000L, 30L, now),
            ProductEntity("PRD-07", "KNS-0001", "Keripik Singkong Bu Wati", "8991007", "Snack", "KNS", "bks", "CONSIGN-01", 1000L, null, 5L, false, "ACTIVE", 10000L, 20L, now)
        )
        productDao().insertProducts(products)

        // 4. Seed Stock Layers for FIFO
        stockDao().insertLayer(StockLayerEntity("L-PRD-01-1", "PRD-01", "GRN-INIT-01", now - 86400000L, 20L, 20L, 60000L, "EQT"))
        stockDao().insertLayer(StockLayerEntity("L-PRD-02-1", "PRD-02", "GRN-INIT-02", now - 86400000L, 35L, 35L, 14000L, "EQT"))
        stockDao().insertLayer(StockLayerEntity("L-PRD-03-1", "PRD-03", "GRN-INIT-03", now - 86400000L, 25L, 25L, 15000L, "EQT"))
        stockDao().insertLayer(StockLayerEntity("L-PRD-04-1", "PRD-04", "GRN-INIT-04", now - 86400000L, 15L, 15L, 24000L, "EQT"))
        stockDao().insertLayer(StockLayerEntity("L-PRD-05-1", "PRD-05", "GRN-INIT-05", now - 86400000L, 40L, 40L, 10500L, "EQT"))
        stockDao().insertLayer(StockLayerEntity("L-PRD-06-1", "PRD-06", "GRN-INIT-06", now - 86400000L, 30L, 30L, 11000L, "EQT"))
        stockDao().insertLayer(StockLayerEntity("L-PRD-07-1", "PRD-07", "GRN-INIT-07", now - 86400000L, 20L, 20L, 9000L, "KNS"))

        // 5. Seed Multi-UoM options
        productDao().insertUom(ProductUomEntity("UOM-02-DUS", "PRD-02", "Dus (12 Pcs)", 12L, true))
        productDao().insertUom(ProductUomEntity("UOM-05-DUS", "PRD-05", "Dus (48 Klg)", 48L, true))
        productDao().insertPrice(ProductPriceEntity("PRC-02-W1", "PRD-02", "btl", 12L, 16000L)) // Grosir Rp 16.000 jika beli >= 12

        // 6. Seed Sample Contacts
        contactDao().insertContacts(
            listOf(
                ContactEntity("CUST-01", "Pak Joko (Langganan)", "0813-1111-2222", "CUSTOMER", true, 500000L, 14L, 0L, false, now),
                ContactEntity("CUST-02", "Bu Siti (Langganan)", "0813-3333-4444", "CUSTOMER", true, 500000L, 14L, 0L, false, now),
                ContactEntity("SUPP-01", "Agen Beras Makmur", "0812-5555-6666", "SUPPLIER", false, 0L, 0L, 0L, false, now),
                ContactEntity("SUPP-02", "Distributor Sembako Berkah", "0812-7777-8888", "SUPPLIER", false, 0L, 0L, 0L, false, now),
                ContactEntity("CONSIGN-01", "Bu Wati (Keripik Singkong)", "0815-9999-0000", "CONSIGNOR", false, 0L, 0L, 0L, false, now)
            )
        )
    }
}
