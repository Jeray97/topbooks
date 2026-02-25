const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

// Esta función se dispara automáticamente cuando se CREA un nuevo documento en la colección de amigos
exports.notificarNuevoSeguidor = onDocumentCreated('users/{miUid}/friends/{amigoUid}', async (event) => {

    // 1. AHORA SÍ: miUid es el que pulsa el botón (Seguidor). amigoUid es el que lo recibe (Seguido).
    const seguidorId = event.params.miUid;
    const usuarioSeguidoId = event.params.amigoUid;

    try {
        // 2. Sacamos el nombre del que acaba de dar a "seguir" (seguidorId)
        const seguidorDoc = await admin.firestore().collection('users').doc(seguidorId).get();
        const nombreSeguidor = seguidorDoc.data()?.displayName || "Un usuario";

        // 3. Sacamos el TOKEN del amigo al que han empezado a seguir (usuarioSeguidoId)
        const seguidoDoc = await admin.firestore().collection('users').doc(usuarioSeguidoId).get();
        const fcmToken = seguidoDoc.data()?.fcmToken;

        if (!fcmToken) {
            return console.log(`El usuario ${usuarioSeguidoId} no tiene token de notificaciones.`);
        }

        // 4. Construimos la notificación Push
        const mensaje = {
            notification: {
                title: "¡Tienes un nuevo seguidor! 🎉",
                body: `${nombreSeguidor} ha empezado a seguirte.`
            },
            data: {
                type: "NEW_FOLLOWER",
                // PASAMOS EL ID DEL SEGUIDOR (El que pulsó el botón) para el Deep Link
                followerId: seguidorId
            },
            token: fcmToken
        };

        // 5. ¡La disparamos al móvil del amigo!
        await admin.messaging().send(mensaje);
        console.log(`Notificación enviada con éxito a ${usuarioSeguidoId}`);

    } catch (error) {
        console.error("Error al enviar la notificación:", error);
    }
});