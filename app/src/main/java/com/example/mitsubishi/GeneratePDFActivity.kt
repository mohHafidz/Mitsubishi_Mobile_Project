package com.example.mitsubishi

import android.Manifest
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
    private lateinit var searchButton: Button
    private lateinit var generatePDFButton: Button
    private lateinit var shareButton: Button
    private lateinit var noPolTextField: EditText
    private lateinit var noTelp: EditText

    private companion object{
        private const val STORAGE_PERMISSION_CODE = 100
        private const val TAG = "PERMISSION_TAG"
    }

    private var pdfFilePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_pdf)

        // Inisialisasi tampilan
        noPolTextField = findViewById(R.id.noPolTextField)
        searchButton = findViewById(R.id.searchButton)
        generatePDFButton = findViewById(R.id.generatePDFButton)
        shareButton = findViewById(R.id.sharePDFButton)
        noTelp = findViewById(R.id.phoneNumberTextField)

        // Sembunyikan tombol generate PDF dan share PDF secara awal
        generatePDFButton.visibility = View.GONE
        shareButton.visibility = View.GONE

        if (checkPermission()){
            Log.d(TAG, "onCreate: Permission already granted")
        }else{
            Log.d(TAG, "onCreate: Permission was not granted")
            requestPermission()
        }

        // Setel pendengar klik untuk tombol pencarian
        searchButton.setOnClickListener {
            val noPol = noPolTextField.text.toString().trim()
            if (noPol.isNotEmpty()) {
                searchCarByNoPol(noPol)
            } else {
                Toast.makeText(this, "Please enter a car number", Toast.LENGTH_SHORT).show()
            }
        }

        // Setel pendengar klik untuk tombol generate PDF
        generatePDFButton.setOnClickListener {
            val noPol = noPolTextField.text.toString().trim()
            generatePDF(noPol)
        }

        // Setel pendengar klik untuk tombol share
        shareButton.setOnClickListener {
            pdfFilePath?.let { path -> sharePDF(path) }
            val phoneNumber = noTelp.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                openWhatsappContact(phoneNumber)
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
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
                    val urgensi = document.getString("urgensi") ?: "Unknown"
                    val status = document.getBoolean("status") ?: false

                    val photoList = mutableListOf<Photo>()
                    val photos = document.get("photos") as? List<Map<String, Any>>

                    photos?.forEach { photoMap ->
                        val photo = Photo(
                            codeBarang = photoMap["codeBarang"] as? String ?: "",
                            description = photoMap["description"] as? String ?: "",
                            harga = (photoMap["harga"] as? Double)?.toLong() ?: 0L,
                            jumlah = (photoMap["jumlah"]),
                            totalPrice = (photoMap["totalPrice"] as? Double)?.toLong() ?: 0L,
                            url = photoMap["url"] as? String ?: ""
                        )

                        photoList.add(photo)
                    }


                    val pdfGenerator = PDFGenerator(this)
                    pdfGenerator.createPDF(noPol, model, urgensi, status, photoList) { pdfFile ->
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
            .addOnFailureListener { e ->
                Log.e("GeneratePDF", "Error generating PDF", e)
                Toast.makeText(this, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sharePDF(filePath: String) {
        val file = File(filePath)
        val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
    }

    private fun openWhatsappContact(number: String) {
        val file = File(pdfFilePath ?: return)
        val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("jid", "$number@s.whatsapp.net")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        intent.setPackage("com.whatsapp")

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, "Send PDF via WhatsApp"))
        } else {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
