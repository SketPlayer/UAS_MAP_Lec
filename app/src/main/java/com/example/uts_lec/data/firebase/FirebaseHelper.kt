package com.example.uts_lec.data.firebase

import android.content.Context
import com.example.uts_lec.data.model.UserModel
import com.example.uts_lec.utils.Result
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseHelper {
    val auth = Firebase.auth

    val database = Firebase.database.getReference(USER_REFERENCE)

    fun getCurrentUser() = auth.currentUser

    fun observeCurrentUser(uid: String) = database.child(uid)

    fun addImageProfile(uid: String, image: String) =
        database.child(uid).child("profileImage").setValue(image)

    fun register(userModel: UserModel) = flow {
        emit(Result.Loading)
        try {
            val user = suspendCoroutine { continuation ->
                auth.createUserWithEmailAndPassword(userModel.email, userModel.password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            auth.signOut()

                            user?.let {
                                database.child(user.uid).setValue(userModel)
                                    .addOnSuccessListener {
                                        user.sendEmailVerification()
                                        continuation.resume(true to "Successfully Created Account!")
                                    }
                                    .addOnFailureListener { e -> continuation.resume(false to "Error: ${e.message}") }
                            }

                        } else {
                            continuation.resumeWithException(Exception("Register Failed"))
                        }
                    }
            }
            emit(Result.Success(user))
        } catch (e: Exception) {
            emit(Result.Error(e.message.toString()))
        }
    }.flowOn(Dispatchers.IO)

    fun login(userModel: UserModel) = flow {
        emit(Result.Loading)
        try {
            val user = suspendCoroutine { continuation ->
                auth.signInWithEmailAndPassword(userModel.email, userModel.password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth
                            continuation.resume(user)
                        } else {
                            continuation.resumeWithException(Exception("Login Failed"))
                        }
                    }
            }
            emit(Result.Success(user))
        } catch (e: Exception) {
            emit(Result.Error(e.message.toString()))
        }
    }.flowOn(Dispatchers.IO)

    fun changePassword(uid: String, newPassword: String) = flow {
        emit(Result.Loading)
        try {
            val user = auth.currentUser
            if (user != null) {
                suspendCoroutine { continuation ->
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                database.child(uid).child("password").setValue(newPassword)
                                continuation.resume(true to "Password changed successfully!")
                            } else {
                                continuation.resume(false to "Password change failed.")
                            }
                        }
                }.let { result ->
                    emit(Result.Success(result))
                }
            } else {
                emit(Result.Error("User session expired. Log Out First!"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message.toString()))
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val USER_REFERENCE = "users"

        @Volatile
        private var INSTANCE: FirebaseHelper? = null

        @JvmStatic
        fun getInstance(
            context: Context
        ): FirebaseHelper {
            if (INSTANCE == null) {
                synchronized(FirebaseHelper::class.java) {
                    INSTANCE = FirebaseHelper()
                }
            }
            return INSTANCE as FirebaseHelper
        }
    }
}