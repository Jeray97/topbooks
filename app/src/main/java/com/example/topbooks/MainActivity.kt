package com.example.topbooks

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.topbooks.data.preferences.SettingsManager
import com.example.topbooks.ui.AppNavigation
import com.example.topbooks.ui.theme.TopBooksTheme

class MainActivity : ComponentActivity() {

    // 1. ESTADO REACTIVO: Guarda la ruta a la que queremos navegar por Deep Link
    private var pendingRoute by mutableStateOf<String?>(null)

    // Manejador del permiso de notificaciones (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "¡Genial! Te avisaremos de las novedades.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notificaciones desactivadas.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermission()
        processIntent(intent)

        val settingsManager = SettingsManager(this)

        setContent {
            TopBooksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(pendingRoute) {
                        pendingRoute?.let { route ->
                            navController.navigate(route)
                            pendingRoute = null
                        }
                    }

                    // 🟢 Mantenemos AppNavigation aquí. ¡Es lo correcto!
                    AppNavigation(
                        navController = navController,
                        settingsManager = settingsManager
                    )
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent) {
        val type = intent.getStringExtra("type")

        when (type) {
            "NEW_FOLLOWER" -> {
                val followerId = intent.getStringExtra("followerId")
                if (!followerId.isNullOrEmpty()) {
                    pendingRoute = "profile/$followerId"
                }
            }
            "NEW_REPLY" -> {
                val bookId = intent.getStringExtra("bookId")
                if (!bookId.isNullOrEmpty()) {
                    pendingRoute = "reviews_thread/$bookId"
                }
            }
        }

        // Limpiamos los extras
        intent.removeExtra("type")
        intent.removeExtra("followerId")
        intent.removeExtra("bookId")
        intent.removeExtra("commentId")
    }
}