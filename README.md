# TopBooks

Red social para lectores. Descubre libros, organiza tu biblioteca, comparte reseñas y conecta con otros lectores.

**Kotlin** · **Jetpack Compose** · **Firebase** · **MVVM**

---

## Funcionalidades

### Biblioteca personal
- Estanterías personalizables con colores, visibilidad pública/privada y reordenación por drag-and-drop
- Diario de lectura con valoraciones multi-eje (romance, felicidad, tristeza, picante), playlists, citas y notas
- Seguimiento de libros leídos, pendientes y favoritos
- Marcadores con cita, página y capítulo

### Descubrimiento
- Buscador híbrido con 5 filtros (general, título, autor, ISBN, saga) combinando Google Books + Open Library + catálogo comunitario
- Escáner QR/ISBN con CameraX + ML Kit para libros físicos
- Motor de recomendaciones en 4 fases (similitud de favoritos, hidratación por género, populares, fallback)
- 16 géneros literarios con nombres localizados (ES/EN)

### Comunidad
- Feed social con 3 modos: amigos, algorítmico y top
- Historias efímeras de 24h (portadas, citas, estado de lectura)
- Posts: reseñas, citas, "terminé de leer", "estoy leyendo"
- Reacciones, likes, guardados y respuestas en hilos
- Edición comunitaria de sagas con sistema de votos

### Clubes de lectura
- Crear, unirse y gestionar clubes
- Debates por capítulo con flag de spoilers
- Recordatorio semanal automático vía Cloud Functions

### Social
- Sistema de amigos con búsqueda y sugerencias
- Muro de actividad social (global y por amigo)
- Perfiles con avatares de capibara, bio, estadísticas y estanterías públicas
- Notificaciones push (FCM) con deep-linking

### Modo offline
- Caché local con Room (libros 24h, posts 30min, usuarios 1h)
- Limpieza automática cada 6h vía WorkManager
- `NetworkMonitor` con `ConnectivityManager` para fallback transparente

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM (ViewModel + StateFlow) |
| Navegación | Compose Navigation |
| Backend | Firebase (Auth, Firestore, FCM, Functions, Analytics) |
| APIs externas | Google Books API, Open Library API |
| Red | Retrofit 2.11 + Gson |
| BD local | Room 2.6.1 (KSP) |
| Preferencias | Jetpack DataStore |
| Background | WorkManager 2.9 |
| Imágenes | Coil 2.5 |
| Cámara/QR | CameraX 1.4.1 + ML Kit Barcode 17.3 |
| Auth | Firebase Auth + Google Sign-In |
| Testing | JUnit 4, MockK, Coroutines Test, Compose UI Test |
| Build | Gradle Kotlin DSL, Version Catalog, compileSdk 36, minSdk 24 |

---

## Arquitectura

```
app/src/main/java/com/example/topbooks/
├── data/
│   ├── local/          Room (AppDatabase, DAOs, Entities, Mappers)
│   ├── model/          Domain models (Book, Post, Club, Journal, Shelf...)
│   ├── network/        Retrofit (BooksApiService, RetrofitClient, FCM Service)
│   ├── preferences/    DataStore (SettingsManager)
│   └── repository/     11 repositorios (Auth, Books, Club, Community, Journal,
│                       Post, Progress, Shelf, SocialFeed, Story, User)
├── ui/
│   ├── auth/           Login, Register, Google Sign-In
│   ├── book/           Detalle, diario de lectura
│   ├── category/       Grid de géneros, detalle por categoría
│   ├── club/           Clubes, debates, creación
│   ├── community/      Feed social, posts, historias
│   ├── components/     BookItem, TopBar, SearchBar, fondos decorativos
│   ├── config/         Ajustes (modo oscuro, idioma, géneros, cuenta)
│   ├── friends/        Amigos, actividad social
│   ├── home/           Inicio, recomendaciones
│   ├── navigation/     BottomNavItem
│   ├── profile/        Perfil, listas de usuario
│   ├── progress/       Biblioteca (diarios, favoritos, pendientes, leídos)
│   ├── reviews/        Reseñas, comentarios en tiempo real
│   ├── scanner/        Escáner QR/ISBN
│   ├── search/         Búsqueda con debounce
│   ├── shelf/          Estanterías propias y de amigos
│   ├── theme/          Colores, tipografía, tema Material 3
│   └── tutorial/       Onboarding (3 pasos)
└── utils/              AvatarHelper, CategoryProvider, HtmlCleaner,
                        SeriesDetector, Resource
```

### Backend (Cloud Functions)

| Función | Tipo | Descripción |
|---|---|---|
| `notificarNuevoSeguidor` | Firestore trigger | Push al seguir a un usuario |
| `enviarNotificacionRespuesta` | HTTPS callable | Push al responder una reseña |
| `enviarNotificacionRespuestaPost` | HTTPS callable | Push al responder un post |
| `notificarNuevaDiscusion` | HTTPS callable | Multicast a miembros del club |
| `limpiarStoriesExpiradas` | Scheduled (1h) | Borra historias >24h |
| `recordatorioSemanalClubes` | Scheduled (lun 09:00) | Recordatorio a clubes activos |

---

## Requisitos

- Android Studio Ladybug o superior
- JDK 11
- Android SDK (minSdk 24, targetSdk 36)
- Cuenta de Google Firebase con proyecto configurado
- Clave API de Google Books

## Instalación

1. Clona el repositorio:
```bash
git clone https://github.com/Jeray97/topbooks.git
```

2. Añade tu `google-services.json` en `app/src/` (descárgalo desde Firebase Console).

3. Crea `local.properties` en la raíz con tu clave de Google Books:
```properties
GOOGLE_BOOKS_API_KEY=tu_clave_aqui
```

4. Despliega las Cloud Functions:
```bash
cd functions
npm install
firebase deploy --only functions
```

5. Abre el proyecto en Android Studio y ejecuta.

## Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

20 tests unitarios cubriendo todos los ViewModels principales + 1 test de integración de UI con Compose.

---

## Licencia

Este proyecto es de uso educativo.
