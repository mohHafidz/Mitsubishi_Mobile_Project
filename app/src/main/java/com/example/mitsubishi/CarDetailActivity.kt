package com.example.mitsubishi

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class CarDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var photoItems: List<PhotoItem> = emptyList() // Tambahkan photoItems sebagai variabel global

    private lateinit var urgencyRadioGroup: RadioGroup
    private var selectedUrgency: String = "Comfort" // Default value
    private var sparePartSuggestions: List<String> = emptyList()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_detail)

        val noPolTextView = findViewById<TextView>(R.id.noPolTextView)
        val modelTextView = findViewById<TextView>(R.id.modelTextView)
        val statusTextView = findViewById<TextView>(R.id.status_tv)
        val confirmButton = findViewById<Button>(R.id.confirm_Btn)
        val carPhotosRecyclerView = findViewById<RecyclerView>(R.id.carPhotosRecyclerView)

        carPhotosRecyclerView.layoutManager = LinearLayoutManager(this)

        val carId = intent.getStringExtra("NO_POLISI")
        if (carId != null) {
            Log.d("CarDetail", "Fetching data for car ID: $carId")
            fetchCarDetails(carId, noPolTextView, modelTextView, statusTextView, confirmButton, carPhotosRecyclerView)
        } else {
            Toast.makeText(this, "Invalid car ID", Toast.LENGTH_SHORT).show()
        }

        urgencyRadioGroup = findViewById(R.id.urgensiRadioGroup)
        urgencyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedUrgency = when (checkedId) {
                R.id.comfortRadioButton -> "Comfort"
                R.id.safetyRadioButton -> "Safety"
                else -> "Comfort" // Default case
            }
        }
        fetchSparePartsSuggestions { suggestions ->
            sparePartSuggestions = suggestions // Store fetched suggestions in the global list
        }
    }

    private fun fetchCarDetails(
        carId: String,
        noPolTextView: TextView,
        modelTextView: TextView,
        statusTextView: TextView,
        confirmButton: Button,
        carPhotosRecyclerView: RecyclerView
    ) {
        db.collection("cars")
            .whereEqualTo("noPolis", carId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Car not found", Toast.LENGTH_SHORT).show()
                } else {
                    for (document in documents) {
                        val noPol = document.getString("noPolis") ?: "Unknown"
                        val model = document.getString("model") ?: "Unknown"
                        val status = document.getBoolean("status") ?: false
                        val photos = document.get("photos") as? List<Map<String, String>>

                        noPolTextView.text = noPol
                        modelTextView.text = model
                        statusTextView.text = if (status) "Pending" else "Confirm"

                        val documentId = document.id

                        confirmButton.setOnClickListener {
                            // Check if urgency is selected
                            val selectedUrgency = when (urgencyRadioGroup.checkedRadioButtonId) {
                                R.id.comfortRadioButton -> "Comfort"
                                R.id.safetyRadioButton -> "Safety"
                                else -> null
                            }

                            when {
                                // Validate each codeBarang against the spare part suggestions
                                photoItems.any { it.codeBarang !in sparePartSuggestions } -> {
                                    Toast.makeText(this, "Invalid codeBarang. Please ensure all items are valid.", Toast.LENGTH_SHORT).show()
                                }

                                // Ensure 'jumlah' is greater than 0 for all items
                                photoItems.any { it.jumlah <= 0 } -> {
                                    Toast.makeText(this, "Quantity must be greater than 0 for all items.", Toast.LENGTH_SHORT).show()
                                }

                                // Check if urgency is selected
                                selectedUrgency == null -> {
                                    Toast.makeText(this, "Please select an urgency (Comfort or Safety).", Toast.LENGTH_SHORT).show()
                                }

                                // If all validations pass, proceed to show confirmation dialog
                                else -> {
                                    showConfirmationDialog(status, documentId, statusTextView)
                                }
                            }
                        }

                        photoItems = photos?.mapNotNull {
                            val url = it["url"] as? String
                            val description = it["description"] as? String
                            val codeBarang = it["codeBarang"] as? String
                            val jumlah = (it["jumlah"] as? Number)?.toInt() ?: 0

                            if (url != null && description != null && codeBarang != null) {
                                PhotoItem(url, description, codeBarang, jumlah)
                            } else {
                                null
                            }
                        } ?: emptyList()

                        fetchSparePartsSuggestions { suggestions ->
                            val adapter = DetailPhotoAdapter(photoItems, suggestions)
                            carPhotosRecyclerView.adapter = adapter
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("CarDetail", "Error fetching car details: ", exception)
                Toast.makeText(this, "Error fetching car details: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCarStatus(documentId: String, newStatus: Boolean, updatedPhotos: List<Map<String, Any>>) {
        val updates = mapOf(
            "status" to newStatus,
            "photos" to updatedPhotos,
            "urgensi" to selectedUrgency // Add urgency to Firestore
        )

        Log.d("CarDetail", "Updating status for document ID: $documentId to $newStatus")
        db.collection("cars").document(documentId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("CarDetail", "Car status updated successfully")
                Toast.makeText(this, "Status updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("CarDetail", "Error updating car status: ", exception)
                Toast.makeText(this, "Error updating status: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchSparePartsSuggestions(onSuccess: (List<String>) -> Unit) {
        db.collection("Spare Part")
            .get()
            .addOnSuccessListener { documents ->
                val suggestions = documents.mapNotNull { it.getString("Nomor Barang") }
                Log.d("CarDetail", "Spare parts suggestions: $suggestions")
                onSuccess(suggestions)
            }
            .addOnFailureListener { exception ->
                Log.e("CarDetail", "Error fetching spare parts: ", exception)
                Toast.makeText(this, "Error fetching spare parts: ${exception.message}", Toast.LENGTH_SHORT).show()
                onSuccess(emptyList())
            }
    }

    private fun showConfirmationDialog(currentStatus: Boolean, documentId: String, statusTextView: TextView) {
        if (currentStatus) {
            Toast.makeText(this, "Status sudah Active dan tidak dapat diubah lagi.", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi Status")
        builder.setMessage("Apakah Anda yakin ingin mengubah status mobil ini?")

        builder.setPositiveButton("Ya") { dialog, _ ->
            val newStatus = !currentStatus
            statusTextView.text = if (newStatus) "Active" else "Inactive"

            val updatedPhotos = photoItems.map {
                mapOf(
                    "url" to it.url,
                    "description" to it.description,
                    "codeBarang" to it.codeBarang,
                    "jumlah" to it.jumlah
                )
            }

            updateCarStatus(documentId, newStatus, updatedPhotos)
            Toast.makeText(this, "Status berhasil diperbarui", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            finish()
        }

        builder.setNegativeButton("Tidak") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }
}
