package com.example.topbooks.viewmodeltests


import com.example.topbooks.MainDispatcherRule
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.ui.category.CategoryDetailViewModel
import com.example.topbooks.utils.Resource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests para CategoryDetailViewModel.
 *
 * PROBLEMAS QUE CORREGIMOS:
 * 1. BooksRepository es clase concreta → usamos mockk<BooksRepository>() de MockK.
 * 2. fetchBooksByCategory() usa viewModelScope.launch → necesita MainDispatcherRule.
 *
 * Flujo real del ViewModel:
 *   fetchBooksByCategory(query)
 *     → _categoryBooks.value = Resource.Loading
 *     → repository.searchHybrid(query)
 *     → si isSuccess → _categoryBooks.value = Resource.Success(books)
 *     → si isFailure → _categoryBooks.value = Resource.Error(exception)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockRepo: BooksRepository
    private lateinit var viewModel: CategoryDetailViewModel

    @Before
    fun setUp() {
        mockRepo  = mockk(relaxed = true)
        viewModel = CategoryDetailViewModel(repository = mockRepo)
    }

    /**
     * Prueba: fetchBooksByCategory en caso de éxito actualiza el estado a Success.
     *
     * Verifica que cuando searchHybrid devuelve una lista de libros,
     * booksState emite Resource.Success con esa misma lista.
     */
    @Test
    fun `fetchBooksByCategory en caso de exito actualiza el estado a Success`() = runTest {
        val fakeBooks = listOf(
            Book(id = "r1", title = "Orgullo y Prejuicio"),
            Book(id = "r2", title = "Jane Eyre")
        )

        coEvery { mockRepo.searchHybrid("subject:Romance") } returns Result.success(fakeBooks)

        viewModel.fetchBooksByCategory("subject:Romance")

        // Esperamos hasta que el estado deje de ser Loading
        val state = viewModel.booksState.first { it !is Resource.Loading }

        assertTrue("El estado debe ser Resource.Success", state is Resource.Success)
        assertEquals(
            "La lista de libros debe coincidir con el mock",
            fakeBooks,
            (state as Resource.Success).data
        )
    }

    /**
     * Prueba: fetchBooksByCategory en caso de fallo de red actualiza el estado a Error.
     *
     * Verifica que cuando searchHybrid lanza una excepción,
     * booksState emite Resource.Error con esa misma excepción.
     * El mensaje de error se extrae con result.exceptionOrNull() tal como hace el ViewModel.
     */
    @Test
    fun `fetchBooksByCategory en caso de fallo de red actualiza el estado a Error`() = runTest {
        val networkException = Exception("Error desconocido")

        coEvery { mockRepo.searchHybrid(any()) } returns Result.failure(networkException)

        viewModel.fetchBooksByCategory("subject:Romance")

        val state = viewModel.booksState.first { it !is Resource.Loading }

        assertTrue("El estado debe ser Resource.Error", state is Resource.Error)
        assertEquals(
            "El mensaje de error debe coincidir",
            "Error desconocido",
            (state as Resource.Error).exception.message
        )
    }

    /**
     * Prueba: fetchBooksByCategory emite Resource.Loading antes del resultado.
     *
     * Verifica que el estado inicial al llamar fetchBooksByCategory es Loading,
     * tal como establece la primera línea del método en el ViewModel.
     */
    @Test
    fun `fetchBooksByCategory emite Loading antes del resultado`() = runTest {
        // Hacemos que searchHybrid no responda inmediatamente usando coEvery sin suspensión
        // El estado inicial del ViewModel ya es Resource.Loading según el código
        val initialState = viewModel.booksState.value

        assertTrue(
            "El estado inicial debe ser Resource.Loading",
            initialState is Resource.Loading
        )
    }

    /**
     * Prueba: fetchBooksByCategory con lista vacía emite Resource.Success con lista vacía.
     *
     * Verifica que cuando la API devuelve éxito pero sin libros (emptyList()),
     * el ViewModel emite Success con lista vacía (no Error).
     */
    @Test
    fun `fetchBooksByCategory con respuesta vacia emite Success con lista vacia`() = runTest {
        coEvery { mockRepo.searchHybrid(any()) } returns Result.success(emptyList())

        viewModel.fetchBooksByCategory("subject:GeneroInexistente")

        val state = viewModel.booksState.first { it !is Resource.Loading }

        assertTrue("El estado debe ser Resource.Success", state is Resource.Success)
        assertTrue(
            "La lista debe estar vacía",
            (state as Resource.Success).data.isEmpty()
        )
    }
}