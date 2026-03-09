package com.example.topbooks.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.topbooks.MainActivity
import com.example.topbooks.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Servicio encargado de gestionar las notificaciones Push entrantes de Firebase Cloud Messaging (FCM).
 * * Esta clase escucha en segundo plano los mensajes enviados desde tu servidor o base de datos.
 */
class TopBooksMessagingService : FirebaseMessagingService() {

    /**
     * Se invoca automáticamente cada vez que el dispositivo recibe una notificación de Firebase.
     * * Se encarga de extraer la información, preparar la navegación (Deep Link) y mostrar
     * la alerta visual en la barra superior del teléfono.
     *
     * @param remoteMessage Objeto que contiene tanto el mensaje visual (título/cuerpo) como los datos ocultos (type, IDs).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Extraer TODOS los datos posibles del servidor (Payload de datos)
        val type = remoteMessage.data["type"]
        val followerId = remoteMessage.data["followerId"]
        val bookId = remoteMessage.data["bookId"]
        val commentId = remoteMessage.data["commentId"]

        // 2. Extraer lo que el usuario va a leer (Payload de notificación)
        val title = remoteMessage.notification?.title ?: "¡Novedad en TopBooks!"
        val body = remoteMessage.notification?.body ?: "Tienes una nueva notificación."

        // 3. Configurar el Intent Universal para el Deep Link
        // Esto le dice a MainActivity qué pantalla debe abrir cuando el usuario toque la notificación.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Metemos TO-DO en el sobre.
            putExtra("type", type)
            putExtra("followerId", followerId)
            putExtra("bookId", bookId)
            putExtra("commentId", commentId)
        }

        // Creamos el PendingIntent que envuelve a nuestro Intent original
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Crear el Canal (Obligatorio para Android 8.0 Oreo y versiones superiores)
        val channelId = "topbooks_social_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Notificaciones de la comunidad",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 5. Construir el diseño visual de la notificación
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setColor(ContextCompat.getColor(this, R.color.ColorBackGroundGeneral))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        // 6. ¡Lanzarla al móvil!
        // Usamos la hora actual como ID para que las notificaciones no se sobrescriban entre sí.
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    /**
     * Se invoca automáticamente cuando Firebase genera o actualiza el token único de este dispositivo.
     * * Este token funciona como la "dirección postal" del móvil. Si cambia, debe actualizarse
     * en la colección 'users' de Firestore para que las notificaciones sigan llegando.
     *
     * @param token El nuevo token generado por FCM.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token generado: $token")
        //TODO MEJORAR LA ACTUALIZACION DE TOKEN DESDE AQUI SI EL USUARIO ESTA LOGEADO
    }
}