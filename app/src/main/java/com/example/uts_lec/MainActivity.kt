package com.example.uts_lec

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.os.Build
import android.os.Bundle

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


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
                }
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

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val photoData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "date" to name,
                "location" to location?.let {
                    mapOf("latitude" to it.latitude, "longitude" to it.longitude)
                },
                "imageUrl" to imageUrl
            )

            firestore.collection("photos").add(photoData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Photo data saved to Firestore", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving data to Firestore", e)
                    Toast.makeText(this, "Failed to save data to Firestore", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build() // Ensure imageCapture is initialized here

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                Toast.makeText(this, "Camera initialized", Toast.LENGTH_SHORT).show() // Confirm camera initialization
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val permissionGranted = permissions.entries.all { it.value }
            if (!permissionGranted) {
                Toast.makeText(baseContext, "Permission request denied", Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    companion object {
        private const val TAG = "ParkTrack Cam"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}
