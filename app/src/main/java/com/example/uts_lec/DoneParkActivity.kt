package com.example.uts_lec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoneParkActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donepark)

        // Find the back to home button
        val backToHomeButton = findViewById<Button>(R.id.back_to_home_button)

        // Set click listener for the button
        backToHomeButton.setOnClickListener {
            resetParkingStateAndNavigateHome()
        }
    }

    private fun resetParkingStateAndNavigateHome() {
        val userUID = auth.currentUser?.uid
        if (userUID == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show()
            return
        }

        // Reset user parking state in Firestore
        firestore.collection("users").document(userUID)
            .update(
                "activeParking", false,
                "currentParkingID", null // Clear reference to the current parking session
            )
            .addOnSuccessListener {
                Log.d("DoneParkActivity", "User parking state reset successfully.")
                navigateToHome()
            }
            .addOnFailureListener { exception ->
                Log.e("DoneParkActivity", "Failed to reset parking state.", exception)
                Toast.makeText(this, "Failed to reset parking state.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToHome() {
        // Navigate back to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        // Clear all activities in the back stack to start fresh
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Navigate to MainActivity when the back button is pressed
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
