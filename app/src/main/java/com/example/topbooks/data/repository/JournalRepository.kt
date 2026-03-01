package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Journal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface JournalRepository {
    suspend fun saveJournal(journal: Journal): Result<Boolean>
    suspend fun getJournal(bookId: String): Result<Journal?>
    suspend fun getAllJournals(userId: String): Result<List<Journal>>

    suspend fun deleteJournal(bookId: String): Result<Boolean>
}

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
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllJournals(userId: String): Result<List<Journal>> {
        return try {
            val snap = db.collection("users").document(userId)
                .collection("journals")
                .get()
                .await()
            Result.success(snap.toObjects(Journal::class.java))
        } catch (e: Exception) {
            android.util.Log.e("JournalRepo", "Error invisible al traer diarios: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteJournal(bookId: String): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
        return try {
            db.collection("users").document(uid).collection("journals").document(bookId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}