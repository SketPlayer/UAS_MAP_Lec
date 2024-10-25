package com.example.uts_lec.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
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

        setListeners()
    }

    private fun setListeners() {
        binding.apply {
            btnLogin.setOnClickListener {
                if (isValid()) {
                    val userModel = UserModel()
                    userModel.email = binding.edEmail.text.toString()
                    userModel.password = binding.edPassword.text.toString()

                    lifecycleScope.launch {
                        firebaseHelper.login(userModel).collect { result ->
                            when (result) {
                                is Result.Loading -> {
                                    showLoading(true)
                                }

                                is Result.Success -> {
                                    showLoading(false)
                                    val firebaseAuth = result.data
                                    firebaseAuth.currentUser?.let { user ->
                                        if (user.isEmailVerified) {
                                            finishAffinity()
                                            val iProfile = Intent(
                                                this@LoginActivity,
                                                ProfileActivity::class.java
                                            )
                                            iProfile.putExtra(ProfileActivity.EXTRA_UID, user.uid)
                                            startActivity(iProfile)
                                        } else {
                                            user.sendEmailVerification()
                                            val iVerif = Intent(
                                                this@LoginActivity,
                                                VerificationActivity::class.java
                                            )
                                            iVerif.putExtra(
                                                VerificationActivity.EXTRA_EMAIL,
                                                userModel.email
                                            )
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
        if (binding.edEmail.text.isNullOrEmpty() || !Patterns.EMAIL_ADDRESS.matcher(binding.edEmail.text.toString())
                .matches()
        ) {
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

