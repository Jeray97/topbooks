package com.example.topbooks.services

import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.topbooks.R
import com.example.topbooks.ui.navigation.BottomNavItem.Friends.title
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TopBooksMessagingService : FirebaseMessagingService() {

    // Se dispara cuando llega una notificación y la app está abierta
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        /* TODO Ajustar icono de notificaciones
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setColor(ContextCompat.getColor(this, R.color.purple_500))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)*/

        Log.d("FCM", "Mensaje recibido de: ${remoteMessage.from}")

        // Si el mensaje tiene datos
        remoteMessage.notification?.let {
            Log.d("FCM", "Cuerpo de la notificación: ${it.body}")
            //TODO Aquí podríamos mostrar un Toast o una notificación personalizada
        }
    }

    // Si el token cambia, lo imprimimos (opcional, lo manejamos en ViewModel pero es aconsejable por Android)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token generado: $token")
    }
}