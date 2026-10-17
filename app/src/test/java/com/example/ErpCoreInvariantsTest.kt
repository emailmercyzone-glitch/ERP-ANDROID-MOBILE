package com.example

import com.example.domain.accounting.AccountingEngine
import com.example.domain.auth.AuthPolicy
import com.example.domain.cash.CashRounding
import com.example.domain.inventory.FifoEngine
import com.example.domain.model.B2bContract
import com.example.domain.model.Product
import com.example.domain.model.ProductKind
import com.example.domain.model.ProductPrice
import com.example.domain.model.ProductStatus
import com.example.domain.model.Promo
import com.example.domain.model.StockLayer
import com.example.domain.money.Money
import com.example.domain.pricing.PricingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErpCoreInvariantsTest {

    @Test
    fun `test exact money arithmetic and overflow protection`() {
        assertEquals(50000L, Money.add(20000L, 30000L))
        assertEquals(15000L, Money.subtract(30000L, 15000L))
        assertEquals(100000L, Money.multiply(20000L, 5L))

        // Overflow check (fail-hard)
        var overflowCaught = false
        try {
            Money.add(Long.MAX_VALUE, 1L)
        } catch (e: ArithmeticException) {
            overflowCaught = true
        }
        assertTrue(overflowCaught)
    }

    @Test
    fun `test cash rounding to nearest Rp 500 half-up`() {
        // CSH-R05: Rp 12.200 -> Rp 12.000 (-200)
        val (rounded1, diff1) = CashRounding.round500(12200L)
        assertEquals(12000L, rounded1)
        assertEquals(-200L, diff1)

        // Rp 12.300 -> Rp 12.500 (+200)
        val (rounded2, diff2) = CashRounding.round500(12300L)
        assertEquals(12500L, rounded2)
        assertEquals(200L, diff2)

        // Exact multiple
        val (rounded3, diff3) = CashRounding.round500(15000L)
        assertEquals(15000L, rounded3)
        assertEquals(0L, diff3)
    }

    @Test
    fun `test pricing hierarchy canonical order`() {
        val product = Product(
            id = "PRD-01",
            sku = "EQT-0001",
            name = "Beras Ramos 5kg",
            barcode = null,
            categoryId = "Beras",
            productKind = ProductKind.EQT,
            baseUomId = "sak",
            retailPrice = 68000L
        )

        // Fallback retail
        val pRetail = PricingEngine.resolvePrice(product, 1L, "sak")
        assertEquals(68000L, pRetail.unitPrice)

        // Wholesale tier for >= 5 sak: Rp 65.000
        val tiers = listOf(ProductPrice("PRC-1", "PRD-01", "sak", 5L, 65000L))
        val pWholesale = PricingEngine.resolvePrice(product, 5L, "sak", wholesaleTiers = tiers)
        assertEquals(65000L, pWholesale.unitPrice)

        // Active promo: Rp 62.000 (cheaper than wholesale, promo wins over wholesale)
        val promos = listOf(Promo("PRM-1", "PRD-01", 62000L))
        val pPromo = PricingEngine.resolvePrice(product, 5L, "sak", promos = promos, wholesaleTiers = tiers)
        assertEquals(62000L, pPromo.unitPrice)

        // B2B contract for Pak Joko: Rp 60.000 (contract wins over promo & wholesale)
        val contracts = listOf(B2bContract("CTR-1", "CUST-01", "PRD-01", 60000L))
        val pContract = PricingEngine.resolvePrice(product, 5L, "sak", customerId = "CUST-01", contracts = contracts, promos = promos, wholesaleTiers = tiers)
        assertEquals(60000L, pContract.unitPrice)
    }

    @Test
    fun `test fifo layer consumption and exact cogs`() {
        val layers = listOf(
            StockLayer("L1", "PRD-01", "GRN-1", 1000L, 10L, 10L, 10000L),
            StockLayer("L2", "PRD-01", "GRN-2", 2000L, 20L, 20L, 11000L)
        )

        // Consume 15 items: should take all 10 from L1 (@ 10.000) and 5 from L2 (@ 11.000)
        // Total COGS = 10*10.000 + 5*11.000 = 100.000 + 55.000 = 155.000
        val result = FifoEngine.consume(
            saleId = "SALE-01",
            productId = "PRD-01",
            productKind = ProductKind.EQT,
            productName = "Beras",
            requestedBaseQty = 15L,
            availableLayers = layers
        )

        assertEquals(155000L, result.totalCogs)
        assertEquals(2, result.consumptions.size)
        assertEquals(10L, result.consumptions[0].qtyTaken)
        assertEquals(5L, result.consumptions[1].qtyTaken)
        assertEquals(0L, result.updatedLayers[0].remainingQty)
        assertEquals(15L, result.updatedLayers[1].remainingQty)
    }

    @Test
    fun `test anti oversell rejects when eqt stock is insufficient`() {
        val layers = listOf(
            StockLayer("L1", "PRD-01", "GRN-1", 1000L, 5L, 5L, 10000L)
        )

        var rejected = false
        try {
            FifoEngine.consume(
                saleId = "SALE-01",
                productId = "PRD-01",
                productKind = ProductKind.EQT,
                productName = "Beras",
                requestedBaseQty = 10L, // Available is only 5
                availableLayers = layers
            )
        } catch (e: IllegalStateException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun `test double entry journal debit equals credit invariant`() {
        val journal = AccountingEngine.createSaleJournal(
            journalId = "JRN-01",
            saleId = "JUAL-01",
            paidCash = 100000L,
            paidQris = 0L,
            paidTransfer = 0L,
            receivableAmount = 0L,
            cogsTotal = 60000L,
            equitySalesRevenue = 100000L,
            consignorPayable = 0L,
            consignmentCommission = 0L,
            packagingAndDeliveryFee = 0L,
            roundingDiff = 0L
        )

        val totalDebit = journal.lines.sumOf { it.debit }
        val totalCredit = journal.lines.sumOf { it.credit }

        assertEquals(160000L, totalDebit)
        assertEquals(160000L, totalCredit)
        assertEquals(totalDebit, totalCredit)
    }

    @Test
    fun `test auth pin hashing and verification`() {
        val pin = "123456"
        val hash = AuthPolicy.hashPin(pin)

        assertTrue(AuthPolicy.verifyPin("123456", hash))
        assertFalse(AuthPolicy.verifyPin("654321", hash))
    }
}
