package com.example.mitsubishi

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class AddPhotoAdapter(private val photoList: ArrayList<Bitmap>) :
    RecyclerView.Adapter<AddPhotoAdapter.PhotoViewHolder>() { // Perbaikan: Ubah "PhotoAdapter" menjadi "AddPhotoAdapter"

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.photolist, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.imageView.setImageBitmap(photoList[position])
    }

    override fun getItemCount(): Int {
        return photoList.size
    }
}