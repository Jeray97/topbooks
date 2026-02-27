package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Journal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// 1. Creamos una interfaz para definir qué puede hacer este repositorio
interface JournalRepository {
    suspend fun saveJournal(journal: Journal): Result<Boolean>
    suspend fun getJournal(bookId: String): Result<Journal?>
}

// 2. Creamos la implementación real que usa Firebase
class JournalRepositoryImpl : JournalRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun saveJournal(journal: Journal): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
        return try {
            val finalJournal = journal.copy(userId = uid)
            db.collection("users").document(uid)
                .collection("journals").document(journal.bookId)
                .set(finalJournal)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getJournal(bookId: String): Result<Journal?> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
        return try {
            val doc = db.collection("users").document(uid)
                .collection("journals").document(bookId)
                .get()
                .await()

            if (doc.exists()) {
                Result.success(doc.toObject(Journal::class.java))
            } else {
                Result.success(null) // No existe un diario previo, no es un error
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}