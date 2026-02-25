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

    //1. ESTADO REACTIVO: Compose "escuchará" esta variable
    private var pendingFollowerId by mutableStateOf<String?>(null)

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

        //2. SI LA APP ESTABA CERRADA: Leemos el intent inicial
        intent.getStringExtra("followerId")?.let {
            pendingFollowerId = it
            intent.removeExtra("followerId") // Lo borramos para que no se repita
        }

        setContent {
            val navController = rememberNavController()

            // MAGIA DE COMPOSE: Si pendingFollowerId cambia, navegamos
            LaunchedEffect(pendingFollowerId) {
                pendingFollowerId?.let { id ->
                    navController.navigate("profile/$id") {
                        launchSingleTop = true // Evita abrir pantallas duplicadas
                    }
                    // Reseteamos el estado después de navegar
                    pendingFollowerId = null
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

    // SI LA APP YA ESTABA ABIERTA (En segundo plano o usándose)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Atrapamos el nuevo ID y actualizamos el estado
        intent.getStringExtra("followerId")?.let {
            pendingFollowerId = it
            intent.removeExtra("followerId") // Lo borramos
        }
    }
}