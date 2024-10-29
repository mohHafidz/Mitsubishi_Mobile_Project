package com.example.mitsubishi

data class Photo(

    val codeBarang: String,
    val description: String,
    val harga: Long,
    val jumlah: Any?,
    val totalPrice: Long,
    val url: String
)

data class Data_Car (
    val Biaya : Long,
    val model : String,
    val noPolis : String,
    val photo : List<Photo>
)