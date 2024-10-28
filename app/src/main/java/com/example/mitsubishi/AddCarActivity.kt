package com.example.mitsubishi

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.util.UUID

class AddCarActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var photoAdapter: AddPhotoAdapter
    private val photoList = ArrayList<Bitmap>()
    private val descriptions = MutableList(0) { "" }
    private lateinit var blurOverlay : FrameLayout
    private var progressDialog: ProgressDialog? = null

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
        blurOverlay = findViewById(R.id.blurOverlay);

        progressDialog = ProgressDialog(this).apply {
            setMessage("Uploading data...")
            setCancelable(false)
        }

        photoAdapter = AddPhotoAdapter(photoList, descriptions)
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

            if (nopol.isEmpty() || model.isEmpty() || photoList.isEmpty()) {
                Toast.makeText(this, "Tolong isi semua field dan tambahkan foto", Toast.LENGTH_SHORT).show()
            } else {
                showProgressDialog()
                uploadPhotosAndSaveCar(nopol, model)
            }
        }
    }

    private fun showProgressDialog() {
        blurOverlay.visibility = View.VISIBLE // Show blur effect
        progressDialog!!.show() // Show loading dialog
    }

    private fun hideProgressDialog() {
        progressDialog!!.dismiss() // Hide loading dialog
        blurOverlay.visibility = View.GONE // Hide blur effect
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadPhotosAndSaveCar(nopol: String, model: String) {
        val carId = UUID.randomUUID().toString()
        val photoDataList = mutableListOf<Map<String, String>>()

        for (i in photoList.indices) {
            val bitmap = photoList[i]
            val imageRef: StorageReference = storage.reference.child("car_photos/$carId/image_$i.jpg")

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            val description = descriptions[i]

            imageRef.putBytes(data)
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Ambil deskripsi dari list
                        val photoData = mapOf(
                            "url" to downloadUrl.toString(),
                            "description" to description.ifEmpty { "Deskripsi gambar ke-${i + 1}" }, // Menggunakan deskripsi dari input pengguna
                            "codeBarang" to "",
                            "jumlah" to "0"
                        )
                        photoDataList.add(photoData)

                        // Simpan data mobil ke Firestore setelah semua gambar berhasil di-upload
                        if (photoDataList.size == photoList.size) {
                            val carData = hashMapOf(
                                "noPolis" to nopol,
                                "model" to model,
                                "photos" to photoDataList,
                                "status" to false
                            )
                            db.collection("cars").document(carId)
                                .set(carData)
                                .addOnSuccessListener {
                                    hideProgressDialog()
                                    Toast.makeText(this, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                }
                                .addOnFailureListener { e ->
                                    hideProgressDialog()
                                    hideProgressDialog()
                                    Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal mengupload gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
                openCamera()
            } else {
                Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val photo = data?.extras?.get("data") as? Bitmap
            if (photo != null) {
                // Tambahkan foto ke daftar
                photoList.add(photo)
                // Tambahkan deskripsi default atau ambil dari EditText jika diperlukan
                descriptions.add("") // Anda bisa menambahkan logika untuk menginput deskripsi di sini
                photoAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
