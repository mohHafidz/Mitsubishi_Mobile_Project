package com.example.mitsubishi

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class GeneratePDFActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var generatePDFButton: Button
    private lateinit var shareButton: Button
    private lateinit var noTelp: EditText
    private lateinit var no_plat : TextView
    private lateinit var delete : TextView

    private companion object{
        private const val STORAGE_PERMISSION_CODE = 100
        private const val TAG = "PERMISSION_TAG"
    }

    private var pdfFilePath: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_pdf)

        val plat = intent.getStringExtra("no.pol") ?: "No plate provided"

        // Inisialisasi tampilan
        generatePDFButton = findViewById(R.id.generatePDFButton)
        shareButton = findViewById(R.id.sharePDFButton)
        noTelp = findViewById(R.id.phoneNumberTextField)
        no_plat = findViewById(R.id.plat)
        delete = findViewById(R.id.delete)

        no_plat.text = plat

        delete.setOnClickListener {
            showConfirmDialog(this,"Apakah anda yakin ingin menghapus data ini") {
                deleteDocumentByNoPolis(plat)
                finish()
            }

        }

        // Sembunyikan tombol generate PDF dan share PDF secara awal
//        generatePDFButton.visibility = View.GONE
        shareButton.visibility = View.GONE

        if (checkPermission()){
            Log.d(TAG, "onCreate: Permission already granted")
        }else{
            Log.d(TAG, "onCreate: Permission was not granted")
            requestPermission()
        }

        // Setel pendengar klik untuk tombol generate PDF
        generatePDFButton.setOnClickListener {
            val noPol = plat.toString().trim()
            generatePDF(noPol)
        }

        // Setel pendengar klik untuk tombol share
        shareButton.setOnClickListener {
            val phoneNumber = noTelp.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                val formattedPhoneNumber = if (phoneNumber.startsWith("0")) {
                    "62" + phoneNumber.substring(1) // Mengganti "0" di awal dengan "62"
                } else {
                    phoneNumber
                }

                pdfFilePath?.let { path ->
                    sharePDF(path, formattedPhoneNumber)
                }
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showConfirmDialog(context: Context, message: String, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Ya") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    fun deleteDocumentByNoPolis(noPolis: String) {
        // Query Firestore collection to find document with matching noPolis
        db.collection("cars")
            .whereEqualTo("noPolis", noPolis)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Delete each document found with the matching noPolis
                    db.collection("cars").document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            println("Document with noPolis: $noPolis deleted successfully!")
                        }
                        .addOnFailureListener { e ->
                            println("Error deleting document: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                println("Error finding document: ${e.message}")
            }
    }


    private fun requestPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            try {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                val uri = Uri.fromParts("package", this.packageName,null)
                intent.data = uri
                storageActivityResultLauncher.launch((intent))
            }
            catch (e: Exception){
                Log.e(TAG, "requestPermission: ", e)
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storageActivityResultLauncher.launch((intent))
            }
        }
        else{
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    private val storageActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (Environment.isExternalStorageManager()){
                Log.d(TAG, "Permission granted")
            } else {
                Log.d(TAG, "Permission denied")
                showToast("Manage External Storage Permission is Denied.")
            }
        }
    }

    private fun checkPermission(): Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            Environment.isExternalStorageManager()
        } else {
            val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "Permission granted")
            } else {
                Log.d(TAG, "Permission denied")
                showToast("External Storage Permission Denied.")
            }
        }
    }

    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun searchCarByNoPol(noPol: String) {
        db.collection("cars")
            .whereEqualTo("noPolis", noPol)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(this, "Data found for $noPol", Toast.LENGTH_SHORT).show()
                    generatePDFButton.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "No data found for $noPol", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("GeneratePDF", "Error fetching car details", e)
                Toast.makeText(this, "Failed to fetch car details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generatePDF(noPol: String) {
        db.collection("cars")
            .whereEqualTo("noPolis", noPol)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val model = document.getString("model") ?: "Unknown"
                    val totalPrice = document.getLong("Biaya") ?: 0

                    // Process photo list
                    val photoList = mutableListOf<Photo>()
                    val photos = document.get("photos") as? List<Map<String, Any>>
                    photos?.forEach { photoMap ->
                        val photo = Photo(
                            codeBarang = photoMap["codeBarang"] as? String ?: "",
                            description = photoMap["description"] as? String ?: "",
                            harga = (photoMap["harga"] as? Double)?.toLong() ?: 0L,
                            jumlah = photoMap["jumlah"],
                            totalPrice = (photoMap["totalPrice"] as? Double)?.toLong() ?: 0L,
                            url = photoMap["url"] as? String ?: "",
                            urgensi = photoMap["urgensi"] as? String ?: ""
                        )
                        photoList.add(photo)
                    }

                    // Get codeBarang from the first photo as an example (adjust as needed)
                    val codeBarang = photoList.firstOrNull()?.codeBarang ?: ""

                    // Use searchNamaPart with a callback to wait for namaPart asynchronously
                    searchNamaPart(codeBarang) { namaPart ->
                        val pdfGenerator = PDFGenerator(this)
                        pdfGenerator.createPDF(noPol, namaPart, model, totalPrice, photoList) { pdfFile ->
                            if (pdfFile != null && pdfFile.exists()) {
                                Toast.makeText(this, "PDF generated successfully", Toast.LENGTH_SHORT).show()
                                pdfFilePath = pdfFile.absolutePath

                                shareButton.visibility = View.VISIBLE
                                noTelp.visibility = View.VISIBLE
                            } else {
                                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("GeneratePDF", "Error generating PDF", e)
                Toast.makeText(this, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    fun searchNamaPart(noBarang: String, callback: (String) -> Unit) {
        db.collection("Spare Part")
            .whereEqualTo("Nomor Barang", noBarang)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Assuming there's only one matching document
                    val namaBarang = documents.documents[0].getString("Nama Barang") ?: "Unknown"
                    callback(namaBarang)
                } else {
                    callback("Unknown")
                }
            }
            .addOnFailureListener { e ->
                Log.e("GeneratePDF", "Error fetching car details", e)
                Toast.makeText(this, "Failed to fetch car details: ${e.message}", Toast.LENGTH_SHORT).show()
                callback("Unknown")
            }
    }


    private fun sharePDF(filePath: String, phoneNumber: String) {
        val file = File(filePath)
        val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("jid", "$phoneNumber@s.whatsapp.net") // Format nomor WhatsApp
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        intent.setPackage("com.whatsapp")

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWhatsappContact(filePath: String, number: String) {
        val file = File(filePath)
        val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("jid", "$number@s.whatsapp.net")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        intent.setPackage("com.whatsapp")

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent) // Kirim langsung ke WhatsApp tanpa chooser
        } else {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
