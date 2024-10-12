package com.example.mitsubishi

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AddCarActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var photoAdapter: AddPhotoAdapter
    private val photoList = ArrayList<Bitmap>()

    private val CAMERA_REQUEST_CODE = 1
    private val CAMERA_PERMISSION_CODE = 100

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car)

        val add = findViewById<Button>(R.id.Add_BTN)
        val nopolET = findViewById<EditText>(R.id.Nopo_ET)
        val modelET = findViewById<EditText>(R.id.Model_ET)
        val addPhotoButton = findViewById<Button>(R.id.addPhotoBTN)
        val recyclerView = findViewById<RecyclerView>(R.id.ListPhoto)

        photoAdapter = AddPhotoAdapter(photoList)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = photoAdapter



        addPhotoButton.setOnClickListener {
            // Periksa izin kamera sebelum membuka kamera
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Minta izin kamera jika belum diberikan
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            } else {
                // Jika izin sudah diberikan, buka kamera
                openCamera()
            }
        }

        add.setOnClickListener {
            val nopol = nopolET.text.toString().trim()
            val model = modelET.text.toString().trim()

            if (nopol.isEmpty() || model.isEmpty()) {
                Toast.makeText(this, "Tolong isi semua field", Toast.LENGTH_SHORT).show()
            } else {
                val mobil = hashMapOf(
                    "polisi" to nopol,
                    "Model" to model,
                    "status" to "prediksi"
                )

                // Tambahkan data ke Firestore
                db.collection("costumer")
                    .add(mobil)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Jika izin diberikan, buka kamera
                openCamera()
            } else {
                // Jika izin ditolak
                Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val photo = data?.extras?.get("data") as? Bitmap
            if (photo != null) {
                // Tambahkan foto ke dalam daftar dan perbarui RecyclerView
                photoList.add(photo)
                photoAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
