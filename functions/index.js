const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();

// --- FUNCIÓN 1: SEGUIDORES ---
exports.notificarNuevoSeguidor = onDocumentCreated('users/{miUid}/friends/{amigoUid}', async (event) => {
    const seguidorId = event.params.miUid;
    const usuarioSeguidoId = event.params.amigoUid;

    try {
        const seguidorDoc = await admin.firestore().collection('users').doc(seguidorId).get();
        const nombreSeguidor = seguidorDoc.data()?.displayName || "Un usuario";

        const seguidoDoc = await admin.firestore().collection('users').doc(usuarioSeguidoId).get();
        const fcmToken = seguidoDoc.data()?.fcmToken;

        if (!fcmToken) {
            return console.log(`El usuario ${usuarioSeguidoId} no tiene token.`);
        }

        const mensaje = {
            notification: {
                title: "¡Tienes un nuevo seguidor! 🎉",
                body: `${nombreSeguidor} ha empezado a seguirte.`
            },
            data: {
                type: "NEW_FOLLOWER",
                followerId: seguidorId
            },
            token: fcmToken
        };

        await admin.messaging().send(mensaje);
        console.log(`Notificación de seguidor enviada a ${usuarioSeguidoId}`);

    } catch (error) {
        console.error("Error en notificarNuevoSeguidor:", error);
    }
});

// --- FUNCIÓN 2: RESPUESTAS ---
exports.enviarNotificacionRespuesta = onCall(async (request) => {
    // 🟢 MEGÁFONO: Estos logs nos dirán si la app llega al servidor
    console.log("FUNCIÓN DESPERTADA");
    console.log("Datos recibidos desde Android:", JSON.stringify(request.data));

    const { autorComentarioOriginalId, nombreRespondedor, bookId, commentId } = request.data;

    try {
        const autorDoc = await admin.firestore().collection('users').doc(autorComentarioOriginalId).get();
        const fcmToken = autorDoc.data()?.fcmToken;

        if (!fcmToken) {
            console.log(`ALERTA: El usuario ${autorComentarioOriginalId} no tiene token.`);
            // Esto le devolverá un success=false a la app de Android
            return { success: false, error: "Usuario sin token" };
        }

        const mensaje = {
            notification: {
                title: "¡Nuevas respuestas en tu reseña! 📚",
                body: `${nombreRespondedor} ha comentado en tu reseña.`
            },
            data: {
                type: "NEW_REPLY",
                bookId: bookId,
                commentId: commentId
            },
            token: fcmToken
        };

        await admin.messaging().send(mensaje);
        console.log(`Notificación de respuesta enviada con éxito a ${autorComentarioOriginalId}`);
        return { success: true };

    } catch (error) {
        console.error("Error CRÍTICO en enviarNotificacionRespuesta:", error);
        return { success: false, error: error.message };
    }
});