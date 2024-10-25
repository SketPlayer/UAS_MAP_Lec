package com.example.uts_lec

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.uts_lec.data.firebase.FirebaseHelper
import com.example.uts_lec.ui.login.LoginActivity
import com.example.uts_lec.ui.profile.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private val firebaseHelper by lazy { FirebaseHelper.getInstance(this) }
    private var doubleBackToExitPressedOnce = false // Flag to track double back press

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Set the listener for navigation item selections
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Navigate to MainActivity (or HomeFragment)
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_camera -> {
                    // Navigate to CameraActivity
                    startActivity(Intent(this, CameraActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    // Get current user UID
                    val user = firebaseHelper.getCurrentUser() // Make sure this method returns the current user
                    if (user != null) {
                        // User is logged in, navigate to ProfileActivity with UID
                        val iProfile = Intent(this, ProfileActivity::class.java)
                        iProfile.putExtra(ProfileActivity.EXTRA_UID, user.uid)
                        startActivity(iProfile)
                    } else {
                        // Optionally handle case where user is not logged in
                        Toast.makeText(this, "Please log in to access your profile.", Toast.LENGTH_SHORT).show()
                        // Navigate to LoginActivity if needed
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed() // Call the default behavior to finish the activity
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
