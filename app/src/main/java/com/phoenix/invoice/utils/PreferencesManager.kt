package com.phoenix.invoice.utils
import android.content.Context
import com.phoenix.invoice.models.InvoiceData

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("phoenix_prefs", Context.MODE_PRIVATE)

    fun saveCompanyDetails(invoice: InvoiceData) {
        prefs.edit().apply {
            putString("company_name", invoice.companyName)
            putString("company_address", invoice.companyAddress)
            putString("company_phone", invoice.companyPhone)
            putString("company_email", invoice.companyEmail)
            putString("company_gst", invoice.companyGst)
            putString("company_logo", invoice.companyLogoUri)
            apply()
        }
    }

    fun loadCompanyDetails(invoice: InvoiceData) {
        invoice.companyName    = prefs.getString("company_name", "") ?: ""
        invoice.companyAddress = prefs.getString("company_address", "") ?: ""
        invoice.companyPhone   = prefs.getString("company_phone", "") ?: ""
        invoice.companyEmail   = prefs.getString("company_email", "") ?: ""
        invoice.companyGst     = prefs.getString("company_gst", "") ?: ""
        invoice.companyLogoUri = prefs.getString("company_logo", "") ?: ""
    }

    fun getDefaultTax(): Double =
        prefs.getString("default_tax", "18.0")?.toDoubleOrNull() ?: 18.0
}
