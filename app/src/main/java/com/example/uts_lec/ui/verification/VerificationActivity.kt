package com.example.uts_lec.ui.verification

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.uts_lec.data.firebase.FirebaseHelper
import com.example.uts_lec.databinding.ActivityVerificationBinding
import com.example.uts_lec.ui.profile.ProfileActivity

class VerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerificationBinding

    private val firebaseHelper by lazy { FirebaseHelper.getInstance(this) }

    private lateinit var handler: Handler
    private lateinit var checkEmailVerificationRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper())
        checkEmailVerificationRunnable = Runnable {
            checkEmailVerification()
        }

        handler.post(checkEmailVerificationRunnable)

        binding.btnResendEmail.setOnClickListener {
            firebaseHelper.getCurrentUser()?.sendEmailVerification()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Verification email sent.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to send verification email.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }


    private fun checkEmailVerification() {
        firebaseHelper.getCurrentUser()?.let { user ->
            if (user.isEmailVerified) {
                Log.e("FTEST", "checkEmailVerification: YA")

                handler.removeCallbacks(checkEmailVerificationRunnable) // Stop the check
                finishAffinity()
                startActivity(Intent(this, ProfileActivity::class.java))
            } else {
                Log.e("FTEST", "checkEmailVerification: BLUM")
            }
        } ?: run {
            Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Schedule the next check after 1 second
        handler.postDelayed(checkEmailVerificationRunnable, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkEmailVerificationRunnable) // Clean up to prevent leaks
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }
}