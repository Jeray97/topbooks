package com.example.topbooks.data.repository

import android.util.Log
import com.example.topbooks.data.model.User
import com.google.firebase.auth.FirebaseAuth
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
                    id = firebaseUser.uid,
                    name = name,
                    email = email
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

    override fun logout() {
        auth.signOut()
    }


}

