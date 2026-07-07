const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
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

// --- FUNCIÓN 3: RESPUESTAS A POSTS DEL FEED ---
exports.enviarNotificacionRespuestaPost = onCall(async (request) => {
    console.log("FUNCIÓN POST DESPERTADA");
    console.log("Datos recibidos:", JSON.stringify(request.data));

    const { postAuthorId, responderName, postId } = request.data;

    try {
        const authorDoc = await admin.firestore().collection('users').doc(postAuthorId).get();
        const fcmToken = authorDoc.data()?.fcmToken;

        if (!fcmToken) {
            console.log(`El autor del post ${postAuthorId} no tiene token.`);
            return { success: false, error: "Usuario sin token" };
        }

        const mensaje = {
            notification: {
                title: "¡Nueva respuesta en tu post! 💬",
                body: `${responderName} ha respondido a tu publicación.`
            },
            data: {
                type: "NEW_POST_REPLY",
                postId: postId
            },
            token: fcmToken
        };

        await admin.messaging().send(mensaje);
        console.log(`Notificación enviada a ${postAuthorId}`);
        return { success: true };

    } catch (error) {
        console.error("Error en enviarNotificacionRespuestaPost:", error);
        return { success: false, error: error.message };
    }
});

// --- FUNCIÓN 4: LIMPIEZA AUTOMÁTICA DE STORIES EXPIRADAS ---
exports.limpiarStoriesExpiradas = onSchedule("every 1 hours", async (event) => {
    const now = new Date();
    console.log(`Limpiando stories expiradas a las ${now.toISOString()}`);

    try {
        const expiredStories = await admin.firestore()
            .collection('stories')
            .where('expiresAt', '<', now)
            .limit(100)
            .get();

        if (expiredStories.empty) {
            console.log("No hay stories expiradas.");
            return;
        }

        const batch = admin.firestore().batch();
        expiredStories.docs.forEach(doc => {
            batch.delete(doc.ref);
        });

        await batch.commit();
        console.log(`Eliminadas ${expiredStories.size} stories expiradas.`);

    } catch (error) {
        console.error("Error limpiando stories:", error);
    }
});

// --- FUNCIÓN 5: NOTIFICACIÓN DE NUEVA DISCUSIÓN EN CLUB ---
exports.notificarNuevaDiscusion = onCall(async (request) => {
    const { clubId, clubName, discussionTitle, creatorId, creatorName } = request.data;

    try {
        const clubDoc = await admin.firestore().collection('clubs').doc(clubId).get();
        const clubData = clubDoc.data();
        const memberIds = clubData?.memberIds || [];

        const membersToNotify = memberIds.filter(id => id !== creatorId);

        if (membersToNotify.length === 0) {
            return { success: true, notified: 0 };
        }

        const userDocs = await Promise.all(
            membersToNotify.map(id => admin.firestore().collection('users').doc(id).get())
        );

        const tokens = userDocs
            .map(doc => doc.data()?.fcmToken)
            .filter(token => token);

        if (tokens.length === 0) {
            return { success: true, notified: 0 };
        }

        const mensaje = {
            notification: {
                title: `Nueva discusión en ${clubName} 📚`,
                body: `${creatorName} abrió: "${discussionTitle}"`
            },
            data: {
                type: "NEW_CLUB_DISCUSSION",
                clubId: clubId
            },
            tokens: tokens
        };

        const response = await admin.messaging().sendEachForMulticast(mensaje);
        console.log(`Notificaciones enviadas: ${response.successCount}/${tokens.length}`);

        return { success: true, notified: response.successCount };

    } catch (error) {
        console.error("Error en notificarNuevaDiscusion:", error);
        return { success: false, error: error.message };
    }
});

// --- FUNCIÓN 6: RECORDATORIO SEMANAL DE CLUB ACTIVO ---
exports.recordatorioSemanalClubes = onSchedule("every monday 09:00", async (event) => {
    console.log("Ejecutando recordatorio semanal de clubes");

    try {
        const clubs = await admin.firestore()
            .collection('clubs')
            .where('currentBookId', '!=', '')
            .get();

        let totalNotified = 0;

        for (const clubDoc of clubs.docs) {
            const club = clubDoc.data();
            const memberIds = club.memberIds || [];

            if (memberIds.length === 0) continue;

            const userDocs = await Promise.all(
                memberIds.map(id => admin.firestore().collection('users').doc(id).get())
            );

            const tokens = userDocs
                .map(doc => doc.data()?.fcmToken)
                .filter(token => token);

            if (tokens.length === 0) continue;

            const mensaje = {
                notification: {
                    title: `¿Cómo va tu lectura? 📖`,
                    body: `Recuerda que en ${club.name} están leyendo "${club.currentBookTitle}"`
                },
                data: {
                    type: "CLUB_REMINDER",
                    clubId: clubDoc.id
                },
                tokens: tokens
            };

            const response = await admin.messaging().sendEachForMulticast(mensaje);
            totalNotified += response.successCount;
        }

        console.log(`Recordatorios enviados: ${totalNotified}`);

    } catch (error) {
        console.error("Error en recordatorioSemanalClubes:", error);
    }
});