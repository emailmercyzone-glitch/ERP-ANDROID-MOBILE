package com.example.domain.model

enum class ProductKind {
    EQT, // Equity - store owned, FIFO, inventory asset
    KNS, // Consignment - third party owned, liability born on sale, 10% commission default
    AFL, // Affiliate - referral only, no physical store stock
    VRT  // Virtual - digital products / PPOB (deferred)
}

enum class ProductStatus {
    DRAFT,
    ACTIVE,
    BLOCKED_AFL
}

enum class MovementType {
    IN_RECEIPT,      // Incoming purchase receipt (GRN)
    OUT_SALE,        // Sold at cashier POS
    IN_RETURN_SALE,  // Returned by customer
    OUT_RETURN_BUY,  // Returned to supplier
    ADJUST_COUNT,    // Cycle count / stock adjustment
    OUT_DAMAGED      // Damaged / expired write-off
}

enum class Role {
    OWNER,
    KASIR
}

enum class PaymentMethod {
    TUNAI,
    QRIS,
    TRANSFER
}

enum class PoStatus {
    DRAFT,
    ORDERED,
    PARTIAL,
    RECEIVED,
    CANCELLED
}

enum class BillStatus {
    UNPAID,
    PARTIAL,
    PAID
}

enum class ShiftStatus {
    OPEN,
    CLOSED
}

enum class ExpenseCategory(val displayName: String, val accountCode: String) {
    GAJI_KARYAWAN("Gaji Karyawan", "5-1200"),
    LISTRIK("Listrik PLN", "5-1400"),
    AIR("Air PDAM", "5-1400"),
    SEWA_TEMPAT("Sewa Tempat", "5-1400"),
    INTERNET_PULSA("Internet & Pulsa", "5-1400"),
    BBM_OPERASIONAL("BBM & Transport", "5-1400"),
    LAIN_LAIN("Biaya Lain-Lain", "5-1400")
}

enum class AccountType {
    ASET,
    KEWAJIBAN,
    EKUITAS,
    PENDAPATAN,
    BEBAN
}

enum class PriceSource {
    B2B,
    PROMO,
    WHOLESALE,
    RETAIL
}
