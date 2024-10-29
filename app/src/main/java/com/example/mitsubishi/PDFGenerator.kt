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

class PDFGenerator(private val context: Context) {
    // Function to create a PDF file and return the generated File
    fun createPDF(noPol: String, model: String, urgensi: String, status: Boolean, photos: MutableList<Photo>): File? {
        return try {
            // Path to save the PDF
            val pdfPath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.toString()
            if (pdfPath == null) {
                Log.e("PDFGenerator", "Failed to get the directory for saving PDF.")
                return null
            }
            val file = File(pdfPath, "$noPol.pdf")

            // Create output stream and write PDF
            FileOutputStream(file).use { outputStream ->
                // Initialize PdfWriter and Document
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)

                // Add title
                document.add(Paragraph("Car Details").setBold().setFontSize(20f))

                // Create a table with 4 columns
                val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f, 1f, 1f)))
                table.setWidth(UnitValue.createPercentValue(100f))

                // Add table headers
                table.addCell(Cell().add(Paragraph("No Polisi").setBold()))
                table.addCell(Cell().add(Paragraph("Model").setBold()))
                table.addCell(Cell().add(Paragraph("Urgensi").setBold()))
                table.addCell(Cell().add(Paragraph("Status").setBold()))

                // Add data to the table
                table.addCell(noPol)
                table.addCell(model)
                table.addCell(urgensi)
                table.addCell(if (status) "Active" else "Inactive")

                // Add the table to the PDF document
                document.add(table)

                // Add photos if available
                photos.forEach { photo ->
                    Log.d("PDFGenerator", "Photo Code Barang: ${photo.codeBarang}")
                    Log.d("PDFGenerator", "Photo Description: ${photo.description}")

                    // Adding title or description of the photo if available
                    if (!photo.codeBarang.isNullOrEmpty()) {
                        document.add(Paragraph("Title: ${photo.codeBarang}").setBold())
                    }
                    if (!photo.description.isNullOrEmpty()) {
                        document.add(Paragraph("Description: ${photo.description}"))
                    }

                    val photoUrl = photo.url
                    Log.d("PDFGenerator", "Photo URL: $photoUrl")

                    if (!photoUrl.isNullOrEmpty()) {
                        // Use Glide to load the image into a Bitmap
                        Glide.with(context)
                            .asBitmap()
                            .load(photoUrl)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    // Convert Bitmap to ByteArray
                                    val stream = ByteArrayOutputStream()
                                    resource.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    val byteArray = stream.toByteArray()

                                    // Create ImageData from ByteArray
                                    val imageData = ImageDataFactory.create(byteArray)
                                    val image = Image(imageData).apply {
                                        setWidth(UnitValue.createPercentValue(50f))
                                        setAutoScaleHeight(true)
                                    }
                                    // Add image to the document
                                    document.add(image)
                                    Log.d("PDFGenerator", "Image added successfully.")
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // Handle cleanup if necessary
                                }

                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    Log.e("PDFGenerator", "Failed to load image from URL: $photoUrl")
                                }
                            })
                    } else {
                        Log.e("PDFGenerator", "Photo URL is null or empty")
                    }
                }

                // Close the document after completion
                document.close()
                Log.d("PDFGenerator", "PDF created successfully for $noPol")
                file // Return the generated file
            }
        } catch (e: Exception) {
            Log.e("PDFGenerator", "Error creating PDF: $e")
            null
        }
    }
}
