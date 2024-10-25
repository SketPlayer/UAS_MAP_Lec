package com.example.uts_lec.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.uts_lec.MainActivity
import com.example.uts_lec.data.firebase.FirebaseHelper
import com.example.uts_lec.data.model.UserModel
import com.example.uts_lec.databinding.ActivityLoginBinding
import com.example.uts_lec.ui.profile.ProfileActivity
import com.example.uts_lec.ui.register.RegisterActivity
import com.example.uts_lec.ui.verification.VerificationActivity
import com.example.uts_lec.utils.Result
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private val firebaseHelper by lazy { FirebaseHelper.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Check if user is already logged in
        checkUserLoggedIn()

        setListeners()
    }

    private fun checkUserLoggedIn() {
        val firebaseAuth = firebaseHelper.getCurrentUser() // Adjust this method based on your FirebaseHelper
        firebaseAuth?.let { user ->
            // User is logged in, redirect to ProfileActivity
            if (user.isEmailVerified) {
                finishAffinity() // Close all activities
                val iMain = Intent(this, MainActivity::class.java) // Change to MainActivity
                startActivity(iMain) // Start MainActivity
            }
        }
    }

    private fun setListeners() {
        binding.apply {
            btnLogin.setOnClickListener {
                if (isValid()) {
                    val userModel = UserModel().apply {
                        email = binding.edEmail.text.toString()
                        password = binding.edPassword.text.toString()
                    }

                    lifecycleScope.launch {
                        firebaseHelper.login(userModel).collect { result ->
                            when (result) {
                                is Result.Loading -> {
                                    showLoading(true)
                                }

                                is Result.Success -> {
                                    showLoading(false)
                                    val user = result.data.currentUser
                                    if (user != null) {
                                        if (user.isEmailVerified) {
                                            finishAffinity()
                                            val iMain = Intent(this@LoginActivity, MainActivity::class.java) // Change to MainActivity
                                            startActivity(iMain) // Start MainActivity
                                        } else {
                                            user.sendEmailVerification()
                                            val iVerif = Intent(this@LoginActivity, VerificationActivity::class.java)
                                            iVerif.putExtra(VerificationActivity.EXTRA_EMAIL, userModel.email)
                                            startActivity(iVerif)
                                        }
                                    }
                                }

                                is Result.Error -> {
                                    showLoading(false)
                                    showToast(result.error)
                                }
                            }
                        }
                    }
                }
            }

            btnRegister.setOnClickListener {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }
        }
    }

    private fun isValid() =
        if (binding.edEmail.text.isNullOrEmpty() || !Patterns.EMAIL_ADDRESS.matcher(binding.edEmail.text.toString()).matches()) {
            showToast("Isi Email dengan benar!")
            false
        } else if (binding.edPassword.text.isNullOrEmpty()) {
            showToast("Isi Password dengan benar!")
            false
        } else if (binding.edPassword.text.length < 8) {
            showToast("Password minimal 8 karakter!")
            false
        } else {
            true
        }

    private fun showLoading(isLoading: Boolean) {
        with(binding) {
            progressbar.isVisible = isLoading
            btnRegister.isVisible = !isLoading
            edEmail.isEnabled = !isLoading
            edPassword.isEnabled = !isLoading
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
