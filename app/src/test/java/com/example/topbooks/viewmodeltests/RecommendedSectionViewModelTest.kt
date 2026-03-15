package com.example.topbooks.viewmodeltests

import com.example.topbooks.MainDispatcherRule
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.CommunityRepository
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.ui.home.RecommendedSectionViewModel
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
 * Tests para RecommendedSectionViewModel.
 *
 * PROBLEMA QUE CORREGIMOS:
 * - BooksRepository es una clase concreta (no interfaz), así que usamos mockk<BooksRepository>()
 *   de MockK, que puede mockear clases Kotlin final usando el agente de byte-code.
 * - viewModelScope.launch usa Dispatchers.Main → necesita MainDispatcherRule para funcionar
 *   en un entorno JVM sin Android.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecommendedSectionViewModelTest {

    // Sustituye Dispatchers.Main por UnconfinedTestDispatcher antes de cada test
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mocks — BooksRepository es clase concreta, se mockea con MockK (no Mockito)
    private lateinit var mockBooksRepo: BooksRepository
    private lateinit var mockCommunityRepo: CommunityRepository
    private lateinit var mockUserRepo: UserRepository
    private lateinit var viewModel: RecommendedSectionViewModel

    @Before
    fun setUp() {
        mockBooksRepo    = mockk(relaxed = true)
        mockCommunityRepo = mockk(relaxed = true)
        mockUserRepo     = mockk(relaxed = true)

        // Creamos el ViewModel inyectando los mocks
        viewModel = RecommendedSectionViewModel(
            repository    = mockBooksRepo,
            communityRepo = mockCommunityRepo,
            userRepo      = mockUserRepo
        )
    }

    /**
     * Prueba: loadSectionData con sección "popular" llama a searchHybrid y emite Resource.Success
     * con la lista de libros devuelta por el repositorio.
     *
     * Flujo real del código:
     *   loadSectionData("popular", query)
     *     → fetchData()
     *       → fetchFromApi() con currentPage == 1
     *         → repository.searchHybrid(query)   ← interceptamos aquí
     *         → _booksState.value = Resource.Success(books)
     */
    @Test
    fun `loadSectionData con seccion popular carga datos de la API`() = runTest {
        val fakeBooks = listOf(
            Book(id = "1", title = "Dune"),
            Book(id = "2", title = "Foundation")
        )

        // searchHybrid devuelve éxito con la lista de libros fake
        coEvery { mockBooksRepo.searchHybrid(any()) } returns Result.success(fakeBooks)

        viewModel.loadSectionData(type = "popular", genre = "sci-fi")

        val state = viewModel.booksState.first { it !is Resource.Loading }

        assertTrue("El estado debe ser Success", state is Resource.Success)
        assertEquals(
            "La lista debe contener los libros del mock",
            fakeBooks,
            (state as Resource.Success).data
        )
    }

    /**
     * Prueba: loadSectionData con sección "popular" y fallo de red emite Resource.Error
     * cuando searchHybrid lanza una excepción.
     *
     * Flujo real:
     *   fetchFromApi() → repository.searchHybrid() lanza Exception
     *     → catch (e: Exception) → if (currentPage == 1) _booksState.value = Resource.Error(e)
     */
    @Test
    fun `loadSectionData con fallo de red emite Resource Error`() = runTest {
        val networkError = Exception("Sin conexión a internet")

        coEvery { mockBooksRepo.searchHybrid(any()) } throws networkError

        viewModel.loadSectionData(type = "popular", genre = "sci-fi")

        val state = viewModel.booksState.first { it !is Resource.Loading }

        assertTrue("El estado debe ser Error", state is Resource.Error)
        assertEquals(
            "El mensaje de error debe coincidir",
            "Sin conexión a internet",
            (state as Resource.Error).exception.message
        )
    }

    /**
     * Prueba: loadSectionData con sección "friends" y sin amigos emite Resource.Success con lista vacía.
     *
     * Flujo real:
     *   fetchFriendsFavorites()
     *     → communityRepo.getMyFriendsIds() → emptySet()
     *     → _booksState.value = Resource.Success(emptyList())
     */
    @Test
    fun `loadSectionData friends sin amigos emite lista vacia`() = runTest {
        coEvery { mockCommunityRepo.getMyFriendsIds() } returns Result.success(emptySet())

        viewModel.loadSectionData(type = "friends", genre = "")

        val state = viewModel.booksState.first { it !is Resource.Loading }

        assertTrue("El estado debe ser Success", state is Resource.Success)
        assertTrue(
            "La lista debe estar vacía si no hay amigos",
            (state as Resource.Success).data.isEmpty()
        )
    }

    /**
     * Prueba: loadSectionData no recarga si se llama dos veces con los mismos parámetros.
     *
     * Flujo real:
     *   loadSectionData comprueba if (currentType != type || currentGenre != genre)
     *   Si son iguales, NO llama a fetchData() ni a la API.
     */
    @Test
    fun `loadSectionData no recarga si type y genre no cambian`() = runTest {
        val fakeBooks = listOf(Book(id = "1", title = "Dune"))
        coEvery { mockBooksRepo.searchHybrid(any()) } returns Result.success(fakeBooks)

        // Primera llamada — carga datos
        viewModel.loadSectionData(type = "popular", genre = "sci-fi")
        val stateAfterFirst = viewModel.booksState.value

        // Segunda llamada con los mismos parámetros — no debe recargar
        viewModel.loadSectionData(type = "popular", genre = "sci-fi")
        val stateAfterSecond = viewModel.booksState.value

        // El estado no debe haber cambiado (sigue siendo el mismo Success)
        assertEquals(stateAfterFirst, stateAfterSecond)
    }
}