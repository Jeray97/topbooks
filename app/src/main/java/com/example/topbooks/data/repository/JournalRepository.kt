package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Journal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * 1. DEFINICIÓN DE LA INTERFAZ
 * Contrato que define las operaciones permitidas para los Diarios de Lectura.
 */
interface JournalRepository {
    /** Guarda o actualiza un diario de lectura. */
    suspend fun saveJournal(journal: Journal): Result<Boolean>

    /** Obtiene el diario de un libro específico para el usuario actual. */
    suspend fun getJournal(bookId: String): Result<Journal?>

    /** Obtiene todos los diarios de lectura de un usuario (útil para el perfil público o propio). */
    suspend fun getAllJournals(userId: String): Result<List<Journal>>

    /** Elimina el diario asociado a un libro específico. */
    suspend fun deleteJournal(bookId: String): Result<Boolean>
}

/**
 * 2. IMPLEMENTACIÓN DE LA INTERFAZ
 * * Gestiona la conexión con Firebase Firestore.
 * * ARQUITECTURA: Los diarios se guardan como una subcolección dentro del documento del usuario
 * (Ruta: users/{userId}/journals/{bookId}). Esto optimiza enormemente los costes y tiempos de lectura.
 */
class JournalRepositoryImpl : JournalRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Guarda o sobrescribe un diario de lectura en la base de datos.
     * * @param journal Objeto con todos los datos (notas, ratings, tropos) recogidos en la UI.
     */
    override suspend fun saveJournal(journal: Journal): Result<Boolean> {
        // Medida de seguridad: Comprobamos que el usuario esté logueado
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
        return try {
            // Medida de seguridad: Forzamos que el userId del objeto sea el del usuario actual,
            // evitando manipulaciones accidentales o intencionadas de datos.
            val finalJournal = journal.copy(userId = uid)

            // Navegamos: Colección(users) -> Documento(MiUID) -> Colección(journals) -> Documento(ID del libro)
            db.collection("users").document(uid)
                .collection("journals").document(journal.bookId)
                .set(finalJournal) // 'set' sobrescribe si ya existe, o crea uno nuevo si no existe.
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Busca en la base de datos si el usuario actual ya tiene un diario escrito para este libro.
     *
     * @param bookId ID del libro a consultar.
     * @return Result.success con el [Journal] si existe, o Result.success(null) si aún no ha escrito ninguno.
     */
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

    /**
     * Descarga TODOS los diarios escritos por un usuario específico.
     * * A diferencia de los otros métodos, este recibe un 'userId' como parámetro para que
     * puedas ver los diarios de tus amigos al visitar sus perfiles.
     *
     * @param userId El ID del usuario del que queremos consultar los diarios.
     */
    override suspend fun getAllJournals(userId: String): Result<List<Journal>> {
        return try {
            val snap = db.collection("users").document(userId)
                .collection("journals")
                .get()
                .await()
            Result.success(snap.toObjects(Journal::class.java))
        } catch (e: Exception) {
            // Usamos un Log para no asustar al usuario si simplemente aún no hay colección
            android.util.Log.e("JournalRepo", "Error invisible al traer diarios: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Borra permanentemente el diario asociado a un libro.
     */
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