package com.example.topbooks.ui.home

import android.util.Log
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.*
import com.example.topbooks.utils.Resource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendedSectionViewModelTest {

    private val booksRepo = mockk<BooksRepository>()
    private val communityRepo = mockk<CommunityRepository>()
    private val userRepo = mockk<UserRepository>()

    private lateinit var viewModel: RecommendedSectionViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        viewModel = RecommendedSectionViewModel(booksRepo, communityRepo, userRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadSectionData con seccion popular carga datos de la API`() = runTest {
        // GIVEN: Creamos un libro falso para que la lista no esté vacía
        val mockBook = mockk<Book>(relaxed = true)
        val listaConLibros = listOf(mockBook)

        // Configuramos el mock para que devuelva nuestra lista con 1 libro
        coEvery {
            booksRepo.getBooks(any(), any(), any(), any(), any())
        } returns Result.success(listaConLibros)

        // WHEN
        viewModel.loadSectionData("popular", "Fantasía")
        advanceUntilIdle() // Esperamos a que terminen las corrutinas

        // THEN:
        val state = viewModel.booksState.value

        // Si vuelve a fallar, imprimimos de qué tipo es realmente el estado para saber por qué falla
        assertTrue(
            "El estado actual es: ${state::class.java.simpleName}, pero se esperaba Success",
            state is Resource.Success
        )

        // Verificamos que se llamó al repositorio exactamente 1 vez
        coVerify(exactly = 1) { booksRepo.getBooks(any(), any(), any(), any(), any()) }
    }
}