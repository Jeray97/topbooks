package com.example.topbooks

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
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {


    //MIRAR SI TENEMOS PERMISO PARA NOTIFICACIONES PUSH
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido
            Toast.makeText(this, "¡Genial! Te avisaremos de las novedades.", Toast.LENGTH_SHORT).show()
        } else {
            // Permiso denegado
            Toast.makeText(
                this,
                "Notificaciones desactivadas. No recibirás avisos de tus amigos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //PEDIR PERMISO PARA NOTIFICACIONES PUSH
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

        askNotificationPermission() // <-- Requiere Andorid 13 o sup

        // Inicializamos el SettingsManager (DataStore necesita el context)
        val settingsManager = SettingsManager(applicationContext)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Se lo pasamos a la navegación central
                    AppNavigation(settingsManager = settingsManager)
                }
            }
        }
    }


}