package com.example.topbooks.data.repository

import android.util.Log
import com.example.topbooks.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

//1. Interfaz, definimos que puede hacer nuestra app con la autentificación
interface AuthRepository {
    //Devuelve el usuario actual o null si no hay sesión
    val currentUser: com.google.firebase.auth.FirebaseUser?

    //Funciones suspendidas para corrutinas
    suspend fun login(email: String, pass: String): Result<Boolean>
    suspend fun register(name: String, email: String, pass: String): Result<Boolean>
    fun logout()
    suspend fun loginWithGoogle(idToken: String): Result<Boolean>
}

//2. Implementación, definimos que se hace usando Firebase
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
            //1. Crear usuario en Auth (Email/Pass)
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                //2. Creamos un objeto User propio para Firestore
                val newUser = User(
                    uid = firebaseUser.uid,
                    displayName = name,
                    email = email,
                    photoURL = "",
                    role = "user",
                    preferences = emptyMap()
                )

                //3. Guardamos en la coleccion "users"
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUser)
                    .await()

                Result.success(true)
            } else {
                Result.failure(Exception("Error creando usuario: UID nulo"))
            }
        } catch (e: Exception) {
            // Si falla algo (ej: email repetido), devolvemos el error
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
                // Opcional: Comprobamos si el usuario ya existe en Firestore para no sobrescribirlo
                val docSnapshot = firestore.collection("users").document(firebaseUser.uid).get().await()

                if (!docSnapshot.exists()) {
                    // Si es la primera vez que entra con Google, lo guardamos en la base de datos
                    val newUser = User(
                        uid = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: "Usuario Google",
                        email = firebaseUser.email ?: "",
                        photoURL = firebaseUser.photoUrl.toString(),
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

