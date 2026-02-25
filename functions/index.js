const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

// Esta función se dispara automáticamente cuando se CREA un nuevo documento en la colección de amigos
exports.notificarNuevoSeguidor = onDocumentCreated('users/{miUid}/friends/{amigoUid}', async (event) => {

    // 1. En la v2, los parámetros vienen dentro de 'event'
    const miUid = event.params.miUid;
    const amigoUid = event.params.amigoUid;

    // 2. Obtenemos los datos del que empezó a seguir
    const snap = event.data;
    const datosSeguidor = snap.data();
    const nombreSeguidor = datosSeguidor.displayName || "Alguien";

    console.log(`Usuario ${miUid} ha empezado a seguir a ${amigoUid}`);

    try {
        // 3. Buscamos el documento del amigo para robarle su fcmToken
        const amigoDoc = await admin.firestore().collection('users').doc(amigoUid).get();
        const amigoData = amigoDoc.data();

        if (!amigoData || !amigoData.fcmToken) {
            return console.log(`El usuario ${amigoUid} no tiene token de notificaciones.`);
        }

        const token = amigoData.fcmToken;

        // 4. Construimos la notificación Push
        const mensaje = {
            notification: {
                title: "¡Tienes un nuevo seguidor en TopBooks! 🎉",
                body: `${nombreSeguidor} ha empezado a seguirte.`
            },
            token: token
        };

        // 5. ¡La disparamos al móvil!
        await admin.messaging().send(mensaje);
        return console.log("¡Notificación enviada con éxito!");

    } catch (error) {
        return console.error("Error al enviar la notificación:", error);
    }
});