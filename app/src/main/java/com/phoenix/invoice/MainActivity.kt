package com.phoenix.invoice

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.phoenix.invoice.adapters.ItemsAdapter
import com.phoenix.invoice.models.InvoiceData
import com.phoenix.invoice.models.InvoiceItem
import com.phoenix.invoice.utils.PdfGenerator
import com.phoenix.invoice.utils.PreferencesManager
import kotlinx.coroutines.*
import java.io.File
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var invoice: InvoiceData
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: ItemsAdapter
    private val rupee = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Views
    private lateinit var etCName: TextInputEditText
    private lateinit var etCAddr: TextInputEditText
    private lateinit var etCPhone: TextInputEditText
    private lateinit var etCEmail: TextInputEditText
    private lateinit var etCGst: TextInputEditText
    private lateinit var ivLogo: ImageView
    private lateinit var etKName: TextInputEditText
    private lateinit var etKPhone: TextInputEditText
    private lateinit var etKAddr: TextInputEditText
    private lateinit var etKGst: TextInputEditText
    private lateinit var etInvNum: TextInputEditText
    private lateinit var etInvDate: TextInputEditText
    private lateinit var etDueDate: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var etPaid: TextInputEditText
    private lateinit var rvItems: RecyclerView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvDiscount: TextView
    private lateinit var tvTax: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var tvBalance: TextView
    private lateinit var rowDiscount: View
    private lateinit var rowTax: View
    private lateinit var btnT0: MaterialButton
    private lateinit var btnT1: MaterialButton
    private lateinit var btnT2: MaterialButton
    private lateinit var btnDownload: MaterialButton
    private lateinit var btnShare: MaterialButton

    private val logoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: Exception) {}
            invoice.companyLogoUri = it.toString()
            ivLogo.setImageURI(it)
            ivLogo.visibility = View.VISIBLE
        }
    }
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferencesManager(this)
        invoice = InvoiceData().also {
            prefs.loadCompanyDetails(it)
            it.items.add(InvoiceItem(taxPercent = prefs.getDefaultTax()))
        }
        bindViews(); populateFields(); setupRecycler()
        setupTemplates(); setupActions(); setupDates()
        requestNeededPermissions(); updateTotals()
    }

    private fun bindViews() {
        etCName = f(R.id.etCompanyName); etCAddr = f(R.id.etCompanyAddress)
        etCPhone = f(R.id.etCompanyPhone); etCEmail = f(R.id.etCompanyEmail)
        etCGst = f(R.id.etCompanyGst); ivLogo = findViewById(R.id.ivLogo)
        etKName = f(R.id.etCustomerName); etKPhone = f(R.id.etCustomerPhone)
        etKAddr = f(R.id.etCustomerAddress); etKGst = f(R.id.etCustomerGst)
        etInvNum = f(R.id.etInvoiceNumber); etInvDate = f(R.id.etInvoiceDate)
        etDueDate = f(R.id.etDueDate); etNotes = f(R.id.etNotes); etPaid = f(R.id.etAmountPaid)
        rvItems = findViewById(R.id.rvItems)
        tvSubtotal = findViewById(R.id.tvSubtotal); tvDiscount = findViewById(R.id.tvDiscount)
        tvTax = findViewById(R.id.tvTax); tvGrandTotal = findViewById(R.id.tvGrandTotal)
        tvBalance = findViewById(R.id.tvBalanceDue)
        rowDiscount = findViewById(R.id.layoutDiscountRow); rowTax = findViewById(R.id.layoutTaxRow)
        btnT0 = findViewById(R.id.btnTemplate0); btnT1 = findViewById(R.id.btnTemplate1); btnT2 = findViewById(R.id.btnTemplate2)
        btnDownload = findViewById(R.id.btnDownload); btnShare = findViewById(R.id.btnShare)
        findViewById<MaterialButton>(R.id.btnUploadLogo).setOnClickListener { logoPicker.launch("image/*") }
        findViewById<MaterialButton>(R.id.btnNewInvoice).setOnClickListener { confirmNew() }
        findViewById<MaterialButton>(R.id.btnAddItem).setOnClickListener {
            invoice.items.add(InvoiceItem(taxPercent = prefs.getDefaultTax()))
            adapter.notifyItemInserted(invoice.items.size - 1); updateTotals()
        }
    }

    private fun f(id: Int): TextInputEditText = findViewById(id)

    private fun populateFields() {
        etCName.setText(invoice.companyName); etCAddr.setText(invoice.companyAddress)
        etCPhone.setText(invoice.companyPhone); etCEmail.setText(invoice.companyEmail)
        etCGst.setText(invoice.companyGst); etInvNum.setText(invoice.invoiceNumber)
        etInvDate.setText(invoice.invoiceDate)
        if (invoice.companyLogoUri.isNotEmpty()) {
            ivLogo.setImageURI(Uri.parse(invoice.companyLogoUri)); ivLogo.visibility = View.VISIBLE
        }
    }

    private fun setupRecycler() {
        adapter = ItemsAdapter(invoice.items) { updateTotals() }
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = adapter
        rvItems.isNestedScrollingEnabled = false
    }

    private fun setupTemplates() {
        fun sel(i: Int) {
            invoice.selectedTemplate = i
            listOf(btnT0, btnT1, btnT2).forEachIndexed { idx, btn ->
                btn.strokeWidth = if (idx == i) 4 else 1
                btn.isSelected = idx == i
            }
        }
        btnT0.setOnClickListener { sel(0) }; btnT1.setOnClickListener { sel(1) }
        btnT2.setOnClickListener { sel(2) }; sel(0)
    }

    private fun setupActions() {
        btnDownload.setOnClickListener { generate(share = false) }
        btnShare.setOnClickListener { generate(share = true) }
    }

    private fun setupDates() {
        fun picker(et: TextInputEditText) {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                et.setText("%02d/%02d/%04d".format(d, m + 1, y))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        etInvDate.setOnClickListener { picker(etInvDate) }
        etInvDate.setOnFocusChangeListener { _, f -> if (f) picker(etInvDate) }
        etDueDate.setOnClickListener { picker(etDueDate) }
        etDueDate.setOnFocusChangeListener { _, f -> if (f) picker(etDueDate) }
    }

    private fun updateTotals() {
        invoice.amountPaid = etPaid.text.toString().toDoubleOrNull() ?: 0.0
        tvSubtotal.text = rupee.format(invoice.subtotal)
        tvDiscount.text = "- ${rupee.format(invoice.totalDiscount)}"
        tvTax.text = rupee.format(invoice.totalTax)
        tvGrandTotal.text = rupee.format(invoice.grandTotal)
        tvBalance.text = rupee.format(invoice.balanceDue)
        rowDiscount.visibility = if (invoice.totalDiscount > 0) View.VISIBLE else View.GONE
        rowTax.visibility = if (invoice.totalTax > 0) View.VISIBLE else View.GONE
    }

    private fun collect() {
        invoice.companyName    = etCName.text.toString().trim()
        invoice.companyAddress = etCAddr.text.toString().trim()
        invoice.companyPhone   = etCPhone.text.toString().trim()
        invoice.companyEmail   = etCEmail.text.toString().trim()
        invoice.companyGst     = etCGst.text.toString().trim()
        invoice.customerName   = etKName.text.toString().trim()
        invoice.customerPhone  = etKPhone.text.toString().trim()
        invoice.customerAddress= etKAddr.text.toString().trim()
        invoice.customerGst    = etKGst.text.toString().trim()
        invoice.invoiceNumber  = etInvNum.text.toString().trim()
        invoice.invoiceDate    = etInvDate.text.toString().trim()
        invoice.dueDate        = etDueDate.text.toString().trim()
        invoice.notes          = etNotes.text.toString().trim()
        invoice.amountPaid     = etPaid.text.toString().toDoubleOrNull() ?: 0.0
        prefs.saveCompanyDetails(invoice)
    }

    private fun validate(): Boolean {
        if (invoice.companyName.isEmpty()) { snack("Enter company name", true); etCName.requestFocus(); return false }
        if (invoice.customerName.isEmpty()) { snack("Enter customer name", true); etKName.requestFocus(); return false }
        if (invoice.items.all { it.name.isEmpty() || it.unitPrice == 0.0 }) { snack("Add at least one item with price", true); return false }
        return true
    }

    private fun generate(share: Boolean) {
        collect(); updateTotals()
        if (!validate()) return
        btnDownload.isEnabled = false; btnShare.isEnabled = false
        btnDownload.text = "Generating PDF..."
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                try { PdfGenerator.generatePdf(this@MainActivity, invoice) } catch (e: Exception) { null }
            }
            btnDownload.isEnabled = true; btnShare.isEnabled = true
            btnDownload.text = "⬇  Download Invoice"
            if (file != null) {
                if (share) openWhatsApp(file) else { snack("✓ Saved to PhoenixInvoices folder", false); offerOpen(file) }
            } else snack("Error generating PDF. Try again.", true)
        }
    }

    private fun openWhatsApp(pdf: File) {
        val msg = """Hello ${invoice.customerName},

Please find your invoice attached.

Invoice: ${invoice.invoiceNumber}
Date: ${invoice.invoiceDate}
Amount: ${rupee.format(invoice.grandTotal)}
${if (invoice.balanceDue > 0 && invoice.amountPaid > 0) "Balance Due: ${rupee.format(invoice.balanceDue)}" else ""}

Thank you for your business!
— ${invoice.companyName}""".trimIndent()

        snack("✓ PDF saved. Opening WhatsApp...", false)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", pdf)
        val wa = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"; setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, msg)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { startActivity(wa) } catch (_: Exception) {
            val phone = invoice.customerPhone.replace("[^0-9+]".toRegex(), "")
            val url = "https://wa.me/$phone?text=${URLEncoder.encode(msg, "UTF-8")}"
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            catch (_: Exception) { snack("WhatsApp not found. PDF saved locally.", true) }
        }
    }

    private fun offerOpen(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Invoice Saved ✓")
            .setMessage("Saved as: ${file.name}\n\nOpen now?")
            .setPositiveButton("Open PDF") { _, _ ->
                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                try {
                    startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "Open with..."
                    ))
                } catch (_: Exception) { snack("Install a PDF viewer app", true) }
            }
            .setNegativeButton("Close", null).show()
    }

    private fun confirmNew() {
        AlertDialog.Builder(this)
            .setTitle("New Invoice")
            .setMessage("Clear all current data?")
            .setPositiveButton("Yes") { _, _ ->
                invoice = InvoiceData().also {
                    prefs.loadCompanyDetails(it)
                    it.items.add(InvoiceItem(taxPercent = prefs.getDefaultTax()))
                }
                listOf(etKName, etKPhone, etKAddr, etKGst, etDueDate, etNotes, etPaid).forEach { it.setText("") }
                etInvNum.setText(invoice.invoiceNumber); etInvDate.setText(invoice.invoiceDate)
                adapter = ItemsAdapter(invoice.items) { updateTotals() }; rvItems.adapter = adapter
                updateTotals(); snack("New invoice ready", false)
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun requestNeededPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permLauncher.launch(perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray())
    }

    private fun snack(msg: String, err: Boolean) {
        val s = Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
        if (err) s.setBackgroundTint(getColor(R.color.error))
        s.show()
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
