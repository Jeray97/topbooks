package com.example.topbooks.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.PostRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            
            db.bookDao().deleteOldBooks(oneWeekAgo)
            db.postDao().deleteOldPosts(oneWeekAgo)
            db.userDao().deleteOldUsers(oneWeekAgo)
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
