package com.example.topbooks.viewmodeltests

import com.example.topbooks.MainDispatcherRule
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.ui.scanner.ScannerViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests para ScannerViewModel.
 *
 * PROBLEMAS QUE CORREGIMOS:
 * 1. BooksRepository es clase concreta → mockk<BooksRepository>()
 * 2. onIsbnDetected() usa viewModelScope.launch → necesita MainDispatcherRule
 * 3. onIsbnDetected() tiene un guard clause que ignora llamadas si el estado no está limpio:
 *      if (isLoading || notFoundIsbn != null || foundBook != null) return
 *    Por eso creamos un nuevo ViewModel en cada @Before, garantizando siempre
 *    el estado inicial limpio ScannerUiState().
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockRepo: BooksRepository
    private lateinit var viewModel: ScannerViewModel

    @Before
    fun setUp() {
        mockRepo  = mockk(relaxed = true)
        // Nueva instancia en cada test para resetear el guard clause del estado
        viewModel = ScannerViewModel(repository = mockRepo)
    }

    /**
     * Prueba: onIsbnDetected con ISBN que encuentra un libro actualiza foundBook.
     *
     * Flujo real:
     *   onIsbnDetected(isbn)
     *     → _uiState.update { isLoading = true }
     *     → repository.getBookByIsbn(isbn)
     *     → book != null
     *     → BooksRepository.lastScannedBook = book  (caché global)
     *     → _uiState.update { isLoading = false, foundBook = book }
     */
    @Test
    fun `onIsbnDetected con libro encontrado actualiza foundBook en uiState`() = runTest {
        val fakeBook = Book(id = "book_isbn_1", title = "El Nombre del Viento")

        coEvery { mockRepo.getBookByIsbn("9788445073490") } returns Result.success(fakeBook)

        viewModel.onIsbnDetected("9788445073490")

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse("isLoading debe ser false tras completar la búsqueda", state.isLoading)
        assertNotNull("foundBook no debe ser null cuando se encuentra el libro", state.foundBook)
        assertEquals("El título debe coincidir", "El Nombre del Viento", state.foundBook?.title)
        assertNull("notFoundIsbn debe ser null cuando se encuentra el libro", state.notFoundIsbn)
    }

    /**
     * Prueba: onIsbnDetected con ISBN sin resultados actualiza notFoundIsbn.
     *
     * Flujo real:
     *   onIsbnDetected(isbn)
     *     → repository.getBookByIsbn(isbn) → Result.success(null)
     *     → book == null
     *     → _uiState.update { isLoading = false, notFoundIsbn = isbn }
     */
    @Test
    fun `onIsbnDetected sin libro encontrado actualiza notFoundIsbn`() = runTest {
        // getBookByIsbn devuelve éxito pero con null (sin resultados)
        coEvery { mockRepo.getBookByIsbn("0000000000000") } returns Result.success(null)

        viewModel.onIsbnDetected("0000000000000")

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse("isLoading debe ser false", state.isLoading)
        assertNull("foundBook debe ser null cuando no se encuentra el libro", state.foundBook)
        assertEquals(
            "notFoundIsbn debe contener el ISBN buscado",
            "0000000000000",
            state.notFoundIsbn
        )
    }

    /**
     * Prueba: onIsbnDetected guarda el libro en BooksRepository.lastScannedBook (caché global).
     *
     * Flujo real:
     *   → BooksRepository.lastScannedBook = book
     * Esta caché permite a BookDetailScreen leer el libro sin rellamar a la API.
     */
    @Test
    fun `onIsbnDetected guarda el libro en la cache global lastScannedBook`() = runTest {
        val fakeBook = Book(id = "cached_book", title = "Dune")

        coEvery { mockRepo.getBookByIsbn(any()) } returns Result.success(fakeBook)

        viewModel.onIsbnDetected("9780593099322")

        advanceUntilIdle()

        assertEquals(
            "lastScannedBook debe tener el libro encontrado",
            fakeBook,
            BooksRepository.lastScannedBook
        )
    }

    /**
     * Prueba: onIsbnDetected ignora llamadas mientras isLoading es true (guard clause).
     *
     * Flujo real:
     *   if (currentState.isLoading || ...) return
     * Si el estado ya tiene isLoading = true, la función retorna inmediatamente sin
     * lanzar otra corrutina.
     */
    @Test
    fun `onIsbnDetected ignora segunda llamada si ya hay una en curso`() = runTest {
        val fakeBook = Book(id = "b1", title = "Primer libro")

        // Primer call encontrará el libro
        coEvery { mockRepo.getBookByIsbn("ISBN_1") } returns Result.success(fakeBook)

        viewModel.onIsbnDetected("ISBN_1")
        advanceUntilIdle()

        // El estado ahora tiene foundBook != null, el guard clause bloqueará la siguiente llamada
        val stateAfterFirst = viewModel.uiState.value
        assertNotNull(stateAfterFirst.foundBook)

        // Segunda llamada — el guard clause devuelve sin hacer nada
        viewModel.onIsbnDetected("ISBN_2")
        advanceUntilIdle()

        // El estado no debe haber cambiado
        assertEquals(stateAfterFirst, viewModel.uiState.value)
    }

    /**
     * Prueba: dismissError limpia notFoundIsbn del estado.
     *
     * Flujo real:
     *   dismissError()
     *     → _uiState.update { it.copy(notFoundIsbn = null) }
     */
    @Test
    fun `dismissError limpia notFoundIsbn`() = runTest {
        coEvery { mockRepo.getBookByIsbn(any()) } returns Result.success(null)

        viewModel.onIsbnDetected("9999999999999")
        advanceUntilIdle()

        // Precondición: notFoundIsbn debe estar relleno
        assertNotNull(viewModel.uiState.value.notFoundIsbn)

        viewModel.dismissError()

        assertNull(
            "notFoundIsbn debe ser null tras llamar a dismissError()",
            viewModel.uiState.value.notFoundIsbn
        )
    }

    /**
     * Prueba: dismissBookInfo limpia foundBook del estado.
     *
     * Flujo real:
     *   dismissBookInfo()
     *     → _uiState.update { it.copy(foundBook = null) }
     */
    @Test
    fun `dismissBookInfo limpia foundBook`() = runTest {
        val fakeBook = Book(id = "b1", title = "Dune")
        coEvery { mockRepo.getBookByIsbn(any()) } returns Result.success(fakeBook)

        viewModel.onIsbnDetected("9780593099322")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.foundBook)

        viewModel.dismissBookInfo()

        assertNull(
            "foundBook debe ser null tras llamar a dismissBookInfo()",
            viewModel.uiState.value.foundBook
        )
    }
}