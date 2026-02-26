package com.example.topbooks.data.repository

import com.example.topbooks.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

// 1. DEFINICIÓN DE LA INTERFAZ
interface AuthRepository {
    val currentUser: com.google.firebase.auth.FirebaseUser?
    suspend fun login(email: String, pass: String): Result<Boolean>
    suspend fun register(name: String, email: String, pass: String): Result<Boolean>
    fun logout()
    suspend fun loginWithGoogle(idToken: String): Result<Boolean>

    // 🟢 NUEVAS FUNCIONES DE SEGURIDAD
    suspend fun sendPasswordResetEmail(email: String): Result<Boolean>
    suspend fun sendEmailVerification(): Result<Boolean>
    suspend fun reloadUser(): Result<Boolean>
}

// 2. IMPLEMENTACIÓN DE LA INTERFAZ
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
                // Creamos el modelo de usuario
                val newUser = User(
                    uid = firebaseUser.uid,
                    displayName = name,
                    displayNameLowercase = name.lowercase(Locale.getDefault()),
                    email = email,
                    photoURL = "capibara_1",
                    isTutorialCompleted = false // Forzamos el tutorial
                )

                // RUTA ESTÁNDAR: users/{uid}
                firestore.collection("users").document(firebaseUser.uid)
                    .set(newUser)
                    .await()

                Result.success(true)
            } else {
                Result.failure(Exception("Error al crear usuario en Auth"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<Boolean> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Verificamos si ya existe en Firestore
                val userRef = firestore.collection("users").document(firebaseUser.uid)
                val doc = userRef.get().await()

                if (!doc.exists()) {
                    val rawName = firebaseUser.displayName ?: "Usuario Google"
                    val newUser = User(
                        uid = firebaseUser.uid,
                        displayName = rawName,
                        displayNameLowercase = rawName.lowercase(Locale.getDefault()),
                        email = firebaseUser.email ?: "",
                        photoURL = firebaseUser.photoUrl?.toString() ?: "capibara_1",
                        isTutorialCompleted = false
                    )
                    userRef.set(newUser).await()
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Error en Login con Google"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        auth.signOut()
    }

    // 🟢 IMPLEMENTACIÓN DE RECUPERACIÓN DE CONTRASEÑA
    override suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🟢 IMPLEMENTACIÓN DE VERIFICACIÓN DE EMAIL
    override suspend fun sendEmailVerification(): Result<Boolean> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🟢 ACTUALIZAR ESTADO DEL USUARIO DESDE FIREBASE
    override suspend fun reloadUser(): Result<Boolean> {
        return try {
            auth.currentUser?.reload()?.await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}