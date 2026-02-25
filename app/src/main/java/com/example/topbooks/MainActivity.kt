package com.example.topbooks

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.topbooks.data.preferences.SettingsManager
import com.example.topbooks.ui.AppNavigation
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    // 1. ESTADO REACTIVO: Ahora guarda la RUTA COMPLETA a la que queremos navegar
    private var pendingRoute by mutableStateOf<String?>(null)

    // Manejador del permiso de notificaciones (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "¡Genial! Te avisaremos de las novedades.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notificaciones desactivadas.", Toast.LENGTH_LONG).show()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()

        val settingsManager = SettingsManager(applicationContext)

        // 2. SI LA APP ESTABA CERRADA: Procesamos el intent inicial
        processIntent(intent)

        setContent {
            val navController = rememberNavController()

            // 3. MAGIA DE COMPOSE: Si pendingRoute cambia, navegamos a donde nos diga
            LaunchedEffect(pendingRoute) {
                pendingRoute?.let { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                    // Reseteamos el estado después de navegar
                    pendingRoute = null
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        navController = navController,
                        settingsManager = settingsManager
                    )
                }
            }
        }
    }

    // 4. SI LA APP YA ESTABA ABIERTA (En segundo plano o usándose)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Procesamos el nuevo intent que acaba de llegar
        processIntent(intent)
    }

    // 5. FUNCIÓN CENTRAL DE DEEP LINKING: Decide a qué ruta ir según el 'type'
    private fun processIntent(intent: Intent) {
        val type = intent.getStringExtra("type")

        when (type) {
            "NEW_FOLLOWER" -> {
                val followerId = intent.getStringExtra("followerId")
                if (!followerId.isNullOrEmpty()) {
                    pendingRoute = "profile/$followerId" // Ruta al perfil
                }
            }
            "NEW_REPLY" -> {
                val bookId = intent.getStringExtra("bookId")
                val commentId = intent.getStringExtra("commentId")
                if (!bookId.isNullOrEmpty() && !commentId.isNullOrEmpty()) {
                    pendingRoute = "reviews_thread/$bookId/$commentId" // Ruta a los comentarios
                }
            }
        }

        // Limpiamos los extras para que no se re-dispare al rotar la pantalla
        intent.removeExtra("type")
        intent.removeExtra("followerId")
        intent.removeExtra("bookId")
        intent.removeExtra("commentId")
    }
}