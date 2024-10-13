package com.example.mitsubishi

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class AddPhotoAdapter(private val photoList: ArrayList<Bitmap>, private val descriptions: MutableList<String>) :
    RecyclerView.Adapter<AddPhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image)
        var keterangan: EditText = view.findViewById(R.id.ketPhoto)
        var delete: Button = view.findViewById(R.id.delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.photolist, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, @SuppressLint("RecyclerView") position: Int) {
        // Set image bitmap
        holder.imageView.setImageBitmap(photoList[position])

        holder.keterangan.setText(descriptions[position])

        holder.keterangan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                descriptions[position] = s.toString() // Update the description in the list
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Set click listener for the delete button
        holder.delete.setOnClickListener {
            // Remove the photo from the list
            photoList.removeAt(position)
            // Notify the adapter that the item has been removed
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, photoList.size)
        }
    }

    override fun getItemCount(): Int {
        return photoList.size
    }

}
