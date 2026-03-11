import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

// Obtener properties para poder ver la clave API escondida
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
// Guardamos la clave en una variable segura
val googleBooksApiKey: String = localProperties.getProperty("GOOGLE_BOOKS_API_KEY") ?: ""

android {
    namespace = "com.example.topbooks"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.topbooks"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_KEY", "\"$googleBooksApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // --- LIBRERÍAS DE ANDROID Y COMPOSE (Base) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // --- ARQUITECTURA (ViewModels y Compose) ---
    // ACTUALIZADO: 2.8.7 es la versión estable y madura que soluciona bugs de retención de memoria en Compose
    val lifecycle_version = "2.8.7"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_version")

    // --- ICONOS EXTENDIDOS (Material 3) ---
    implementation("androidx.compose.material:material-icons-extended")

    // --- NAVEGACIÓN ---
    // ACTUALIZADO: 2.8.5 añade soporte estable para Type-Safe Navigation y corrige cierres en el backstack
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // --- FIREBASE  ---
    // ACTUALIZADO: 33.7.0 actualiza los binarios internos nativos a 16KB y mejora seguridad
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-functions")

    // --- RED (Retrofit) ---
    // ACTUALIZADO: 2.11.0 da soporte nativo completo a Coroutines modernas
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // --- IMÁGENES (Coil) ---
    implementation("io.coil-kt:coil-compose:2.5.0")

    // --- AUTH Y PREFERENCIAS ---
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- TESTING (PRUEBAS) ---
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.10.5")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.0")

    // --- QR (CAMARA Y ESCÁNER) ---
    // ACTUALIZADO CRÍTICO: 1.4.1 recompila libimage_processing_util_jni.so para páginas de 16 KB
    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ACTUALIZADO CRÍTICO: 17.3.0 recompila libbarhopper_v3.so para páginas de 16 KB
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // ACTUALIZADO: 0.36.0 para mantener compatibilidad de permisos con las nuevas versiones de Compose
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
    implementation("com.google.guava:guava:31.1-android")
}