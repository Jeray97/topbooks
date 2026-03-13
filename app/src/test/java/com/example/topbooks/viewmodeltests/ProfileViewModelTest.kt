package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.profile.ProfileViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val userRepo = mockk<UserRepository>()
    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0

        viewModel = ProfileViewModel(userRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `toggleFriend cambia la UI de forma optimista y hace rollback si hay fallo`() = runTest {
        // GIVEN
        coEvery { userRepo.getCurrentUserId() } returns "miUID"
        // Forzamos que la llamada a red falle
        coEvery { userRepo.toggleFriendship(any(), any(), any(), any(), any()) } returns Result.failure(Exception("Red caída"))

        // Estado inicial de prueba: no es amigo
        assertFalse(viewModel.uiState.value.isFriend)

        // WHEN
        viewModel.toggleFriend("amigo1", "Juan", "foto.jpg")

        // Comprobación de Optimistic UI: El botón cambia antes de acabar la corrutina
        assertTrue("La UI debe cambiar antes de acabar la petición", viewModel.uiState.value.isFriend)

        advanceUntilIdle() // Adelantamos el tiempo para que salte el error

        // THEN: Rollback
        assertFalse("Debe volver a false tras fallar la red", viewModel.uiState.value.isFriend)
    }
}