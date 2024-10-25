package com.example.uts_lec.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.uts_lec.data.firebase.FirebaseHelper
import com.example.uts_lec.databinding.ActivitySplashBinding
import com.example.uts_lec.ui.login.LoginActivity
import com.example.uts_lec.ui.profile.ProfileActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val firebaseHelper by lazy { FirebaseHelper.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed(
            {
                firebaseHelper.getCurrentUser()?.let { user ->
                    val iProfile = Intent(this@SplashActivity, ProfileActivity::class.java)
                    iProfile.putExtra(ProfileActivity.EXTRA_UID, user.uid)
                    finishAffinity()
                    startActivity(iProfile)
                } ?: kotlin.run {
                    finishAffinity()
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                }
            },
            1500L
        )
    }
}