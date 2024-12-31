package com.example.uts_lec.ui.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.uts_lec.R
import com.example.uts_lec.data.firebase.FirebaseHelper
import com.example.uts_lec.data.model.UserModel
import com.example.uts_lec.databinding.ActivityProfileBinding
import com.example.uts_lec.CameraActivity
import com.example.uts_lec.ui.editprofile.EditProfileActivity
import com.example.uts_lec.HistoryActivity
import com.example.uts_lec.MainActivity
import com.example.uts_lec.ui.splash.SplashActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val firebaseHelper by lazy { FirebaseHelper.getInstance(this) }

    private val extraUID by lazy { intent.getStringExtra(EXTRA_UID) }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                binding.ivProfile.setImageURI(uri)
                val base64String = uriToBase64(uri) ?: ""
                firebaseHelper.addImageProfile(extraUID ?: "", base64String)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation() // Call the method to set up bottom navigation

        firebaseHelper.observeCurrentUser(extraUID ?: "")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userModel = snapshot.getValue(UserModel::class.java)
                        userModel?.let {
                            if (userModel.profileImage.isNotEmpty()) {
                                val bitmap =
                                    BitmapFactory.decodeFile(base64ToFile(userModel.profileImage)?.absolutePath)
                                binding.ivProfile.setImageBitmap(bitmap)
                            }
                            binding.tvName.text = it.username
                        }
                    } else {
                        Log.d("UserModel", "No user data found.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserModel", "Database error: ${error.message}")
                }
            })

        setListeners()
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation_profile)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_camera -> {
                    startActivity(Intent(this, CameraActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    // Do nothing or refresh the current profile activity
                    true
                }
                else -> false
            }
        }
    }

    private fun setListeners() {
        binding.apply {
            ivProfile.setOnClickListener {
                pickMedia.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }

            btnHistory.setOnClickListener {
                val intent = Intent(this@ProfileActivity, HistoryActivity::class.java)
                intent.putExtra(EXTRA_UID, extraUID)
                startActivity(intent)
            }

            btnChangeNumber.setOnClickListener {
                val iChangeProfile = Intent(this@ProfileActivity, EditProfileActivity::class.java)
                iChangeProfile.putExtra(EXTRA_UID, extraUID)
                iChangeProfile.putExtra(EditProfileActivity.EXTRA_MODE, 2)
                startActivity(iChangeProfile)
            }

            btnChangePassword.setOnClickListener {
                val iChangeProfile = Intent(this@ProfileActivity, EditProfileActivity::class.java)
                iChangeProfile.putExtra(EXTRA_UID, extraUID)
                iChangeProfile.putExtra(EditProfileActivity.EXTRA_MODE, 1)
                startActivity(iChangeProfile)
            }

            btnChangeUsername.setOnClickListener {
                val iChangeProfile = Intent(this@ProfileActivity, EditProfileActivity::class.java)
                iChangeProfile.putExtra(EXTRA_UID, extraUID)
                iChangeProfile.putExtra(EditProfileActivity.EXTRA_MODE, 0)
                startActivity(iChangeProfile)
            }

            btnLogout.setOnClickListener {
                firebaseHelper.auth.signOut()
                finishAffinity()
                startActivity(Intent(this@ProfileActivity, SplashActivity::class.java))
            }
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val file = uriToFile(uri)
            val byteArray = file.readBytes()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File.createTempFile("temp_image", ".jpg")

        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input?.copyTo(output)
            }
        }

        return file
    }

    private fun base64ToFile(base64String: String): File? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val file = File.createTempFile("decoded_image", ".jpg")

            FileOutputStream(file).use { fos ->
                fos.write(decodedBytes)
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Navigate to MainActivity when the back button is pressed
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_UID = "extra_uid"
    }
}
