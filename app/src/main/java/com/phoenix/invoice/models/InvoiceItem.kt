package com.phoenix.invoice.models
import java.io.Serializable

data class InvoiceItem(
    var id: Long = System.currentTimeMillis(),
    var name: String = "",
    var quantity: Double = 1.0,
    var unitPrice: Double = 0.0,
    var discountPercent: Double = 0.0,
    var taxPercent: Double = 18.0
) : Serializable {
    val subtotal: Double get() = quantity * unitPrice
    val discountAmount: Double get() = subtotal * discountPercent / 100.0
    val taxableAmount: Double get() = subtotal - discountAmount
    val taxAmount: Double get() = taxableAmount * taxPercent / 100.0
    val lineTotal: Double get() = taxableAmount + taxAmount
}
