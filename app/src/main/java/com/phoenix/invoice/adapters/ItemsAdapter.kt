package com.phoenix.invoice.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.phoenix.invoice.R
import com.phoenix.invoice.models.InvoiceItem
import java.text.NumberFormat
import java.util.*

class ItemsAdapter(
    private val items: MutableList<InvoiceItem>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<ItemsAdapter.VH>() {

    private val rupee = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val num: TextView = v.findViewById(R.id.tvItemNumber)
        val name: EditText = v.findViewById(R.id.etItemName)
        val qty: EditText = v.findViewById(R.id.etQuantity)
        val price: EditText = v.findViewById(R.id.etUnitPrice)
        val disc: EditText = v.findViewById(R.id.etDiscount)
        val tax: EditText = v.findViewById(R.id.etTax)
        val total: TextView = v.findViewById(R.id.tvLineTotal)
        val del: ImageButton = v.findViewById(R.id.btnRemoveItem)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_invoice_row, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        h.num.text = "${pos + 1}"

        // Remove all old watchers via tag
        listOf(h.name, h.qty, h.price, h.disc, h.tax).forEach { et ->
            (et.getTag(R.id.tag_watcher) as? TextWatcher)?.let { et.removeTextChangedListener(it) }
        }

        // Set text
        h.name.setTextKeepState(item.name)
        h.qty.setTextKeepState(fmtD(item.quantity))
        h.price.setTextKeepState(if (item.unitPrice > 0) fmtD(item.unitPrice) else "")
        h.disc.setTextKeepState(if (item.discountPercent > 0) fmtD(item.discountPercent) else "")
        h.tax.setTextKeepState(if (item.taxPercent > 0) fmtD(item.taxPercent) else "")
        h.total.text = rupee.format(item.lineTotal)

        // Attach new watchers
        fun watch(et: EditText, update: (String) -> Unit) {
            val w = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val ap = h.adapterPosition
                    if (ap == RecyclerView.NO_ID.toInt()) return
                    update(s?.toString() ?: "")
                    h.total.text = rupee.format(items[ap].lineTotal)
                    onChanged()
                }
            }
            et.setTag(R.id.tag_watcher, w)
            et.addTextChangedListener(w)
        }

        watch(h.name)  { items[h.adapterPosition].name = it }
        watch(h.qty)   { items[h.adapterPosition].quantity = it.toDoubleOrNull() ?: 1.0 }
        watch(h.price) { items[h.adapterPosition].unitPrice = it.toDoubleOrNull() ?: 0.0 }
        watch(h.disc)  { items[h.adapterPosition].discountPercent = it.toDoubleOrNull() ?: 0.0 }
        watch(h.tax)   { items[h.adapterPosition].taxPercent = it.toDoubleOrNull() ?: 0.0 }

        h.del.setOnClickListener {
            val ap = h.adapterPosition
            if (ap != RecyclerView.NO_ID.toInt() && items.size > 1) {
                items.removeAt(ap)
                notifyItemRemoved(ap)
                notifyItemRangeChanged(ap, items.size)
                onChanged()
            }
        }
    }

    private fun fmtD(d: Double) =
        if (d == d.toLong().toDouble()) d.toLong().toString() else "%.2f".format(d)

    private fun EditText.setTextKeepState(text: String) {
        if (this.text.toString() != text) setText(text)
    }
}
