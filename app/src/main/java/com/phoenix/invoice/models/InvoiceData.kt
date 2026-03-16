package com.phoenix.invoice.models
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

data class InvoiceData(
    var companyName: String = "",
    var companyAddress: String = "",
    var companyPhone: String = "",
    var companyEmail: String = "",
    var companyGst: String = "",
    var companyLogoUri: String = "",
    var customerName: String = "",
    var customerPhone: String = "",
    var customerAddress: String = "",
    var customerGst: String = "",
    var invoiceNumber: String = generateInvoiceNumber(),
    var invoiceDate: String = todayDate(),
    var dueDate: String = "",
    var notes: String = "",
    var selectedTemplate: Int = 0,
    var items: MutableList<InvoiceItem> = mutableListOf(),
    var amountPaid: Double = 0.0
) : Serializable {
    val subtotal: Double get() = items.sumOf { it.subtotal }
    val totalDiscount: Double get() = items.sumOf { it.discountAmount }
    val totalTax: Double get() = items.sumOf { it.taxAmount }
    val grandTotal: Double get() = items.sumOf { it.lineTotal }
    val balanceDue: Double get() = grandTotal - amountPaid

    companion object {
        fun generateInvoiceNumber(): String {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR).toString().takeLast(2)
            val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
            val seq = (100..999).random()
            return "PHX-$year$month-$seq"
        }
        fun todayDate(): String =
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }
}
