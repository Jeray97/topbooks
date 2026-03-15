<div align="center">

<img src="main/ic_launcher-playstore.png" width="120" alt="TopBooks Logo"/>

# TopBooks

**Descubre, gestiona y comparte tu vida lectora**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-red?style=flat-square)](LICENSE)

[📱 Descargar APK](#instalación) · [✨ Funcionalidades](#funcionalidades) · [🚀 Configuración](#configuración-del-entorno)

</div>

---

## 📖 ¿Qué es TopBooks?

TopBooks es una aplicación Android nativa para amantes de los libros. Permite descubrir nuevas lecturas mediante búsqueda inteligente en dos APIs externas, gestionar tu biblioteca personal y conectar con otros lectores a través de reseñas, comentarios y un feed social.

Desarrollada como Proyecto Final de Grado del ciclo **DAM 25-26**.

---

## ✨ Funcionalidades

### 📚 Biblioteca
- Búsqueda híbrida en **Google Books API** y **Open Library API** simultáneamente
- Escáner de **código ISBN** con la cámara del móvil
- Organización en listas: **Favoritos**, **Leídos** y **Pendientes**
- Exploración por categorías y géneros

### 👤 Perfil y comunidad
- Registro con **email/contraseña** o **Google Sign-In**
- **Diario de lectura** privado por libro (notas, puntuaciones por tropos, citas)
- **Reseñas públicas** con sistema de estrellas
- **Comentarios por capítulo** con hilos de respuestas
- Sistema de **amigos** con feed de actividad social

### 🌟 Funcionalidades únicas
- **Edición colaborativa de sagas**: la comunidad puede proponer y votar el nombre y número de una saga en cualquier libro
- **Notificaciones push** con Deep Linking directo a la pantalla correspondiente
- **Onboarding personalizado** con selección de géneros y libros favoritos en la primera ejecución
- **Tour guiado** interactivo sobre el menú principal

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
│  Open Library API    │                              │
│  Retrofit 2 + Gson   │                              │
└──────────────────────┴──────────────────────────────┘
```

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Estado | ViewModel + StateFlow |
| Navegación | Navigation Compose |
| Red | Retrofit 2 + Gson |
| Base de datos | Firebase Firestore |
| Autenticación | Firebase Auth |
| Notificaciones | Firebase Cloud Messaging |
| Imágenes | Coil |

---

## 📋 Requisitos previos

| Requisito | Versión mínima |
|---|---|
| Android Studio | Hedgehog (2023.1.1) o superior |
| JDK | 17 (incluido en Android Studio) |
| Android SDK | API 26 (Android 8.0) |
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

Tienes dos opciones:

**Opción A — Usar el archivo de la entrega**
Copia el `google-services.json` proporcionado en la entrega y pégalo en:
```
topbooks/app/src/google-services.json
```

**Opción B — Crear tu propio proyecto Firebase**
1. Ve a [console.firebase.google.com](https://console.firebase.google.com) y crea un proyecto
2. Añade una app Android con el paquete `com.example.topbooks`
3. Descarga el `google-services.json` y colócalo en `app/src/`
4. Activa **Authentication** (Email/Password + Google)
5. Activa **Firestore Database** en modo producción
6. Activa **Cloud Messaging**

### 3. Sincronizar y ejecutar

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

## 🔑 Cuentas de prueba

| Rol | Email | Contraseña | Notas |
|---|---|---|---|
| Usuario estándar | `test1@topbooks.com` | `TopBooks2024!` | Email verificado, todas las funcionalidades activas |
| Usuario nuevo | `test2@topbooks.com` | `TopBooks2024!` | Para probar el onboarding |
| Usuario con reseñas | `reviewer@topbooks.com` | `TopBooks2024!` | Con reseñas y comentarios ya creados |

---

## 🧪 Tests

El proyecto incluye **40 tests de unidad** y **2 tests de integración**.

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

## 📁 Estructura del proyecto

```
app/src/main/java/com/example/topbooks/
├── data/
│   ├── model/          # Book, User, Review, Comment, Journal…
│   ├── network/        # RetrofitClient, BooksApiService, FCM Service
│   ├── preferences/    # SettingsManager (DataStore)
│   └── repository/     # Auth, Books, Progress, Journal, Community…
├── ui/
│   ├── auth/           # Login, Register, AuthViewModel
│   ├── book/           # BookDetail, ReadingJournal
│   ├── category/       # Categories, CategoryDetail
│   ├── friends/        # Friends, SocialActivity, FriendsActivity
│   ├── home/           # Home, Recommended, RecommendedSection
│   ├── profile/        # Profile, UserList
│   ├── progress/       # Progress screen
│   ├── reviews/        # Reviews, SingleComment
│   ├── scanner/        # QR/ISBN Scanner
│   ├── search/         # Search
│   ├── tutorial/       # Onboarding
│   ├── config/         # Settings
│   ├── theme/          # Colors, Typography, Theme
│   └── components/     # Shared composables
└── utils/              # AvatarHelper, CategoryProvider, HtmlCleaner, SeriesDetector
```

---

## 🔒 Permisos

| Permiso | Uso |
|---|---|
| `INTERNET` | Acceso a Google Books API, Open Library y Firebase |
| `CAMERA` | Escáner de código ISBN (opcional, `required="false"`) |
| `POST_NOTIFICATIONS` | Notificaciones push en Android 13+ |

---

## ⚠️ Funcionalidades pendientes

- **Modo offline completo** — el caché de Firestore está parcialmente implementado
- **Panel de administrador** — supervisión de usuarios, reportes y sugerencias
- **Notificaciones de nuevos lanzamientos**
- **Modo oscuro**
- **Publicación en Google Play**

---

## 🛣️ Roadmap

- [ ] Paginación con Jetpack Paging 3
- [ ] Tests de UI con Espresso
- [ ] Soporte de Dark Mode
- [ ] Widget de pantalla de inicio con el libro en progreso
- [ ] Importación de biblioteca desde Goodreads

---

## 👤 Autor

**Jeray Reyes Morales** — Proyecto Final de Grado DAM 25-26

---

<div align="center">
Hecho con ❤️ y muchos libros 📚
</div>
