package com.example.uts_lec.ui.register

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.uts_lec.R
import com.example.uts_lec.data.firebase.FirebaseHelper
import com.example.uts_lec.data.model.UserModel
import com.example.uts_lec.databinding.ActivityRegisterBinding
import com.example.uts_lec.utils.Result
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    private val firebaseHelper by lazy { FirebaseHelper.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setListeners()
    }

    private fun setListeners() {
        binding.apply {
            btnRegister.setOnClickListener {
                if (isValid()) {
                    val userModel = UserModel()
                    userModel.email = binding.edEmail.text.toString()
                    userModel.password = binding.edPassword.text.toString()
                    userModel.username = binding.edUsername.text.toString()

                    lifecycleScope.launch {
                        firebaseHelper.register(userModel).collect { result ->
                            when (result) {
                                is Result.Loading -> {
                                    showLoading(true)
                                }

                                is Result.Success -> {
                                    showLoading(false)
                                    showToast("User Successfully Created! Please Log In")
                                    finish()
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

            btnLogin.setOnClickListener { finish() }
        }
    }

    private fun isValid() = if (binding.edUsername.text.isNullOrEmpty()) {
        showToast("Isi Username dengan benar!")
        false
    } else if (binding.edEmail.text.isNullOrEmpty() || !Patterns.EMAIL_ADDRESS.matcher(binding.edEmail.text.toString())
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
    } else if (binding.edConfirmPassword.text.isNullOrEmpty()) {
        showToast("Isi Confirm Password dengan benar!")
        false
    } else if (binding.edConfirmPassword.text.length < 8) {
        showToast("Confirm Password minimal 8 karakter!")
        false
    } else if (binding.edPassword.text.toString() != binding.edConfirmPassword.text.toString()) {
        showToast("Password dan Confirm Password tidak sama!")
        false
    } else {
        true
    }

    private fun showLoading(isLoading: Boolean) {
        with(binding) {
            progressbar.isVisible = isLoading
            btnRegister.isVisible = !isLoading
            edEmail.isEnabled = !isLoading
            edUsername.isEnabled = !isLoading
            edConfirmPassword.isEnabled = !isLoading
            edPassword.isEnabled = !isLoading
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}