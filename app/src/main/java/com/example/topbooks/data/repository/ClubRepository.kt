package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Club
import com.example.topbooks.data.model.ClubMember
import com.example.topbooks.data.model.Discussion
import com.example.topbooks.data.model.DiscussionMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

interface ClubRepository {
    suspend fun createClub(club: Club): Result<String>
    suspend fun updateClub(club: Club): Result<Boolean>
    suspend fun deleteClub(clubId: String): Result<Boolean>
    suspend fun getClubById(clubId: String): Result<Club>
    suspend fun getMyClubs(): Result<List<Club>>
    suspend fun getPublicClubs(limit: Long = 20): Result<List<Club>>
    suspend fun searchClubs(query: String, limit: Long = 20): Result<List<Club>>
    suspend fun joinClub(clubId: String): Result<Boolean>
    suspend fun leaveClub(clubId: String): Result<Boolean>
    suspend fun isMember(clubId: String): Result<Boolean>
    suspend fun updateCurrentBook(clubId: String, bookId: String, bookTitle: String, bookAuthor: String, bookImageUrl: String): Result<Boolean>
    suspend fun updateMemberProgress(clubId: String, progress: Int): Result<Boolean>

    suspend fun createDiscussion(discussion: Discussion): Result<String>
    suspend fun getDiscussions(clubId: String): Result<List<Discussion>>
    suspend fun getDiscussionById(clubId: String, discussionId: String): Result<Discussion>
    suspend fun addMessage(clubId: String, discussionId: String, message: DiscussionMessage): Result<Boolean>
    suspend fun deleteDiscussion(clubId: String, discussionId: String): Result<Boolean>
}

class ClubRepositoryImpl : ClubRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun createClub(club: Club): Result<String> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val docRef = db.collection("clubs").document()
            val newClub = club.copy(
                id = docRef.id,
                createdBy = myUid,
                memberIds = listOf(myUid),
                memberCount = 1
            )
            docRef.set(newClub).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateClub(club: Club): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val existing = db.collection("clubs").document(club.id).get().await()
                .toObject(Club::class.java)
            if (existing?.createdBy != myUid) {
                return Result.failure(Exception("No tienes permiso para editar este club"))
            }
            db.collection("clubs").document(club.id).set(club).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteClub(clubId: String): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val club = db.collection("clubs").document(clubId).get().await()
                .toObject(Club::class.java)
            if (club?.createdBy != myUid) {
                return Result.failure(Exception("No tienes permiso para eliminar este club"))
            }

            val batch = db.batch()
            val discussions = db.collection("clubs").document(clubId)
                .collection("discussions").get().await()
            discussions.documents.forEach { batch.delete(it.reference) }
            batch.delete(db.collection("clubs").document(clubId))
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getClubById(clubId: String): Result<Club> {
        return try {
            val snap = db.collection("clubs").document(clubId).get().await()
            val club = snap.toObject(Club::class.java)
            if (club != null) Result.success(club)
            else Result.failure(Exception("Club no encontrado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyClubs(): Result<List<Club>> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val snap = db.collection("clubs")
                .whereArrayContains("memberIds", myUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snap.toObjects(Club::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPublicClubs(limit: Long): Result<List<Club>> {
        return try {
            val snap = db.collection("clubs")
                .whereEqualTo("isPublic", true)
                .orderBy("memberCount", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
            Result.success(snap.toObjects(Club::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchClubs(query: String, limit: Long): Result<List<Club>> {
        return try {
            val queryLower = query.lowercase()
            val snap = db.collection("clubs")
                .whereEqualTo("isPublic", true)
                .orderBy("name")
                .startAt(queryLower)
                .endAt(queryLower + "\uf8ff")
                .limit(limit)
                .get().await()
            Result.success(snap.toObjects(Club::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinClub(clubId: String): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val ref = db.collection("clubs").document(clubId)
            ref.update(
                "memberIds", FieldValue.arrayUnion(myUid),
                "memberCount", FieldValue.increment(1)
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveClub(clubId: String): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val ref = db.collection("clubs").document(clubId)
            ref.update(
                "memberIds", FieldValue.arrayRemove(myUid),
                "memberCount", FieldValue.increment(-1)
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isMember(clubId: String): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val snap = db.collection("clubs").document(clubId).get().await()
            val club = snap.toObject(Club::class.java)
            Result.success(club?.memberIds?.contains(myUid) == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCurrentBook(
        clubId: String,
        bookId: String,
        bookTitle: String,
        bookAuthor: String,
        bookImageUrl: String
    ): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val club = db.collection("clubs").document(clubId).get().await()
                .toObject(Club::class.java)
            if (club?.createdBy != myUid) {
                return Result.failure(Exception("No tienes permiso"))
            }
            db.collection("clubs").document(clubId).update(
                "currentBookId", bookId,
                "currentBookTitle", bookTitle,
                "currentBookAuthor", bookAuthor,
                "currentBookImageUrl", bookImageUrl,
                "currentBookStartDate", FieldValue.serverTimestamp()
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemberProgress(clubId: String, progress: Int): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val memberSnap = db.collection("clubs").document(clubId)
                .collection("members").document(myUid).get().await()

            if (memberSnap.exists()) {
                db.collection("clubs").document(clubId)
                    .collection("members").document(myUid)
                    .update("currentProgress", progress).await()
            } else {
                val member = ClubMember(userId = myUid, currentProgress = progress)
                db.collection("clubs").document(clubId)
                    .collection("members").document(myUid)
                    .set(member).await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createDiscussion(discussion: Discussion): Result<String> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val docRef = db.collection("clubs").document(discussion.clubId)
                .collection("discussions").document()
            val newDiscussion = discussion.copy(
                id = docRef.id,
                clubId = discussion.clubId,
                createdBy = myUid
            )
            docRef.set(newDiscussion).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDiscussions(clubId: String): Result<List<Discussion>> {
        return try {
            val snap = db.collection("clubs").document(clubId)
                .collection("discussions")
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snap.toObjects(Discussion::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDiscussionById(clubId: String, discussionId: String): Result<Discussion> {
        return try {
            val snap = db.collection("clubs").document(clubId)
                .collection("discussions").document(discussionId).get().await()
            val discussion = snap.toObject(Discussion::class.java)
            if (discussion != null) Result.success(discussion)
            else Result.failure(Exception("Discusion no encontrada"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMessage(
        clubId: String,
        discussionId: String,
        message: DiscussionMessage
    ): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val docRef = db.collection("clubs").document(clubId)
                .collection("discussions").document(discussionId)
            val newMessage = message.copy(
                id = docRef.collection("messages").document().id,
                userId = myUid
            )
            docRef.update(
                "messages", FieldValue.arrayUnion(newMessage),
                "messageCount", FieldValue.increment(1),
                "lastMessageAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDiscussion(clubId: String, discussionId: String): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val discussion = db.collection("clubs").document(clubId)
                .collection("discussions").document(discussionId).get().await()
                .toObject(Discussion::class.java)
            if (discussion?.createdBy != myUid) {
                return Result.failure(Exception("No tienes permiso"))
            }
            db.collection("clubs").document(clubId)
                .collection("discussions").document(discussionId)
                .delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
