const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

admin.initializeApp();

// --- FUNCIÓN 1: SEGUIDORES ---
exports.notificarNuevoSeguidor = onDocumentCreated({
  document: 'users/{miUid}/friends/{amigoUid}',
  region: 'europe-west1'
}, async (event) => {
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

// --- FUNCIÓN 7: LIKE EN POST ---
exports.notificarLikePost = onDocumentUpdated('posts/{postId}', async (event) => {
    const beforeData = event.data.before.data();
    const afterData = event.data.after.data();
    const postId = event.params.postId;

    const beforeLikes = beforeData?.likedBy || [];
    const afterLikes = afterData?.likedBy || [];

    // Detectar nuevos likes
    const newLikes = afterLikes.filter(uid => !beforeLikes.includes(uid));

    if (newLikes.length === 0) return;

    const postAuthorId = afterData?.userId;
    if (!postAuthorId) return;

    try {
        const authorDoc = await admin.firestore().collection('users').doc(postAuthorId).get();
        const fcmToken = authorDoc.data()?.fcmToken;
        const authorName = authorDoc.data()?.displayName || "Alguien";

        if (!fcmToken) return;

        // Obtener nombre del usuario que dio like
        const likerId = newLikes[0];
        const likerDoc = await admin.firestore().collection('users').doc(likerId).get();
        const likerName = likerDoc.data()?.displayName || "Un usuario";

        // No notificar si el autor se dio like a sí mismo
        if (likerId === postAuthorId) return;

        const mensaje = {
            notification: {
                title: "¡Nuevo like en tu post! ❤️",
                body: `${likerName} le dio like a tu publicación`
            },
            data: {
                type: "POST_LIKE",
                postId: postId
            },
            token: fcmToken
        };

        await admin.messaging().send(mensaje);
        console.log(`Notificación de like en post enviada a ${postAuthorId}`);

    } catch (error) {
        console.error("Error en notificarLikePost:", error);
    }
});

// --- FUNCIÓN 8: LIKE EN RESEÑA ---
exports.notificarLikeResena = onDocumentUpdated('reviews/{reviewId}', async (event) => {
    const beforeData = event.data.before.data();
    const afterData = event.data.after.data();
    const reviewId = event.params.reviewId;

    const beforeLikes = beforeData?.likedBy || [];
    const afterLikes = afterData?.likedBy || [];

    const newLikes = afterLikes.filter(uid => !beforeLikes.includes(uid));

    if (newLikes.length === 0) return;

    const reviewAuthorId = afterData?.userId;
    if (!reviewAuthorId) return;

    try {
        const authorDoc = await admin.firestore().collection('users').doc(reviewAuthorId).get();
        const fcmToken = authorDoc.data()?.fcmToken;

        if (!fcmToken) return;

        const likerId = newLikes[0];
        const likerDoc = await admin.firestore().collection('users').doc(likerId).get();
        const likerName = likerDoc.data()?.displayName || "Un usuario";

        if (likerId === reviewAuthorId) return;

        const mensaje = {
            notification: {
                title: "¡Nuevo like en tu reseña! ⭐",
                body: `${likerName} le dio like a tu reseña`
            },
            data: {
                type: "REVIEW_LIKE",
                reviewId: reviewId,
                bookId: afterData?.bookId || ""
            },
            token: fcmToken
        };

        await admin.messaging().send(mensaje);
        console.log(`Notificación de like en reseña enviada a ${reviewAuthorId}`);

    } catch (error) {
        console.error("Error en notificarLikeResena:", error);
    }
});

// --- FUNCIÓN 9: AMIGO TERMINÓ LIBRO ---
exports.notificarAmigoTerminoLibro = onDocumentCreated('users/{userId}/read_books/{bookId}', async (event) => {
    const userId = event.params.userId;
    const bookId = event.params.bookId;
    const bookData = event.data.data();

    try {
        // Obtener amigos del usuario
        const friendsSnap = await admin.firestore()
            .collection('users')
            .doc(userId)
            .collection('friends')
            .get();

        if (friendsSnap.empty) return;

        const friendIds = friendsSnap.docs.map(doc => doc.id);

        // Obtener tokens de amigos
        const friendDocs = await Promise.all(
            friendIds.map(id => admin.firestore().collection('users').doc(id).get())
        );

        const tokens = friendDocs
            .map(doc => doc.data()?.fcmToken)
            .filter(token => token);

        if (tokens.length === 0) return;

        // Obtener nombre del usuario
        const userDoc = await admin.firestore().collection('users').doc(userId).get();
        const userName = userDoc.data()?.displayName || "Un amigo";

        // Obtener título del libro
        const bookTitle = bookData?.title || "un libro";

        const mensaje = {
            notification: {
                title: "¡Tu amigo terminó un libro! 📚",
                body: `${userName} terminó de leer "${bookTitle}"`
            },
            data: {
                type: "FRIEND_FINISHED_BOOK",
                userId: userId,
                bookId: bookId
            },
            tokens: tokens
        };

        const response = await admin.messaging().sendEachForMulticast(mensaje);
        console.log(`Notificaciones de libro terminado: ${response.successCount}/${tokens.length}`);

    } catch (error) {
        console.error("Error en notificarAmigoTerminoLibro:", error);
    }
});

// --- FUNCIÓN 10: AMIGO AGREGÓ A FAVORITOS ---
exports.notificarAmigoFavorito = onDocumentCreated('users/{userId}/favorites/{bookId}', async (event) => {
    const userId = event.params.userId;
    const bookId = event.params.bookId;
    const favoriteData = event.data.data();

    try {
        const friendsSnap = await admin.firestore()
            .collection('users')
            .doc(userId)
            .collection('friends')
            .get();

        if (friendsSnap.empty) return;

        const friendIds = friendsSnap.docs.map(doc => doc.id);

        const friendDocs = await Promise.all(
            friendIds.map(id => admin.firestore().collection('users').doc(id).get())
        );

        const tokens = friendDocs
            .map(doc => doc.data()?.fcmToken)
            .filter(token => token);

        if (tokens.length === 0) return;

        const userDoc = await admin.firestore().collection('users').doc(userId).get();
        const userName = userDoc.data()?.displayName || "Un amigo";

        const bookTitle = favoriteData?.title || "un libro";

        const mensaje = {
            notification: {
                title: "¡Nuevo favorito! ❤️",
                body: `${userName} agregó "${bookTitle}" a favoritos`
            },
            data: {
                type: "FRIEND_FAVORITE",
                userId: userId,
                bookId: bookId
            },
            tokens: tokens
        };

        const response = await admin.messaging().sendEachForMulticast(mensaje);
        console.log(`Notificaciones de favorito: ${response.successCount}/${tokens.length}`);

    } catch (error) {
        console.error("Error en notificarAmigoFavorito:", error);
    }
});

// --- FUNCIÓN 11: NUEVO MIEMBRO EN CLUB ---
exports.notificarNuevoMiembroClub = onDocumentCreated('clubs/{clubId}/members/{userId}', async (event) => {
    const clubId = event.params.clubId;
    const newMemberId = event.params.userId;

    try {
        const clubDoc = await admin.firestore().collection('clubs').doc(clubId).get();
        const clubData = clubDoc.data();
        const creatorId = clubData?.createdBy;

        // Solo notificar al creador del club
        if (!creatorId || creatorId === newMemberId) return;

        const creatorDoc = await admin.firestore().collection('users').doc(creatorId).get();
        const fcmToken = creatorDoc.data()?.fcmToken;

        if (!fcmToken) return;

        const newMemberDoc = await admin.firestore().collection('users').doc(newMemberId).get();
        const newMemberName = newMemberDoc.data()?.displayName || "Un usuario";

        const clubName = clubData?.name || "tu club";

        const mensaje = {
            notification: {
                title: "¡Nuevo miembro en tu club! 🎉",
                body: `${newMemberName} se unió a "${clubName}"`
            },
            data: {
                type: "NEW_CLUB_MEMBER",
                clubId: clubId,
                userId: newMemberId
            },
            token: fcmToken
        };

        await admin.messaging().send(mensaje);
        console.log(`Notificación de nuevo miembro enviada a ${creatorId}`);

    } catch (error) {
        console.error("Error en notificarNuevoMiembroClub:", error);
    }
});

// --- FUNCIÓN 12: RECORDATORIO DE INACTIVIDAD ---
exports.recordatorioInactividad = onSchedule("every day 10:00", async (event) => {
    console.log("Ejecutando recordatorio de inactividad");

    try {
        const sevenDaysAgo = new Date();
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

        const users = await admin.firestore()
            .collection('users')
            .where('lastLogin', '<', sevenDaysAgo)
            .get();

        let totalNotified = 0;

        for (const userDoc of users.docs) {
            const userData = userDoc.data();
            const fcmToken = userData?.fcmToken;

            if (!fcmToken) continue;

            const mensaje = {
                notification: {
                    title: "¡Te extrañamos! 👋",
                    body: "Hace 7 días que no registras lectura. ¿Qué estás leyendo?"
                },
                data: {
                    type: "INACTIVITY_REMINDER"
                },
                token: fcmToken
            };

            try {
                await admin.messaging().send(mensaje);
                totalNotified++;
            } catch (sendError) {
                console.error(`Error enviando a ${userDoc.id}:`, sendError);
            }
        }

        console.log(`Recordatorios de inactividad enviados: ${totalNotified}`);

    } catch (error) {
        console.error("Error en recordatorioInactividad:", error);
    }
});

// --- FUNCIÓN 13: RECOMENDACIÓN SEMANAL ---
exports.recomendacionSemanal = onSchedule("every sunday 11:00", async (event) => {
    console.log("Ejecutando recomendaciones semanales");

    try {
        const users = await admin.firestore()
            .collection('users')
            .where('favoriteGenres', '!=', [])
            .get();

        let totalNotified = 0;

        for (const userDoc of users.docs) {
            const userData = userDoc.data();
            const fcmToken = userData?.fcmToken;
            const favoriteGenres = userData?.favoriteGenres || [];

            if (!fcmToken || favoriteGenres.length === 0) continue;

            // Seleccionar un género aleatorio de los favoritos
            const randomGenre = favoriteGenres[Math.floor(Math.random() * favoriteGenres.length)];

            const mensaje = {
                notification: {
                    title: "📚 Recomendación semanal",
                    body: `Descubre nuevos libros de ${randomGenre} que te pueden gustar`
                },
                data: {
                    type: "WEEKLY_RECOMMENDATION",
                    genre: randomGenre
                },
                token: fcmToken
            };

            try {
                await admin.messaging().send(mensaje);
                totalNotified++;
            } catch (sendError) {
                console.error(`Error enviando recomendación a ${userDoc.id}:`, sendError);
            }
        }

        console.log(`Recomendaciones semanales enviadas: ${totalNotified}`);

    } catch (error) {
        console.error("Error en recomendacionSemanal:", error);
    }
});

// --- FUNCIÓN 14: RACHA DE LECTURA ---
exports.notificarRachaLectura = onSchedule("every day 20:00", async (event) => {
    console.log("Ejecutando notificación de racha de lectura");

    try {
        const today = new Date();
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);

        // Buscar usuarios que leyeron ayer
        const users = await admin.firestore()
            .collection('users')
            .get();

        let totalNotified = 0;

        for (const userDoc of users.docs) {
            const userData = userDoc.data();
            const fcmToken = userData?.fcmToken;
            const readingStreak = userData?.readingStreak || 0;

            if (!fcmToken || readingStreak < 3) continue;

            // Solo notificar en múltiplos de 7 días
            if (readingStreak % 7 !== 0) continue;

            const mensaje = {
                notification: {
                    title: "¡Increíble racha! 🔥",
                    body: `Llevas ${readingStreak} días seguidos leyendo. ¡Sigue así!`
                },
                data: {
                    type: "READING_STREAK",
                    streak: readingStreak.toString()
                },
                token: fcmToken
            };

            try {
                await admin.messaging().send(mensaje);
                totalNotified++;
            } catch (sendError) {
                console.error(`Error enviando racha a ${userDoc.id}:`, sendError);
            }
        }

        console.log(`Notificaciones de racha enviadas: ${totalNotified}`);

    } catch (error) {
        console.error("Error en notificarRachaLectura:", error);
    }
});

// --- FUNCIÓN 15: RESUMEN SEMANAL ---
exports.resumenSemanal = onSchedule("every monday 08:00", async (event) => {
    console.log("Ejecutando resumen semanal");

    try {
        const oneWeekAgo = new Date();
        oneWeekAgo.setDate(oneWeekAgo.getDate() - 7);

        const users = await admin.firestore()
            .collection('users')
            .get();

        let totalNotified = 0;

        for (const userDoc of users.docs) {
            const userData = userDoc.data();
            const fcmToken = userData?.fcmToken;
            const userId = userDoc.id;

            if (!fcmToken) continue;

            // Contar actividad de amigos en la última semana
            const friendsSnap = await admin.firestore()
                .collection('users')
                .doc(userId)
                .collection('friends')
                .get();

            if (friendsSnap.empty) continue;

            const friendIds = friendsSnap.docs.map(doc => doc.id);

            // Contar libros terminados por amigos
            let booksFinished = 0;
            for (const friendId of friendIds.slice(0, 10)) { // Limitar a 10 amigos
                const readBooks = await admin.firestore()
                    .collection('users')
                    .doc(friendId)
                    .collection('read_books')
                    .where('readAt', '>', oneWeekAgo)
                    .get();
                booksFinished += readBooks.size;
            }

            // Contar nuevas reseñas en el feed
            const newPosts = await admin.firestore()
                .collection('posts')
                .where('userId', 'in', friendIds.slice(0, 10))
                .where('createdAt', '>', oneWeekAgo)
                .get();

            if (booksFinished === 0 && newPosts.size === 0) continue;

            const mensaje = {
                notification: {
                    title: "📊 Tu resumen semanal",
                    body: `Esta semana: ${booksFinished} libros terminados y ${newPosts.size} nuevas publicaciones de tus amigos`
                },
                data: {
                    type: "WEEKLY_SUMMARY"
                },
                token: fcmToken
            };

            try {
                await admin.messaging().send(mensaje);
                totalNotified++;
            } catch (sendError) {
                console.error(`Error enviando resumen a ${userId}:`, sendError);
            }
        }

        console.log(`Resúmenes semanales enviados: ${totalNotified}`);

    } catch (error) {
        console.error("Error en resumenSemanal:", error);
    }
});