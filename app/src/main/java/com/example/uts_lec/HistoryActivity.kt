package com.example.uts_lec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uts_lec.data.firebase.FirebaseHelper
import com.example.uts_lec.ui.login.LoginActivity
import com.example.uts_lec.ui.profile.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class HistoryActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private val firebaseHelper by lazy { FirebaseHelper.getInstance(this) }

    private lateinit var historyAdapter: HistoryAdapter
    private val parkingDataList = mutableListOf<ParkingData>()
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        bottomNavigationView = findViewById(R.id.bottom_navigation_home)

        // Handle bottom navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_camera -> {
                    // Navigate to CameraActivity
                    startActivity(Intent(this, CameraActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    // Handle profile navigation
                    val user = firebaseHelper.getCurrentUser()
                    if (user != null) {
                        val profileIntent = Intent(this, ProfileActivity::class.java)
                        profileIntent.putExtra(ProfileActivity.EXTRA_UID, user.uid)
                        startActivity(profileIntent)
                    } else {
                        Toast.makeText(this, "Please log in to access your profile.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    true
                }
                else -> false
            }
        }

        // Initialize UI components
        val recyclerViewHistory: RecyclerView = findViewById(R.id.recyclerViewHistory)
        progressBar = findViewById(R.id.progressBar)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize RecyclerView
        historyAdapter = HistoryAdapter(parkingDataList)
        recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        recyclerViewHistory.adapter = historyAdapter

        // Fetch data for the logged-in user
        fetchParkingData()
    }

    private fun fetchParkingData() {
        // Get the currently logged-in user's UID
        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userUID = currentUser.uid

        // Show progress bar while loading data
        progressBar.visibility = View.VISIBLE

        // Query Firestore for data specific to this user
        db.collection("parking_data")
            .whereEqualTo("userUID", userUID) // Filter by userUID
            .get()
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE // Hide progress bar
                if (task.isSuccessful && task.result != null) {
                    parkingDataList.clear() // Clear the list before adding new data
                    for (document in task.result!!) {
                        val parkingData = document.toObject(ParkingData::class.java)
                        parkingDataList.add(parkingData)
                    }
                    // Notify the adapter that data has changed
                    historyAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
                    Log.e("FirestoreError", "Error fetching data", task.exception)
                }
            }
    }
}
