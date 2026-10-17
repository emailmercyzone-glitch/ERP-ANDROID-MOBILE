package com.example.domain.inventory

import com.example.domain.model.FifoConsumption
import com.example.domain.model.ProductKind
import com.example.domain.model.StockLayer

/**
 * Implements strict FIFO (First-In, First-Out) stock consumption as defined in
 * PRIN-INV-01..04, INV-R01..R03, and D-009.
 * Consumes oldest available layers first, computes exact integer COGS/HPP,
 * and prevents negative stock for store-owned goods (EQT).
 */
object FifoEngine {

    data class FifoResult(
        val consumptions: List<FifoConsumption>,
        val updatedLayers: List<StockLayer>,
        val totalCogs: Long
    )

    fun consume(
        saleId: String,
        productId: String,
        productKind: ProductKind,
        productName: String,
        requestedBaseQty: Long,
        availableLayers: List<StockLayer>,
        currentTime: Long = System.currentTimeMillis()
    ): FifoResult {
        require(requestedBaseQty > 0L) { "Requested base quantity must be > 0, got: $requestedBaseQty" }

        // Sort layers: oldest receivedAt first
        val sortedLayers = availableLayers
            .filter { it.productId == productId && it.remainingQty > 0L }
            .sortedBy { it.receivedAt }

        val totalAvailable = sortedLayers.sumOf { it.remainingQty }

        // Anti-oversell check for store-owned inventory (EQT) (SAL-R01, INV-R03)
        if (productKind == ProductKind.EQT && totalAvailable < requestedBaseQty) {
            throw IllegalStateException(
                "Stok fisik tidak mencukupi untuk '$productName'! Sisa tersedia: $totalAvailable, diminta: $requestedBaseQty"
            )
        }

        var remainingToConsume = requestedBaseQty
        val consumptions = mutableListOf<FifoConsumption>()
        val updatedLayers = mutableListOf<StockLayer>()
        var totalCogs = 0L

        for (layer in sortedLayers) {
            if (remainingToConsume <= 0L) {
                updatedLayers.add(layer)
                continue
            }

            val qtyToTake = minOf(layer.remainingQty, remainingToConsume)
            val lineCogs = Math.multiplyExact(qtyToTake, layer.unitCost)
            totalCogs = Math.addExact(totalCogs, lineCogs)

            consumptions.add(
                FifoConsumption(
                    consumptionId = "CNS-${saleId}-${layer.layerId}",
                    saleId = saleId,
                    productId = productId,
                    layerId = layer.layerId,
                    qtyTaken = qtyToTake,
                    unitCost = layer.unitCost,
                    cogsAmount = lineCogs
                )
            )

            updatedLayers.add(
                layer.copy(remainingQty = layer.remainingQty - qtyToTake)
            )

            remainingToConsume -= qtyToTake
        }

        return FifoResult(
            consumptions = consumptions,
            updatedLayers = updatedLayers,
            totalCogs = totalCogs
        )
    }
}
