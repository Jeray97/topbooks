package com.example.topbooks

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.topbooks.data.local.SyncWorker
import java.util.concurrent.TimeUnit

class TopBooksApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        setupPeriodicSync()
    }
    
    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            6, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "topbooks_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
