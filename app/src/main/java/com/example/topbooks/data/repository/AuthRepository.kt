package com.example.topbooks.data.repository

import android.util.Log
import com.example.topbooks.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

interface AuthRepository {
    val currentUser: com.google.firebase.auth.FirebaseUser?
    suspend fun login(email: String, pass: String): Result<Boolean>
    suspend fun register(name: String, email: String, pass: String): Result<Boolean>
    fun logout()
    suspend fun loginWithGoogle(idToken: String): Result<Boolean>
}

class AuthRepositoryImpl : AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override val currentUser: com.google.firebase.auth.FirebaseUser?
        get() = auth.currentUser

    override suspend fun login(email: String, pass: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, pass: String): Result<Boolean> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val newUser = User(
                    uid = firebaseUser.uid,
                    displayName = name,
                    displayNameLowercase = name.lowercase(Locale.getDefault()),
                    email = email,
                    // Seteamos el capibara por defecto en el registro manual
                    photoURL = "capibara_1",
                    role = "user",
                    preferences = emptyMap()
                )

                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUser)
                    .await()

                Result.success(true)
            } else {
                Result.failure(Exception("Error creando usuario: UID nulo"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error registro", e)
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<Boolean> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val docSnapshot = firestore.collection("users").document(firebaseUser.uid).get().await()

                if (!docSnapshot.exists()) {
                    val rawName = firebaseUser.displayName ?: "Usuario Google"

                    val newUser = User(
                        uid = firebaseUser.uid,
                        displayName = rawName,
                        displayNameLowercase = rawName.lowercase(Locale.getDefault()),
                        email = firebaseUser.email ?: "",
                        photoURL = "capibara_1",
                        role = "user"
                    )
                    firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                }

                Result.success(true)
            } else {
                Result.failure(Exception("El usuario es nulo tras loguearse con Google"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        auth.signOut()
    }
}