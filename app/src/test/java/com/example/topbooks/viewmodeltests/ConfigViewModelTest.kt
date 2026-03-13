package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.preferences.SettingsManager
import com.example.topbooks.data.repository.AuthRepository
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.ui.config.ConfigViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigViewModelTest {

    private val settingsManager = mockk<SettingsManager>()
    private val authRepository = mockk<AuthRepository>()
    private val userRepository = mockk<UserRepository>()
    private lateinit var viewModel: ConfigViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Mockeamos los flujos locales (DataStore) para evitar NullPointerExceptions en el init{}
        every { settingsManager.darkModeFlow } returns MutableStateFlow(false)
        every { settingsManager.notificationsFlow } returns MutableStateFlow(true)
        every { settingsManager.publicJournalDefaultFlow } returns MutableStateFlow(false)
        every { settingsManager.languageFlow } returns MutableStateFlow("es")

        coEvery { authRepository.reloadUser() } returns Result.success(true)
        coEvery { authRepository.isEmailVerified() } returns true
        coEvery { userRepository.getCurrentUserId() } returns "123"
        coEvery { userRepository.getUserProfile(any()) } returns Result.failure(Exception())

        viewModel = ConfigViewModel(settingsManager, authRepository, userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `saveFavoriteGenres actualiza la lista local en caso de exito`() = runTest {
        // GIVEN
        val nuevasCategorias = listOf("Fantasía", "Ciencia Ficción")
        coEvery { userRepository.updateFavoriteGenres(any(), any()) } returns Result.success(true)

        // WHEN
        var mensaje = ""
        viewModel.saveFavoriteGenres(nuevasCategorias) { msg -> mensaje = msg }
        advanceUntilIdle()

        // THEN
        assertEquals("Categorías actualizadas correctamente.", mensaje)
        assertEquals(nuevasCategorias, viewModel.favoriteGenres.value)
    }
}