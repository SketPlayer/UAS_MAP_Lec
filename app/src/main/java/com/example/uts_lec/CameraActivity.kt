package com.example.uts_lec

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var progressBar: ProgressBar

    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Activity Result Launcher for permissions
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions required to use camera and location", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar)

        // Request permissions
        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
            )
        } else {
            startCamera()
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Capture button listener
        findViewById<AppCompatImageButton>(R.id.captureButton).setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider) }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val userUID = auth.currentUser?.uid ?: return
        // Check if the user has an active parking session
        firestore.collection("users").document(userUID).get()
            .addOnSuccessListener { document ->
                val activeParking = document.getBoolean("activeParking") ?: false
                if (activeParking) {
                    val currentParkingID = document.getString("currentParkingID")
                    Toast.makeText(this, "You already have an active parking session!", Toast.LENGTH_SHORT).show()
                    // Optionally, redirect the user to the home screen or parking details
                    return@addOnSuccessListener
                } else {
                    // Proceed to take a photo and start a parking session
                    proceedToTakePhoto()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check active parking state", exception)
                Toast.makeText(this, "Failed to check parking status. Try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun proceedToTakePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        findViewById<AppCompatImageButton>(R.id.captureButton).isEnabled = false
        progressBar.visibility = View.VISIBLE

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    findViewById<AppCompatImageButton>(R.id.captureButton).isEnabled = true
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    uploadImageToFirebase(savedUri)
                }
            }
        )
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val userUID = auth.currentUser?.uid ?: return
        val storageRef = storage.reference.child("parking_images/$userUID/${UUID.randomUUID()}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d(TAG, "Image uploaded successfully: $downloadUri")
                    recordGpsLocation(downloadUri.toString())
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Image upload failed", exception)
                findViewById<AppCompatImageButton>(R.id.captureButton).isEnabled = true
            }
    }

    private fun recordGpsLocation(imageUrl: String) {
        // Ensure location permission is granted before accessing location
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission if not granted
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            return
        }

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    saveDataToFirestore(imageUrl, location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get location", exception)
            }
    }

    private fun saveDataToFirestore(imageUrl: String, latitude: Double, longitude: Double) {
        val userUID = auth.currentUser?.uid ?: return
        val parkingData = hashMapOf(
            "userUID" to userUID,
            "imageUrl" to imageUrl,
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to System.currentTimeMillis(),
            "active" to true
        )

        val parkingRef = firestore.collection("parking_data").document()

        parkingRef.set(parkingData)
            .addOnSuccessListener {
                val userState = hashMapOf(
                    "activeParking" to true,
                    "currentParkingID" to parkingRef.id
                )
                firestore.collection("users").document(userUID)
                    .set(userState, SetOptions.merge())
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        Log.d(TAG, "User parking data saved successfully")
                        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                        findViewById<AppCompatImageButton>(R.id.captureButton).isEnabled = true

                        // Redirect to MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Optionally finish this activity to prevent going back
                    }
                    .addOnFailureListener { exception ->
                        progressBar.visibility = View.GONE
                        Log.e(TAG, "Failed to save user parking data", exception)
                        Toast.makeText(this, "Failed to update user state", Toast.LENGTH_SHORT).show()
                        findViewById<AppCompatImageButton>(R.id.captureButton).isEnabled = true
                    }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Failed to save parking data", exception)
                findViewById<AppCompatImageButton>(R.id.captureButton).isEnabled = true
            }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onBackPressed() {
        // Check if the ProgressBar is visible
        if (progressBar.visibility == View.VISIBLE) {
            // Optionally show a toast or message informing the user that they cannot go back yet
//            Toast.makeText(this, "Please wait for the process to finish", Toast.LENGTH_SHORT).show()
        } else {
            // Proceed with the default back action
            super.onBackPressed()
        }
    }


    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
