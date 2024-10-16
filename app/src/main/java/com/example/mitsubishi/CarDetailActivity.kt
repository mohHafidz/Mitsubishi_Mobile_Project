package com.example.mitsubishi

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class CarDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_detail)

        val noPolTextView = findViewById<TextView>(R.id.noPolTextView)
        val modelTextView = findViewById<TextView>(R.id.modelTextView)
        val statusTextView = findViewById<TextView>(R.id.statusTextView)
        val photoImageView = findViewById<ImageView>(R.id.carPhotoImageView)

        // Get the carId from the intent
        val carId = intent.getStringExtra("CAR_ID")

        if (carId != null) {
            // Fetch the car details from Firestore
            db.collection("cars").document(carId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val noPol = document.getString("noPolis") ?: "Unknown"
                        val model = document.getString("model") ?: "Unknown"
                        val status = document.getString("status") ?: "Unknown"
                        val photos = document.get("photos") as? List<Map<String, String>>

                        // Set the retrieved data to the UI
                        noPolTextView.text = noPol
                        modelTextView.text = model
                        statusTextView.text = status

                        // Display the first image from the photos list (you can modify this for multiple photos)
                        if (!photos.isNullOrEmpty()) {
                            val photoUrl = photos[0]["url"]
                            if (photoUrl != null) {
                                Glide.with(this)
                                    .load(photoUrl)
                                    .into(photoImageView)
                            } else {
                                Toast.makeText(this, "Photo URL is missing", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "No photos available", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Car not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CarDetail", "Error fetching car details", e)
                    Toast.makeText(this, "Failed to fetch car details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "No car ID provided", Toast.LENGTH_SHORT).show()
        }
    }
}
