package com.example.mitsubishi

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class DetailPhotoAdapter(
    private var photoItems: List<PhotoItem>,
    private var suggestions: List<String> // Make suggestions mutable
) : RecyclerView.Adapter<DetailPhotoAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
        private val descriptionView: TextView = itemView.findViewById(R.id.desc_tv)
        private val kodeBarang: AutoCompleteTextView = itemView.findViewById(R.id.sparePart_tf)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        private val decreaseButton: Button = itemView.findViewById(R.id.decreaseButton)
        private val increaseButton: Button = itemView.findViewById(R.id.increaseButton)
        private val hargaBarang: TextView = itemView.findViewById(R.id.cost_tv)

        private var quantity = 0
        private var hargaEceran: Double? = null // Store the unit price

        init {
            val adapter = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, suggestions)
            kodeBarang.setAdapter(adapter)
            kodeBarang.threshold = 1

            decreaseButton.setOnClickListener {
                if (quantity > 0) {
                    quantity--
                    updateQuantityDisplay()
                    updateHargaBarangDisplay() // Update total price based on new quantity
                }
            }

            increaseButton.setOnClickListener {
                quantity++
                updateQuantityDisplay()
                updateHargaBarangDisplay() // Update total price based on new quantity
            }

            kodeBarang.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    s?.let {
                        val codeBarang = it.toString()
                        photoItems[adapterPosition].codeBarang = codeBarang

                        // Fetch the price and update UI
                        fetchHargaEceran(codeBarang) { harga ->
                            if (harga != null) {
                                hargaEceran = harga // Store the unit price
                                updateHargaBarangDisplay() // Update total price based on current quantity
                            } else {
                                hargaBarang.text = "Harga tidak ditemukan"
                            }
                        }
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        fun bind(photoItem: PhotoItem) {
            Glide.with(itemView.context).load(photoItem.url).into(imageView)
            descriptionView.text = photoItem.description

            quantity = photoItem.jumlah
            updateQuantityDisplay()

            // Set initial `Harga Eceran` if codeBarang is available
            val codeBarang = photoItem.codeBarang
            if (codeBarang.isNotEmpty()) {
                fetchHargaEceran(codeBarang) { harga ->
                    hargaEceran = harga
                    updateHargaBarangDisplay() // Update total price based on current quantity
                }
            }
        }

        private fun updateQuantityDisplay() {
            quantityTextView.text = "Quantity: $quantity"
            photoItems[adapterPosition].jumlah = quantity // Update photoItem's quantity value
        }

        private fun updateHargaBarangDisplay() {
            val totalHarga = hargaEceran?.times(quantity) ?: 0.0
            hargaBarang.text = formatToRupiah(totalHarga) // Format the price
            photoItems[adapterPosition].harga = totalHarga
        }
        private fun formatToRupiah(amount: Double): String {
            val localeID = Locale("in", "ID") // Locale for Indonesia
            val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
            return formatRupiah.format(amount)
        }
    }



    private fun fetchHargaEceran(codeBarang: String, onResult: (Double?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Spare Part")
            .whereEqualTo("Nomor Barang", codeBarang)
            .get()
            .addOnSuccessListener { documents ->
                val hargaEceran = documents.firstOrNull()?.getDouble("Harga Eceran")
                onResult(hargaEceran)
            }
            .addOnFailureListener {
                onResult(null) // Return null if there's an error fetching the price
            }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(photoItems[position])
    }

    override fun getItemCount(): Int {
        return photoItems.size
    }

    // Tambahkan metode untuk memperbarui saran
    fun updateSuggestions(newSuggestions: List<String>) {
        suggestions = newSuggestions // Update the suggestions list
        notifyDataSetChanged() // Notify the adapter to refresh
    }
}
