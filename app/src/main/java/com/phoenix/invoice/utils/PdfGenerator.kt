package com.phoenix.invoice.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.phoenix.invoice.models.InvoiceData
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.*

object PdfGenerator {
    private val rupee = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private const val W = 595
    private const val H = 842
    private const val M = 36f

    fun generatePdf(context: Context, invoice: InvoiceData): File {
        val doc = PdfDocument()
        val pi = PdfDocument.PageInfo.Builder(W, H, 1).create()
        val page = doc.startPage(pi)
        val c = page.canvas
        var y = M

        y = when (invoice.selectedTemplate) {
            1 -> drawGST(c, invoice, y, context)
            2 -> drawCompact(c, invoice, y, context)
            else -> drawModern(c, invoice, y, context)
        }
        y = drawItems(c, invoice, y)
        drawTotals(c, invoice, y)
        drawFooter(c, invoice)

        doc.finishPage(page)
        val dir = File(context.getExternalFilesDir(null), "PhoenixInvoices")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Invoice-${invoice.invoiceNumber}.pdf")
        doc.writeTo(FileOutputStream(file))
        doc.close()
        return file
    }

    private fun paint(color: Int = Color.BLACK, size: Float = 10f, bold: Boolean = false,
                      align: Paint.Align = Paint.Align.LEFT, alpha: Int = 255): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textAlign = align; this.alpha = alpha
        }

    private fun fillPaint(color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    // ─── TEMPLATE 0: MODERN CLEAN ───────────────────────────────────────────
    private fun drawModern(c: Canvas, inv: InvoiceData, sy: Float, ctx: Context): Float {
        c.drawRect(0f, 0f, W.toFloat(), 115f, fillPaint(Color.parseColor("#16213E")))

        // Logo
        if (inv.companyLogoUri.isNotEmpty()) {
            loadBitmap(ctx, inv.companyLogoUri, 70, 70)?.let {
                c.drawBitmap(it, M, 20f, null)
            }
        }

        val logoOff = if (inv.companyLogoUri.isNotEmpty()) 82f else 0f
        c.drawText(inv.companyName.ifEmpty { "Your Company" }, M + logoOff, 42f,
            paint(Color.WHITE, 18f, true))
        c.drawText(inv.companyAddress, M + logoOff, 57f, paint(Color.parseColor("#B0BEC5"), 8f))
        if (inv.companyPhone.isNotEmpty())
            c.drawText("Ph: ${inv.companyPhone}   Email: ${inv.companyEmail}", M + logoOff, 69f,
                paint(Color.parseColor("#B0BEC5"), 8f))
        if (inv.companyGst.isNotEmpty())
            c.drawText("GSTIN: ${inv.companyGst}", M + logoOff, 81f,
                paint(Color.parseColor("#FFB74D"), 8f, true))

        // Invoice badge (right)
        c.drawText("INVOICE", W - M, 48f, paint(Color.parseColor("#E94560"), 26f, true, Paint.Align.RIGHT))
        c.drawText("#${inv.invoiceNumber}", W - M, 65f, paint(Color.WHITE, 10f, false, Paint.Align.RIGHT))
        c.drawText("Date: ${inv.invoiceDate}", W - M, 78f, paint(Color.parseColor("#90A4AE"), 8f, false, Paint.Align.RIGHT))
        if (inv.dueDate.isNotEmpty())
            c.drawText("Due: ${inv.dueDate}", W - M, 90f, paint(Color.parseColor("#90A4AE"), 8f, false, Paint.Align.RIGHT))

        var y = 128f
        c.drawText("BILL TO", M, y, paint(Color.parseColor("#E94560"), 8f, true))
        y += 14f
        c.drawText(inv.customerName, M, y, paint(Color.parseColor("#1A1A2E"), 13f, true))
        y += 14f
        if (inv.customerAddress.isNotEmpty()) { c.drawText(inv.customerAddress, M, y, paint(Color.parseColor("#546E7A"), 8f)); y += 12f }
        if (inv.customerPhone.isNotEmpty()) { c.drawText("Ph: ${inv.customerPhone}", M, y, paint(Color.parseColor("#546E7A"), 8f)); y += 12f }
        if (inv.customerGst.isNotEmpty()) { c.drawText("GSTIN: ${inv.customerGst}", M, y, paint(Color.parseColor("#546E7A"), 8f)); y += 12f }
        y += 8f
        val lp = Paint().apply { color = Color.parseColor("#E0E0E0"); strokeWidth = 1f }
        c.drawLine(M, y, W - M, y, lp); y += 12f
        return y
    }

    // ─── TEMPLATE 1: GST PROFESSIONAL ───────────────────────────────────────
    private fun drawGST(c: Canvas, inv: InvoiceData, sy: Float, ctx: Context): Float {
        c.drawRect(0f, 0f, W.toFloat(), 6f, fillPaint(Color.parseColor("#E65100")))

        var y = 18f
        c.drawText("TAX INVOICE", W / 2f, y + 16f,
            paint(Color.parseColor("#E65100"), 18f, true, Paint.Align.CENTER))
        y += 30f
        c.drawText(inv.companyName.ifEmpty { "Your Company" }, M, y,
            paint(Color.parseColor("#212121"), 15f, true))
        val sp = paint(Color.parseColor("#616161"), 8f)
        var sy2 = y + 14f
        if (inv.companyAddress.isNotEmpty()) { c.drawText(inv.companyAddress, M, sy2, sp); sy2 += 11f }
        if (inv.companyPhone.isNotEmpty()) { c.drawText("Ph: ${inv.companyPhone}", M, sy2, sp); sy2 += 11f }
        if (inv.companyGst.isNotEmpty()) c.drawText("GSTIN: ${inv.companyGst}", M, sy2,
            paint(Color.parseColor("#212121"), 9f, true))

        // Right side invoice details
        val rp = paint(Color.parseColor("#616161"), 9f, false, Paint.Align.RIGHT)
        val rBold = paint(Color.parseColor("#212121"), 10f, true, Paint.Align.RIGHT)
        c.drawText("Invoice No:", W - M - 75, y, rp);      c.drawText(inv.invoiceNumber, W - M, y, rBold)
        c.drawText("Date:", W - M - 75, y + 14f, rp);      c.drawText(inv.invoiceDate, W - M, y + 14f, rBold)
        if (inv.dueDate.isNotEmpty()) { c.drawText("Due:", W - M - 75, y + 28f, rp); c.drawText(inv.dueDate, W - M, y + 28f, rBold) }

        y = 115f
        val op = Paint().apply { color = Color.parseColor("#E65100"); strokeWidth = 1.5f }
        c.drawLine(M, y, W - M, y, op); y += 12f

        // Customer box
        val bx = fillPaint(Color.parseColor("#FFF8E1"))
        c.drawRoundRect(M, y, W / 2f - 10, y + 58f, 4f, 4f, bx)
        c.drawText("BILL TO", M + 8, y + 13f, paint(Color.parseColor("#E65100"), 8f, true))
        c.drawText(inv.customerName, M + 8, y + 26f, paint(Color.parseColor("#212121"), 12f, true))
        c.drawText(inv.customerAddress, M + 8, y + 38f, paint(Color.parseColor("#616161"), 8f))
        if (inv.customerGst.isNotEmpty())
            c.drawText("GSTIN: ${inv.customerGst}", M + 8, y + 50f, paint(Color.parseColor("#616161"), 8f))
        y += 68f
        c.drawLine(M, y, W - M, y, op); y += 14f
        return y
    }

    // ─── TEMPLATE 2: COMPACT RETAIL ─────────────────────────────────────────
    private fun drawCompact(c: Canvas, inv: InvoiceData, sy: Float, ctx: Context): Float {
        var y = sy
        c.drawText(inv.companyName.ifEmpty { "Your Shop" }, W / 2f, y + 20f,
            paint(Color.parseColor("#2E7D32"), 22f, true, Paint.Align.CENTER))
        y += 34f
        c.drawText("${inv.companyAddress}  |  ${inv.companyPhone}", W / 2f, y,
            paint(Color.parseColor("#555555"), 9f, false, Paint.Align.CENTER))
        if (inv.companyGst.isNotEmpty()) {
            y += 13f
            c.drawText("GSTIN: ${inv.companyGst}", W / 2f, y,
                paint(Color.parseColor("#555555"), 9f, false, Paint.Align.CENTER))
        }
        y += 10f
        val gp = Paint().apply { color = Color.parseColor("#2E7D32"); strokeWidth = 2f }
        c.drawLine(M, y, W - M, y, gp); y += 14f
        c.drawText("Invoice: ${inv.invoiceNumber}", M, y, paint(Color.parseColor("#333333"), 9f, true))
        c.drawText("Date: ${inv.invoiceDate}", W - M, y, paint(Color.parseColor("#333333"), 9f, false, Paint.Align.RIGHT))
        y += 13f
        c.drawText("Customer: ${inv.customerName}  ${inv.customerPhone}", M, y, paint(Color.parseColor("#333333"), 9f))
        y += 16f
        val tp = Paint().apply { color = Color.parseColor("#A5D6A7"); strokeWidth = 0.5f }
        c.drawLine(M, y, W - M, y, tp); y += 12f
        return y
    }

    // ─── ITEMS TABLE ─────────────────────────────────────────────────────────
    private fun drawItems(c: Canvas, inv: InvoiceData, startY: Float): Float {
        var y = startY
        // Header
        c.drawRect(M, y, W - M, y + 22f, fillPaint(Color.parseColor("#263238")))
        val hp = paint(Color.WHITE, 8f, true)
        val hpr = paint(Color.WHITE, 8f, true, Paint.Align.RIGHT)
        val cx = colX()
        c.drawText("#", cx[0], y + 15f, hp)
        c.drawText("ITEM", cx[1], y + 15f, hp)
        c.drawText("QTY", cx[2], y + 15f, hpr)
        c.drawText("RATE", cx[3], y + 15f, hpr)
        c.drawText("DISC%", cx[4], y + 15f, hpr)
        c.drawText("TAX%", cx[5], y + 15f, hpr)
        c.drawText("AMOUNT", W - M, y + 15f, hpr)
        y += 22f

        inv.items.forEachIndexed { i, item ->
            val rh = 20f
            val bg = if (i % 2 == 0) Color.WHITE else Color.parseColor("#F5F5F5")
            c.drawRect(M, y, W - M, y + rh, fillPaint(bg))
            val lp = Paint().apply { color = Color.parseColor("#E0E0E0"); strokeWidth = 0.5f }
            c.drawLine(M, y + rh, W - M, y + rh, lp)
            val tp = paint(Color.parseColor("#212121"), 9f)
            val tpr = paint(Color.parseColor("#212121"), 9f, false, Paint.Align.RIGHT)
            c.drawText("${i + 1}", cx[0], y + 13f, tp)
            val nm = if (item.name.length > 26) item.name.take(23) + "…" else item.name
            c.drawText(nm, cx[1], y + 13f, tp)
            c.drawText(fmtNum(item.quantity), cx[2], y + 13f, tpr)
            c.drawText(rupee.format(item.unitPrice), cx[3], y + 13f, tpr)
            c.drawText("${item.discountPercent}%", cx[4], y + 13f, tpr)
            c.drawText("${item.taxPercent}%", cx[5], y + 13f, tpr)
            c.drawText(rupee.format(item.lineTotal), W - M, y + 13f, tpr)
            y += rh
        }
        val bp = Paint().apply { color = Color.parseColor("#263238"); strokeWidth = 1f }
        c.drawLine(M, y, W - M, y, bp)
        return y + 14f
    }

    // ─── TOTALS ──────────────────────────────────────────────────────────────
    private fun drawTotals(c: Canvas, inv: InvoiceData, startY: Float) {
        var y = startY
        val lx = W - M - 165f
        val vx = W - M

        fun row(label: String, value: String, bold: Boolean = false, color: Int = Color.parseColor("#546E7A")) {
            c.drawText(label, lx, y, paint(color, if (bold) 11f else 9f, bold))
            c.drawText(value, vx, y, paint(if (bold) Color.parseColor("#212121") else Color.parseColor("#212121"),
                if (bold) 11f else 9f, bold, Paint.Align.RIGHT))
            y += if (bold) 16f else 14f
        }

        row("Subtotal:", rupee.format(inv.subtotal))
        if (inv.totalDiscount > 0) row("Discount:", "- ${rupee.format(inv.totalDiscount)}", color = Color.parseColor("#2E7D32"))
        if (inv.totalTax > 0) row("Tax (GST):", rupee.format(inv.totalTax))

        // Grand total box
        c.drawRect(lx - 8, y - 4f, vx + 4, y + 18f, fillPaint(Color.parseColor("#263238")))
        c.drawText("GRAND TOTAL:", lx, y + 11f, paint(Color.WHITE, 11f, true))
        c.drawText(rupee.format(inv.grandTotal), vx, y + 11f, paint(Color.parseColor("#FFB74D"), 13f, true, Paint.Align.RIGHT))
        y += 26f

        if (inv.amountPaid > 0) {
            row("Amount Paid:", rupee.format(inv.amountPaid))
            val balColor = if (inv.balanceDue > 0) Color.parseColor("#C62828") else Color.parseColor("#2E7D32")
            c.drawText("Balance Due:", lx, y, paint(balColor, 11f, true))
            c.drawText(rupee.format(inv.balanceDue), vx, y, paint(balColor, 12f, true, Paint.Align.RIGHT))
            y += 18f
        }

        if (inv.notes.isNotEmpty()) {
            y += 8f
            c.drawText("NOTES:", M, y, paint(Color.parseColor("#546E7A"), 8f, true))
            y += 12f
            c.drawText(inv.notes.take(120), M, y, paint(Color.parseColor("#757575"), 8f))
        }
    }

    private fun drawFooter(c: Canvas, inv: InvoiceData) {
        val y = H - 28f
        val lp = Paint().apply { color = Color.parseColor("#E0E0E0"); strokeWidth = 0.5f }
        c.drawLine(M, y - 10f, W - M, y - 10f, lp)
        c.drawText("Thank you for your business! — ${inv.companyName} | Generated by Phoenix Invoice",
            W / 2f, y, paint(Color.parseColor("#9E9E9E"), 7f, false, Paint.Align.CENTER))
    }

    private fun colX() = arrayOf(M, M + 20f, M + 235f, M + 285f, M + 340f, M + 388f)

    private fun fmtNum(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() else "%.2f".format(d)

    private fun loadBitmap(ctx: Context, uriStr: String, w: Int, h: Int): Bitmap? {
        return try {
            val uri = Uri.parse(uriStr)
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            val raw = ctx.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            raw?.let { Bitmap.createScaledBitmap(it, w, h, true) }
        } catch (e: Exception) { null }
    }
}
