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

        // --- ARQUITECTURA (ViewModel & LiveData) ---
        val lifecycle_version = "2.6.2"
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
        implementation("androidx.activity:activity-ktx:1.8.0")

        // --- NAVEGACIÓN ---
        implementation("androidx.navigation:navigation-compose:2.7.5")

        // --- DISEÑO EXTRA ---
        implementation("com.google.android.material:material:1.10.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
        implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

        // --- FIREBASE ---
        implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
        implementation("com.google.firebase:firebase-analytics")
        implementation("com.google.firebase:firebase-auth")
        implementation("com.google.firebase:firebase-firestore")

        // --- RED (Retrofit) ---
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")

        // --- IMÁGENES (Coil) ---
        implementation("io.coil-kt:coil-compose:2.5.0")

        // Importar la BoM de Firebase
        implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

        // Firebase Auth
        implementation("com.google.firebase:firebase-auth")

        // Google Play Services Auth
        implementation("com.google.android.gms:play-services-auth:20.7.0")

        implementation("androidx.datastore:datastore-preferences:1.1.1")

        // =======================================================
        // --- TESTING (PRUEBAS) ---
        // =======================================================

        // Unit Testing (Lógica pura - ViewModels, Repositorios)
        testImplementation(libs.junit) // JUnit 4
        testImplementation("io.mockk:mockk:1.13.8") // MockK (Para simular objetos)
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // Para probar corrutinas
        testImplementation("androidx.arch.core:core-testing:2.2.0") // InstantTaskExecutorRule

        // UI / Integration Testing (Android - Pantallas)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.0") // Testear Compose

        // Debugging (Necesario para ver los tests de UI)
        debugImplementation(libs.androidx.compose.ui.tooling)
        debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.0")

        // --- QR (CAMARA) ---
        // CAMERAX (Para la cámara)
        val cameraxVersion = "1.3.1"
        implementation("androidx.camera:camera-core:$cameraxVersion")
        implementation("androidx.camera:camera-camera2:$cameraxVersion")
        implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
        implementation("androidx.camera:camera-view:$cameraxVersion")

        // ML KIT (Para leer códigos de barras/QR)
        implementation("com.google.mlkit:barcode-scanning:17.2.0")

        // ACCOMPANIST (Para pedir permisos de forma fácil en Compose)
        implementation("com.google.accompanist:accompanist-permissions:0.32.0")

        // Lógica de Camera con Guava (A veces necesario para evitar conflictos)
        implementation("com.google.guava:guava:31.1-android")
    }


}