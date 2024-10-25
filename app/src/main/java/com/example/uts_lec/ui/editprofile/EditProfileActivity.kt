package com.example.uts_lec.ui.editprofile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.uts_lec.data.firebase.FirebaseHelper
import com.example.uts_lec.databinding.ActivityEditProfileBinding
import com.example.uts_lec.ui.profile.ProfileActivity
import com.example.uts_lec.utils.Result
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private val firebaseHelper by lazy { FirebaseHelper.getInstance(this) }

    private val extraUid by lazy { intent.getStringExtra(ProfileActivity.EXTRA_UID) }
    private val extraMode by lazy { intent.getIntExtra(EXTRA_MODE, 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setViewBasedOnMode(extraMode)
    }

    private fun setViewBasedOnMode(mode: Int) {
        when (mode) {
            0 -> {
                binding.apply {
                    toolbar.title = "Ganti Username"

                    tvUsername.isVisible = true
                    edUsername.isVisible = true

                    btnSave.setOnClickListener {
                        val username = edUsername.text.toString()
                        if (username.isEmpty()) {
                            showToast("Isi Username dengan benar!")
                        } else {
                            showLoading(true)
                            firebaseHelper.database.child(extraUid ?: "").child("username")
                                .setValue(username).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        showLoading(false)
                                        finish()
                                        showToast("Username Telah Diganti!")
                                    } else {
                                        showLoading(false)
                                        showToast("Ganti Username Gagal!")
                                    }
                                }
                        }
                    }
                }
            }

            1 -> {
                binding.apply {
                    toolbar.title = "Ganti Password"

                    tvPassword.isVisible = true
                    edPassword.isVisible = true
                    tvConfirmPassword.isVisible = true
                    edConfirmPassword.isVisible = true

                    btnSave.setOnClickListener {
                        if (binding.edPassword.text.length < 8) {
                            showToast("Password minimal 8 karakter!")
                        } else if (binding.edConfirmPassword.text.isNullOrEmpty()) {
                            showToast("Isi Confirm Password dengan benar!")
                        } else if (binding.edConfirmPassword.text.length < 8) {
                            showToast("Confirm Password minimal 8 karakter!")
                        } else if (binding.edPassword.text.toString() != binding.edConfirmPassword.text.toString()) {
                            showToast("Password dan Confirm Password tidak sama!")
                        } else {
                            val pass = edPassword.text.toString()

                            lifecycleScope.launch {
                                firebaseHelper.changePassword(extraUid ?: "", pass)
                                    .collect { result ->
                                        when (result) {
                                            is Result.Loading -> {
                                                showLoading(true)
                                            }

                                            is Result.Success -> {
                                                showLoading(false)
                                                showToast("Password telah diganti!")
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
                }
            }

            2 -> {
                binding.apply {
                    toolbar.title = "Ganti Nomor Telepon"

                    tvTelp.isVisible = true
                    edTelp.isVisible = true

                    btnSave.setOnClickListener {
                        val telp = edTelp.text.toString()
                        if (telp.isEmpty()) {
                            showToast("Isi Nomor Telepon dengan benar!")
                        } else {
                            showLoading(true)
                            firebaseHelper.database.child(extraUid ?: "").child("telpNumber")
                                .setValue(telp).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        showLoading(false)
                                        finish()
                                        showToast("Nomor Telepon Telah Diganti!")
                                    } else {
                                        showLoading(false)
                                        showToast("Ganti Nomor Telepon Gagal!")
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    private fun isValid() = if (binding.edUsername.text.isNullOrEmpty()) {
        showToast("Isi Username dengan benar!")
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
            edUsername.isEnabled = !isLoading
            edTelp.isEnabled = !isLoading
            edConfirmPassword.isEnabled = !isLoading
            edPassword.isEnabled = !isLoading
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }
}