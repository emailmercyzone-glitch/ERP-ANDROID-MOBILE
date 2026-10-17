package com.example.domain.pricing

import com.example.domain.model.B2bContract
import com.example.domain.model.PriceSource
import com.example.domain.model.Product
import com.example.domain.model.ProductPrice
import com.example.domain.model.Promo

/**
 * Implements canonical price resolution hierarchy as defined in D-012, SAL-R03, and SAL-R04:
 * 1. B2B CONTRACT PRICE (CustomerId x ProductId, if active and customer matches)
 * 2. PROMO PRICE (active promo that yields the lowest price; promos do NOT stack)
 * 3. WHOLESALE TIER (based on minQty tier for the given UoM)
 * 4. RETAIL BASE PRICE (fallback default)
 */
object PricingEngine {

    data class ResolvedPrice(
        val unitPrice: Long,
        val priceSource: PriceSource
    )

    fun resolvePrice(
        product: Product,
        qty: Long, // in selected UoM
        uomName: String,
        customerId: String? = null,
        currentTime: Long = System.currentTimeMillis(),
        contracts: List<B2bContract> = emptyList(),
        promos: List<Promo> = emptyList(),
        wholesaleTiers: List<ProductPrice> = emptyList()
    ): ResolvedPrice {
        // 1. Check B2B Contract Price
        if (customerId != null) {
            val contract = contracts.firstOrNull {
                it.customerId == customerId &&
                it.productId == product.id &&
                it.isActive &&
                currentTime in it.validFrom..it.validTo
            }
            if (contract != null && contract.contractPrice > 0L) {
                return ResolvedPrice(contract.contractPrice, PriceSource.B2B)
            }
        }

        // 2. Check Active Promo (Best saving wins, no stacking)
        val validPromos = promos.filter {
            it.productId == product.id &&
            it.isActive &&
            currentTime in it.startAt..it.endAt &&
            qty >= it.minQty
        }
        val bestPromo = validPromos.minByOrNull { it.promoPrice }
        if (bestPromo != null && bestPromo.promoPrice < product.retailPrice) {
            return ResolvedPrice(bestPromo.promoPrice, PriceSource.PROMO)
        }

        // 3. Check Wholesale Tier
        val matchingTiers = wholesaleTiers.filter {
            it.productId == product.id &&
            it.uomName.equals(uomName, ignoreCase = true) &&
            it.isActive &&
            it.minQty > 0L &&
            qty >= it.minQty &&
            currentTime in it.validFrom..it.validTo
        }
        val bestWholesale = matchingTiers.maxByOrNull { it.minQty }
        if (bestWholesale != null && bestWholesale.sellPrice > 0L) {
            return ResolvedPrice(bestWholesale.sellPrice, PriceSource.WHOLESALE)
        }

        // 4. Fallback to Retail Price
        return ResolvedPrice(product.retailPrice, PriceSource.RETAIL)
    }
}
