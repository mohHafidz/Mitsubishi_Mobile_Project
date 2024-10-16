package com.example.mitsubishi

import CarListAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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

    private fun fetchDataFromFirestore() {
        swipeRefreshLayout.isRefreshing = true // Start refreshing layout
        db.collection("cars")
            .get()
            .addOnSuccessListener { result ->
                Log.d("FirestoreData", "Fetched ${result.size()} documents")
                itemList.clear() // Clear the list

                if (result.isEmpty) {
                    Log.d("FirestoreData", "No documents found")
                } else {
                    for (document in result) {
                        val nopol = document.getString("noPolis") ?: "Unknown"
                        val status = document.getString("status") ?: "Unknown"
                        Log.d("FirestoreData", "Adding document: $nopol, $status")
                        itemList.add(car(nopol, status))
                    }
                    adapter.notifyDataSetChanged() // Update RecyclerView
                }
                swipeRefreshLayout.isRefreshing = false // Stop refreshing layout
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error getting documents: ", exception)
                swipeRefreshLayout.isRefreshing = false
            }
    }

    // Handle item click
    override fun onItemClick(car: car) {
        // Create an Intent to navigate to the detail page
        val intent = Intent(this, CarDetailActivity::class.java).apply {
            putExtra("NO_POLISI", car.nopol)
            putExtra("STATUS", car.status)
        }
        startActivity(intent) // Start the detail activity
    }
}

