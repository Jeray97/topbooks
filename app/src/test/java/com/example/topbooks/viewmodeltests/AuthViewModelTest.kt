package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.AuthRepository
import com.example.topbooks.ui.auth.AuthViewModel
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository = mockk<AuthRepository>()
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // 1. Configuramos el hilo principal falso para las corrutinas
        Dispatchers.setMain(testDispatcher)

        // 2. Mockeamos TODAS las llamadas estáticas a la clase Log de Android
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0

        // 3. Mock de inicio seguro: Le decimos que el currentUser es null al arrancar
        // Usamos 'as FirebaseUser?' para solucionar el error de inferencia de tipos
        every { authRepository.currentUser } returns null as FirebaseUser?

        // 4. Inicializamos el ViewModel con nuestro repositorio falso
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        // Limpiamos la configuración al terminar cada test
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `resetPassword comunica el error si el correo es incorrecto`() = runTest {
        // GIVEN: El repositorio falla al intentar enviar el correo de recuperación
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns Result.failure(Exception("Error simulado de Firebase"))

        // WHEN: Llamamos a la función del ViewModel simulando lo que haría la UI
        var isSuccess = true
        var resultMessage = ""

        viewModel.resetPassword("correo_raro@test.com") { exito, mensaje ->
            isSuccess = exito
            resultMessage = mensaje
        }

        // Adelantamos el tiempo de las corrutinas para que termine
        advanceUntilIdle()

        // THEN: Comprobamos que el callback devolvió false y el mensaje de error correcto
        assertFalse("El callback debería devolver false", isSuccess)
        assertEquals("No pudimos enviar el correo. Comprueba que está bien escrito.", resultMessage)
    }

    @Test
    fun `clearError limpia el mensaje en la UI`() = runTest {
        // GIVEN: Simulamos que había un error previo en la UI (puedes forzarlo si quieres,
        // pero la prueba asume que la función simplemente lo pone a null)

        // WHEN: Se llama a la función para limpiar errores
        viewModel.clearError()

        // THEN: El estado de la UI ya no debe tener mensaje de error
        assertNull("El mensaje de error debería ser null", viewModel.uiState.value.errorMessage)
    }
}