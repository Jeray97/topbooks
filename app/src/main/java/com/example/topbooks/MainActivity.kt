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

/**
 * ACTIVIDAD PRINCIPAL (Entry Point).
 * Gestiona el ciclo de vida de la aplicación, los permisos de sistema y
 * la recepción de eventos externos mediante Intents.
 */
class MainActivity : ComponentActivity() {

    // Almacena una ruta de navegación pendiente de ser procesada por el NavController
    private var pendingRoute by mutableStateOf<String?>(null)

    // Registrador para la solicitud de permisos de notificaciones
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notificaciones activadas.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notificaciones desactivadas.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificación inicial de permisos y procesamiento de datos de entrada
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

                    // Navegación reactiva ante rutas pendientes (Deep Linking)
                    LaunchedEffect(pendingRoute) {
                        pendingRoute?.let { route ->
                            navController.navigate(route)
                            pendingRoute = null
                        }
                    }

                    AppNavigation(
                        navController = navController,
                        settingsManager = settingsManager
                    )
                }
            }
        }
    }

    /**
     * Solicita permiso de notificaciones para dispositivos con Android 13 o superior.
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Maneja nuevos intents recibidos mientras la actividad está en segundo plano.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    /**
     * Analiza el contenido del Intent para determinar si el usuario debe ser redirigido
     * a una sección específica debido a una notificación.
     */
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
                val commentId = intent.getStringExtra("commentId")
                val bookId = intent.getStringExtra("bookId")

                if (!commentId.isNullOrEmpty()) {
                    pendingRoute = "single_comment/$commentId"
                } else if (!bookId.isNullOrEmpty()) {
                    pendingRoute = "reviews_thread/$bookId"
                }
            }
        }

        // Limpieza de metadatos del Intent para evitar procesamientos duplicados
        intent.removeExtra("type")
        intent.removeExtra("followerId")
        intent.removeExtra("bookId")
        intent.removeExtra("commentId")
    }
}