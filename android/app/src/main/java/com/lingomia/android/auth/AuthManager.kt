package com.lingomia.android.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthManager private constructor(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "AuthManager"

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserSignedIn: Boolean
        get() = auth.currentUser != null && auth.currentUser?.isEmailVerified == true

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            if (result.user?.isEmailVerified == true) {
                Result.success(result.user!!)
            } else {
                Result.failure(Exception("Email not verified"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with email", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.sendEmailVerification()?.await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up with email", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending password reset email", e)
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): String? {
        return try {
            val user = auth.currentUser ?: return null
            val result = user.getIdToken(true).await()
            Log.d(TAG, "Token refreshed successfully")
            result.token
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token", e)
            null
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun getCurrentToken(forceRefresh: Boolean = false): String? {
        return try {
            val user = auth.currentUser ?: return null
            val result = user.getIdToken(forceRefresh).await()
            result.token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current token", e)
            null
        }
    }

    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
} 