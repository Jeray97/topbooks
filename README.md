<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="120" alt="TopBooks Logo"/>

# TopBooks

**Descubre, gestiona y comparte tu vida lectora**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=flat-square)](#-licencia)

[📱 Descargar APK](#-instalación-del-apk) · [✨ Funcionalidades](#-funcionalidades) · [🚀 Configuración](#-configuración-del-entorno)

</div>

---

## 📖 ¿Qué es TopBooks?

TopBooks es una aplicación Android nativa para amantes de los libros. Permite descubrir nuevas lecturas mediante búsqueda inteligente en dos APIs externas, gestionar tu biblioteca personal y conectar con otros lectores a través de reseñas, comentarios y un feed social.

Proyecto personal desarrollado por [Jeray Reyes Morales](https://github.com/Jeray97).

---

## ✨ Funcionalidades

### 📚 Biblioteca personal
- Estanterías personalizables con colores, visibilidad pública/privada y reordenación por drag-and-drop
- **Diario de lectura** con valoraciones multi-eje (romance, alegría, tristeza, picante), playlists, citas y notas
- Seguimiento de libros leídos, pendientes y favoritos
- Marcadores con cita, página y capítulo

### 🔍 Descubrimiento
- Búsqueda híbrida con 5 filtros (general, título, autor, ISBN, saga) combinando **Google Books API** + **Open Library API** + catálogo comunitario
- Escáner de **código ISBN** con CameraX + ML Kit para libros físicos
- Motor de recomendaciones en 4 fases (similitud de favoritos, hidratación por género, populares, fallback)
- 16 géneros literarios con nombres localizados (ES/EN)

### 👥 Comunidad
- Feed social con 3 modos: amigos, algorítmico y top
- **Historias efímeras** de 24h (portadas, citas, estado de lectura)
- Posts: reseñas, citas, "terminé de leer", "estoy leyendo"
- Reacciones, likes, guardados y respuestas en hilos
- **Edición colaborativa de sagas** con sistema de votos

### 📖 Clubes de lectura
- Crear, unirse y gestionar clubes
- Debates por capítulo con flag de spoilers
- Recordatorio semanal automático vía Cloud Functions

### 🌐 Social
- Sistema de amigos con búsqueda y sugerencias
- Muro de actividad social (global y por amigo)
- Perfiles con avatares de capibara, bio, estadísticas y estanterías públicas
- **Notificaciones push** (FCM) con deep-linking directo a la pantalla correspondiente

### 📴 Modo offline
- Caché local con Room (libros 24h, posts 30min, usuarios 1h)
- Limpieza automática cada 6h vía WorkManager
- `NetworkMonitor` con `ConnectivityManager` para fallback transparente

### 🌙 Modo oscuro
- Tema oscuro/claro completo con paletas de colores cálidos
- Persistido en DataStore, aplicado reactivamente

### 🎯 Onboarding
- Tutorial interactivo de 3 pasos (géneros, libros favoritos, confirmación)
- Tour guiado con spotlight animado sobre el menú principal

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│         Jetpack Compose + Material 3                │
├─────────────────────────────────────────────────────┤
│                 ViewModel Layer                     │
│           ViewModel + StateFlow + MVVM              │
├─────────────────────────────────────────────────────┤
│                Repository Layer                     │
│         Repository Pattern + Coroutines             │
├──────────────────────┬──────────────────────────────┤
│    Remote (APIs)     │    Remote (Firebase)         │
│  Google Books API    │  Firestore · Auth · FCM      │
│  Open Library API    │  Cloud Functions · Analytics │
│  Retrofit 2 + Gson   │                              │
├──────────────────────┴──────────────────────────────┤
│                  Local Layer                        │
│   Room (caché offline) · DataStore (preferencias)   │
│   WorkManager (sync background)                     │
└─────────────────────────────────────────────────────┘
```

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Estado | ViewModel + StateFlow |
| Navegación | Navigation Compose |
| Red | Retrofit 2.11 + Gson |
| Base de datos | Firebase Firestore |
| BD local | Room 2.6.1 (KSP) |
| Autenticación | Firebase Auth + Google Sign-In |
| Notificaciones | Firebase Cloud Messaging |
| Background | WorkManager 2.9 |
| Imágenes | Coil 2.5 |
| Cámara/QR | CameraX 1.4.1 + ML Kit Barcode 17.3 |
| Preferencias | Jetpack DataStore |

---

## 📁 Estructura del proyecto

```
app/src/main/java/com/example/topbooks/
├── data/
│   ├── local/          # Room (AppDatabase, DAOs, Entities, Mappers)
│   ├── model/          # Book, User, Post, Club, Journal, Shelf…
│   ├── network/        # RetrofitClient, BooksApiService, FCM Service
│   ├── preferences/    # SettingsManager (DataStore)
│   └── repository/     # Auth, Books, Club, Community, Journal,
│                       # Post, Progress, Shelf, SocialFeed, Story, User
├── ui/
│   ├── auth/           # Login, Register, Google Sign-In
│   ├── book/           # BookDetail, ReadingJournal
│   ├── category/       # Categories, CategoryDetail
│   ├── club/           # Clubes, debates, creación
│   ├── community/      # Feed social, posts, historias
│   ├── components/     # BookItem, TopBar, SearchBar, fondos decorativos
│   ├── config/         # Ajustes (modo oscuro, idioma, géneros, cuenta)
│   ├── friends/        # Amigos, actividad social
│   ├── home/           # Inicio, recomendaciones
│   ├── navigation/     # BottomNavItem
│   ├── profile/        # Perfil, listas de usuario
│   ├── progress/       # Biblioteca (diarios, favoritos, pendientes, leídos)
│   ├── reviews/        # Reseñas, comentarios en tiempo real
│   ├── scanner/        # Escáner QR/ISBN
│   ├── search/         # Búsqueda con debounce
│   ├── shelf/          # Estanterías propias y de amigos
│   ├── theme/          # Colores, tipografía, tema Material 3
│   └── tutorial/       # Onboarding (3 pasos)
└── utils/              # AvatarHelper, CategoryProvider, HtmlCleaner,
                        # SeriesDetector, Resource
```

### ⚡ Backend (Cloud Functions)

| Función | Tipo | Descripción |
|---|---|---|
| `notificarNuevoSeguidor` | Firestore trigger | Push al seguir a un usuario |
| `enviarNotificacionRespuesta` | HTTPS callable | Push al responder una reseña |
| `enviarNotificacionRespuestaPost` | HTTPS callable | Push al responder un post |
| `notificarNuevaDiscusion` | HTTPS callable | Multicast a miembros del club |
| `limpiarStoriesExpiradas` | Scheduled (1h) | Borra historias >24h |
| `recordatorioSemanalClubes` | Scheduled (lun 09:00) | Recordatorio a clubes activos |

---

## 📋 Requisitos previos

| Requisito | Versión mínima |
|---|---|
| Android Studio | Hedgehog (2023.1.1) o superior |
| JDK | 17 (incluido en Android Studio) |
| Android SDK | API 24 (Android 7.0) |
| Google Play Services | Actualizado |
| Cuenta Firebase | Plan Blaze (Freemium) |

---

## ⚙️ Configuración del entorno

### 1. Clonar el repositorio

```bash
git clone https://github.com/Jeray97/topbooks
cd topbooks
```

### 2. Configurar Firebase

El archivo `google-services.json` **no está incluido** en el repositorio por seguridad.

1. Ve a [console.firebase.google.com](https://console.firebase.google.com) y crea un proyecto
2. Añade una app Android con el paquete `com.example.topbooks`
3. Descarga el `google-services.json` y colócalo en `app/src/`
4. Activa **Authentication** (Email/Password + Google)
5. Activa **Firestore Database** en modo producción
6. Activa **Cloud Messaging**

### 3. Configurar API Key

Crea o edita `local.properties` en la raíz:
```properties
GOOGLE_BOOKS_API_KEY=tu_clave_aqui
```

### 4. Desplegar Cloud Functions

```bash
cd functions
npm install
firebase deploy --only functions
```

### 5. Sincronizar y ejecutar

1. Abre el proyecto en **Android Studio**
2. Espera a que **Gradle** sincronice las dependencias
3. Crea un emulador: `Tools → Device Manager → Create Device` (recomendado **Pixel 9, API 36**)
4. Pulsa **Run ▶** o `Shift + F10`

---

## 📱 Instalación del APK

Para instalar directamente sin compilar:

1. Descarga `TopBooks.apk` desde la sección [Releases](../../releases)
2. En tu dispositivo Android: **Ajustes → Seguridad → Instalar aplicaciones desconocidas** y activa el permiso
3. Abre el APK y sigue el asistente
4. Concede los permisos solicitados al abrirla por primera vez:
   - 📷 **Cámara** — para el escáner de ISBN
   - 🔔 **Notificaciones** — para alertas push
5. Regístrate o inicia sesión con Google
6. Completa el onboarding (géneros y libros favoritos)

---

## 🧪 Tests

```bash
# Ejecutar tests de unidad (no requiere dispositivo)
./gradlew test

# Ejecutar tests de integración (requiere emulador o dispositivo)
./gradlew connectedAndroidTest
```

| Tipo | Archivos | Total |
|---|---|---|
| Unidad (JVM) | 20 archivos en `src/test/` | 40 tests |
| Integración (Compose UI) | `SearchScreenIntegrationTest` | 2 tests |

**Stack de testing:** JUnit4 · Kotlin Coroutines Test · MockK · Compose UI Test

---

## 🔒 Permisos

| Permiso | Uso |
|---|---|
| `INTERNET` | Acceso a Google Books API, Open Library y Firebase |
| `CAMERA` | Escáner de código ISBN (opcional, `required="false"`) |
| `POST_NOTIFICATIONS` | Notificaciones push en Android 13+ |

---

## 🛣️ Roadmap

- [ ] Paginación con Jetpack Paging 3
- [ ] Widget de pantalla de inicio con el libro en progreso
- [ ] Importación de biblioteca desde Goodreads
- [ ] Panel de administrador
- [ ] Publicación en Google Play

---

## 📄 Licencia

**All Rights Reserved**

Copyright © 2026 Jeray Reyes Morales. Todos los derechos reservados.

Este software y su código fuente son propiedad exclusiva del autor. No se permite la reproducción, distribución, modificación ni uso comercial o no comercial sin autorización expresa por escrito.

Para solicitudes de licencia o permisos, contactar a través de [GitHub](https://github.com/Jeray97).

---

## 👤 Autor

**Jeray Reyes Morales** — [GitHub](https://github.com/Jeray97)

---

<div align="center">
Hecho con ❤️ y muchos libros 📚
</div>
