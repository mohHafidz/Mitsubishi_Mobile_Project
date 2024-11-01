package com.example.mitsubishi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PDFGenerator(private val context: Context) {

    fun createPDF(noPol: String, namaPart:String, model: String, totalPrice: Long, photos: MutableList<Photo>, onComplete: (File?) -> Unit) {
        // Path untuk menyimpan PDF

        val tanggalSekarang = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val tanggalFormatted = tanggalSekarang.format(formatter)
        val pdfPath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.toString()
        if (pdfPath == null) {
            Log.e("PDFGenerator", "Failed to get the directory for saving PDF.")
            onComplete(null)
            return
        }
        val file = File(pdfPath, "$noPol.pdf")

        // Ambil semua gambar terlebih dahulu, baru buat PDF
        loadPhotos(photos) { loadedPhotos ->
            try {
                FileOutputStream(file).use { outputStream ->
                    val pdfWriter = PdfWriter(outputStream)
                    val pdfDocument = PdfDocument(pdfWriter)
                    val document = Document(pdfDocument)

                    // Tambahkan Judul
                    document.add(Paragraph("Car Details").setBold().setFontSize(20f))

                    // Buat tabel data kendaraan
                    val tableHeader = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f,1f)))
                    tableHeader.setWidth(UnitValue.createPercentValue(100f))
                    tableHeader.addCell(Cell().add(Paragraph("No Polisi").setBold()))
                    tableHeader.addCell(Cell().add(Paragraph("Model").setBold()))
                    tableHeader.addCell(Cell().add(Paragraph("Tanggal").setBold()))
                    tableHeader.addCell(Cell().add(Paragraph("Total Price").setBold()))
                    tableHeader.addCell(noPol)
                    tableHeader.addCell(model)
                    tableHeader.addCell(tanggalFormatted)
                    tableHeader.addCell(totalPrice.toString())

                    // Tambahkan header tabel ke PDF
                    document.add(tableHeader)

                    // Tambahkan jarak antara tableHeader dan partTable
                    document.add(Paragraph(" ")) // Menambahkan paragraf kosong untuk jarak


                    // Buat tabel untuk data bagian (parts)
                    val partTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f, 2f, 2f, 1f, 1f, 1f, 1f, 2f)))
                    partTable.setWidth(UnitValue.createPercentValue(100f))

                    // Header untuk tabel parts
                    partTable.addCell(Cell().add(Paragraph("NO").setBold()))
                    partTable.addCell(Cell().add(Paragraph("FOTO PART").setBold()))
                    partTable.addCell(Cell().add(Paragraph("NO PART").setBold()))
                    partTable.addCell(Cell().add(Paragraph("NAMA PART").setBold()))
                    partTable.addCell(Cell().add(Paragraph("HARGA").setBold()))
                    partTable.addCell(Cell().add(Paragraph("QTY").setBold()))
                    partTable.addCell(Cell().add(Paragraph("TOTAL").setBold()))
                    partTable.addCell(Cell().add(Paragraph("FAKTOR URGENSI").setBold()))
                    partTable.addCell(Cell().add(Paragraph("KETERANGAN").setBold()))

                    // Ukuran gambar dalam dp
                    val imageWidthPx = dpToPx(50, context)  // Konversi 50dp ke px
                    val imageHeightPx = dpToPx(50, context) // Konversi 50dp ke px

                    // Tambahkan setiap foto beserta detailnya
                    loadedPhotos.forEachIndexed { index, (photo, bitmap) ->
                        // NO
                        partTable.addCell((index + 1).toString())

                        // FOTO PART
                        bitmap?.let {
                            val stream = ByteArrayOutputStream()
                            it.compress(Bitmap.CompressFormat.PNG, 100, stream) // Kualitas tinggi
                            val byteArray = stream.toByteArray()
                            val imageData = ImageDataFactory.create(byteArray)
                            val image = Image(imageData).apply {
                                setWidth(imageWidthPx) // Tetapkan ukuran lebar
                                setHeight(imageHeightPx) // Tetapkan ukuran tinggi
                            }
                            partTable.addCell(Cell().add(image))
                        } ?: partTable.addCell("") // Jika tidak ada gambar


                        // NO PART
                        partTable.addCell(photo.codeBarang ?: "")

                        // NAMA PART
                        partTable.addCell(namaPart ?: "")

                        // HARGA
                        partTable.addCell(photo.harga.toString() ?: "")

                        // QTY
                        partTable.addCell(photo.jumlah.toString() ?: "")

                        // TOTAL
                        partTable.addCell(photo.totalPrice.toString() ?: "")

                        // FAKTOR URGENSI
                        partTable.addCell(photo.urgensi ?: "")

                        // KETERANGAN
                        partTable.addCell(photo.description ?: "")
                    }

                    document.add(partTable)

                    // Tutup dokumen setelah selesai
                    document.close()
                    Log.d("PDFGenerator", "PDF created successfully for $noPol")
                    onComplete(file) // Kembalikan file yang dihasilkan
                }
            } catch (e: Exception) {
                Log.e("PDFGenerator", "Error creating PDF: $e")
                onComplete(null)
            }
        }
    }

    // Fungsi untuk memuat semua gambar dari URL menggunakan Glide
    private fun loadPhotos(photos: List<Photo>, onPhotosLoaded: (List<Pair<Photo, Bitmap?>>) -> Unit) {
        val loadedPhotos = mutableListOf<Pair<Photo, Bitmap?>>()
        var photosProcessed = 0

        photos.forEach { photo ->
            val photoUrl = photo.url
            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .asBitmap()
                    .load(photoUrl)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            loadedPhotos.add(Pair(photo, resource))
                            photosProcessed++
                            if (photosProcessed == photos.size) {
                                onPhotosLoaded(loadedPhotos)
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            photosProcessed++
                            if (photosProcessed == photos.size) {
                                onPhotosLoaded(loadedPhotos)
                            }
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            Log.e("PDFGenerator", "Failed to load image from URL: $photoUrl")
                            loadedPhotos.add(Pair(photo, null))
                            photosProcessed++
                            if (photosProcessed == photos.size) {
                                onPhotosLoaded(loadedPhotos)
                            }
                        }
                    })
            } else {
                loadedPhotos.add(Pair(photo, null))
                photosProcessed++
                if (photosProcessed == photos.size) {
                    onPhotosLoaded(loadedPhotos)
                }
            }
        }
    }

    // Fungsi untuk konversi dp ke px
    private fun dpToPx(dp: Int, context: Context): Float {
        return dp * context.resources.displayMetrics.density
    }
}