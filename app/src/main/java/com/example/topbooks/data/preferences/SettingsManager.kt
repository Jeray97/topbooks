package com.example.topbooks.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Extensión de Context para inicializar DataStore como un Singleton.
 * * Esto asegura que solo haya una instancia activa gestionando el archivo "user_settings.pb",
 * evitando bloqueos o corrupciones de datos al leer/escribir.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsManager(context: Context) {

    /**
     * Gestor centralizado de las preferencias locales del usuario.
     * * Utiliza Jetpack DataStore para guardar configuraciones ligeras en el dispositivo
     * (como el tema oscuro o los permisos de notificaciones) y exponerlas a la UI mediante Kotlin Flows.
     *
     * @param context Contexto de la aplicación necesario para acceder al almacenamiento del dispositivo.
     */
    private val dataStore = context.dataStore

    /**
     * Contiene las claves (Keys) tipadas que identifican cada preferencia guardada.
     */
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")
        private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
        private val PUBLIC_JOURNAL_DEFAULT_KEY = booleanPreferencesKey("public_journal_default")
        private val LANGUAGE_KEY = stringPreferencesKey("language_preference")
    }

    // --- LECTURA DE PREFERENCIAS (FLOWS REACTIVOS) ---

    /**
     * Flujo continuo que emite el estado actual del Modo Oscuro.
     * * Si ocurre un error de lectura (IOException), emite preferencias vacías en lugar de crashear la app.
     * * Por defecto, si no hay nada guardado, devuelve 'false'.
     */
    val darkModeFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    /**
     * Flujo continuo que emite si el usuario desea recibir notificaciones push.
     * * Por defecto, devuelve 'true'.
     */
    val notificationsFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[NOTIFICATIONS_KEY] ?: true
        }

    /**
     * Flujo continuo que indica si los nuevos diarios de lectura deben ser públicos por defecto.
     * * Por defecto, devuelve 'false' para priorizar la privacidad del usuario.
     */
    val publicJournalDefaultFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PUBLIC_JOURNAL_DEFAULT_KEY] ?: false
        }

    /**
     * Flujo continuo que emite el idioma seleccionado por el usuario.
     * Por defecto, devuelve "es" (Español).
     */
    val languageFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[LANGUAGE_KEY] ?: "es"
        }

    // --- ESCRITURA DE PREFERENCIAS ---

    /**
     * Guarda la preferencia del Modo Oscuro.
     * * Utiliza 'suspend' porque la escritura en disco es una operación asíncrona.
     * @param enabled 'true' para activar el modo oscuro, 'false' para desactivarlo.
     */
    suspend fun saveDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    /**
     * Guarda la preferencia sobre recibir Notificaciones.
     * @param enabled 'true' para activar notificaciones, 'false' para silenciarlas.
     */
    suspend fun saveNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }

    /**
     * Guarda la preferencia de privacidad por defecto para los Diarios de Lectura.
     * @param enabled 'true' para que sean públicos automáticamente, 'false' para privados.
     */
    suspend fun savePublicJournalDefault(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PUBLIC_JOURNAL_DEFAULT_KEY] = enabled
        }
    }

    /**
     * Guarda el código de idioma seleccionado.
     * @param languageCode "es" para Español, "en" para Inglés, etc.
     */
    suspend fun saveLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }
}