package com.example.uts_lec

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.uts_lec.data.firebase.FirebaseHelper
import com.example.uts_lec.ui.login.LoginActivity
import com.example.uts_lec.ui.profile.ProfileActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var questionMarkImage: ImageView
    private lateinit var parkingPointText: TextView
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var map: GoogleMap

    private val firebaseHelper by lazy { FirebaseHelper.getInstance(this) }
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var doubleBackToExitPressedOnce = false // Flag to track double back press

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        bottomNavigationView = findViewById(R.id.bottom_navigation_home)
        questionMarkImage = findViewById(R.id.question_mark_image)
        parkingPointText = findViewById(R.id.parking_point_text)
        mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment

        // Load the map asynchronously
        mapFragment.getMapAsync(this)

        // Handle bottom navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // Stay on the home page
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

        // Check and update UI based on parking state
        updateUIForParkingState()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
    }

    private fun updateUIForParkingState() {
        val userUID = auth.currentUser?.uid ?: return

        // Fetch user's parking state from Firestore
        firestore.collection("users").document(userUID).get()
            .addOnSuccessListener { document ->
                val activeParking = document.getBoolean("activeParking") ?: false
                if (activeParking) {
                    val parkingID = document.getString("currentParkingID")
                    if (parkingID != null) {
                        fetchParkingData(parkingID)
                    }
                } else {
                    // No active parking session
                    showInitialUI()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Failed to fetch user parking state", exception)
                Toast.makeText(this, "Failed to load parking status.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showInitialUI() {
        mapFragment.view?.visibility = View.GONE
        questionMarkImage.visibility = View.VISIBLE
        parkingPointText.visibility = View.VISIBLE
    }

    private fun fetchParkingData(parkingID: String) {
        firestore.collection("parking_data").document(parkingID).get()
            .addOnSuccessListener { document ->
                val latitude = document.getDouble("latitude")
                val longitude = document.getDouble("longitude")
                if (latitude != null && longitude != null) {
                    val parkingLocation = LatLng(latitude, longitude)
                    map.addMarker(
                        MarkerOptions().position(parkingLocation).title("Your Parking Spot")
                    )
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(parkingLocation, 15f))

                    // Show map UI
                    mapFragment.view?.visibility = View.VISIBLE
                    questionMarkImage.visibility = View.GONE
                    parkingPointText.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Failed to fetch parking data", exception)
                Toast.makeText(this, "Failed to load parking location.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        updateUIForParkingState() // Refresh UI on resume
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            finishAffinity() // Exit the app completely
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

        // Reset the flag after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }
}
