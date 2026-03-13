package com.example.topbooks

import android.util.Log
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.ui.search.SearchViewModel
import io.mockk.every
import io.mockk.mockkStatic

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    // 1. Preparar las dependencias falsas (Mock)
    private val booksRepository = mockk<BooksRepository>()
    private lateinit var viewModel: SearchViewModel

    // Dispatcher especial para controlar el tiempo (por el delay de 800ms)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Le decimos a MockK que intercepte cualquier llamada estática a Log
        mockkStatic(Log::class)
        // Hacemos que cualquier llamada a Log.d devuelva 0 (que es lo que espera internamente) sin hacer nada
        every { Log.d(any(), any()) } returns 0

        viewModel = SearchViewModel(booksRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `al buscar un texto valido, tras 800ms se obtienen los resultados`() = runTest {
        // GIVEN: Preparamos el escenario con un texto de 3 o más letras
        val query = "Android"
        val mockBooks = listOf(
            Book(id = "1", title = "Clean Architecture", provider = "TestProvider"),
            Book(id = "2", title = "Kotlin in Action", provider = "TestProvider")
        )

        // Simulamos méto-do real: searchHybrid devolviendo un Result.success
        coEvery { booksRepository.searchHybrid(query) } returns Result.success(mockBooks)

        // WHEN: Lanzamos el evento tal y como lo llama tu UI
        viewModel.onQueryChange(query)

        // Adelantamos el tiempo virtual para superar el 'delay(800)' de tu corrutina
        advanceUntilIdle()

        // THEN: Comprobamos el StateFlow real de tu ViewModel
        val results = viewModel.searchResults.value
        val isLoading = viewModel.isLoading.value

        assertEquals(false, isLoading)
        assertEquals(2, results.size)
        assertEquals("Clean Architecture", results[0].title)
    }

    @Test
    fun `si el texto tiene menos de 3 letras, se vacia la lista sin llamar al repositorio`() = runTest {
        // GIVEN: Un texto muy corto
        val query = "An"

        // WHEN
        viewModel.onQueryChange(query)
        advanceUntilIdle()

        // THEN: Aseguramos que la regla de negocio funciona y el StateFlow queda vacío
        val results = viewModel.searchResults.value
        assertEquals(0, results.size)
    }
}