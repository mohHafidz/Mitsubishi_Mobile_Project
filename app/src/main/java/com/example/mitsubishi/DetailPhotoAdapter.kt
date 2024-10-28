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

        private var quantity = 0

        init {
            val adapter = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, suggestions)
            kodeBarang.setAdapter(adapter)
            kodeBarang.threshold = 1

            decreaseButton.setOnClickListener {
                if (quantity > 0) {
                    quantity--
                    updateQuantityDisplay()
                }
            }

            increaseButton.setOnClickListener {
                quantity++
                updateQuantityDisplay()
            }

            kodeBarang.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // Save codeBarang input to photoItem when changed
                    s?.let { photoItems[adapterPosition].codeBarang = it.toString() }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Custom search or filtering logic could go here
                }
            })
        }

        fun bind(photoItem: PhotoItem) {
            Glide.with(itemView.context).load(photoItem.url).into(imageView)
            descriptionView.text = photoItem.description

            // Initialize quantity from photoItem if photoItem has quantity data
            quantity = photoItem.jumlah
            updateQuantityDisplay()
        }

        private fun updateQuantityDisplay() {
            quantityTextView.text = "Quantity: $quantity"
            photoItems[adapterPosition].jumlah = quantity // Update photoItem's quantity value
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
