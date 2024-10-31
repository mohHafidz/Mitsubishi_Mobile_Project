package com.example.mitsubishi

import CarListAdapter
import android.annotation.SuppressLint

import android.content.Intent

import android.os.Bundle

import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge

import androidx.appcompat.app.AppCompatActivity

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), CarListAdapter.OnItemClickListener {
    private lateinit var addCar: FloatingActionButton
    private lateinit var adapter: CarListAdapter
    private val db = FirebaseFirestore.getInstance()
    private val itemList = mutableListOf<car>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchET : EditText
    private lateinit var searchBTN : ImageView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        searchET = findViewById(R.id.noPolisET)
        searchBTN = findViewById(R.id.search)

        searchBTN.setOnClickListener {
            val nopol = searchET.text
            searchCar(nopol.toString())
        }

        // Inisialisasi SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Inisialisasi RecyclerView
        recyclerView = findViewById(R.id.carList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CarListAdapter(itemList, this) // Pass 'this' as the listener
        recyclerView.adapter = adapter

        // Inisialisasi FloatingActionButton
        addCar = findViewById(R.id.addCar)

        // Fetch data dari Firestore
        fetchDataFromFirestore()

        // Setup FloatingActionButton
        addCar.setOnClickListener {
            startActivity(Intent(this, AddCarActivity::class.java))
        }

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            fetchDataFromFirestore() // Fetch data from Firestore
        }
    }

    private fun searchCar(noPolis: String) {
        db.collection("cars")
            .whereEqualTo("noPolis", noPolis)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    itemList.clear()
                    for (document in documents) {
                        val status = document.getBoolean("status") ?: false
                        itemList.add(car(noPolis, status)) // Asumsi class Car sudah ada
                    }
                } else {
                    Toast.makeText(this, "No data found for $noPolis", Toast.LENGTH_SHORT).show()
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("GeneratePDF", "Error fetching car details", e)
                Toast.makeText(this, "Failed to fetch car details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun fetchDataFromFirestore() {
        swipeRefreshLayout.isRefreshing = true // Mulai refresh layout
        db.collection("cars")
            .get()
            .addOnSuccessListener { result ->
                Log.d("FirestoreData", "Fetched ${result.size()} documents")
                itemList.clear() // Kosongkan list

                if (result.isEmpty) {
                    Log.d("FirestoreData", "No documents found")
                } else {
                    for (document in result) {
                        val nopol = document.getString("noPolis")
                        val status = document.getBoolean("status") ?: false // Ambil status sebagai boolean

                        // Periksa jika dokumen memiliki data yang valid
                        if (nopol != null) {
                            itemList.add(car(nopol, status)) // Tambahkan ke itemList
                        } else {
                            Log.d("FirestoreData", "Skipping empty document")
                        }
                    }
                    adapter.notifyDataSetChanged() // Update RecyclerView
                }
                swipeRefreshLayout.isRefreshing = false // Hentikan refresh layout
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error getting documents: ", exception)
                swipeRefreshLayout.isRefreshing = false
            }
    }


    // Handle item click
    override fun onItemClick(car: car) {
        if (car.status) {
            // Jika status true, tampilkan Toast
            val intent = Intent(this, GeneratePDFActivity::class.java).apply{
                putExtra("no.pol", car.nopol)
            }
//            Toast.makeText(this, "Ke page rama", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        } else {
            // Jika status false, navigasi ke detail page
            val intent = Intent(this, CarDetailActivity::class.java).apply {
                putExtra("NO_POLISI", car.nopol)
            }
            startActivity(intent) // Start the detail activity
        }
    }
}

