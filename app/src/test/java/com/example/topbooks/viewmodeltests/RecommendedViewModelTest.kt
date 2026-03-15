package com.example.topbooks.ui.home

import com.example.topbooks.MainDispatcherRule
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.CommunityRepository
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.utils.Resource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests para RecommendedViewModel.
 *
 * CORRECCIONES APLICADAS:
 *
 * PROBLEMA — MockK LIFO: el stub definido ÚLTIMO tiene prioridad.
 *
 * El error original era:
 *   Expected: [Book(p1, "El Nombre del Viento"), Book(p2, "Mistborn")]
 *   Actual:   []
 *
 * Causa: en el test se definía:
 *   1. coEvery { searchHybrid("bestsellers") } → fakePopular   (específico)
 *   2. coEvery { searchHybrid(any()) }          → emptyList    (general, ÚLTIMO)
 *
 * MockK usa LIFO: el stub más reciente gana. Como any() se registró después,
 * interceptaba TODAS las llamadas incluida "bestsellers", devolviendo emptyList.
 *
 * SOLUCIÓN: registrar any() en setUp() (primero = menor prioridad) y el
 * stub específico de cada test después de setUp() (último = mayor prioridad).
 * Así "bestsellers" va al stub específico y el resto al any() de setUp.
 *
 * Regla de oro con MockK:
 *   coEvery { f(any()) }          → en setUp    (fallback)
 *   coEvery { f("valor_exacto") } → en el test  (override, gana siempre)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecommendedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockBooksRepo: BooksRepository
    private lateinit var mockCommunityRepo: CommunityRepository
    private lateinit var mockUserRepo: UserRepository
    private lateinit var viewModel: RecommendedViewModel

    @Before
    fun setUp() {
        mockBooksRepo     = mockk(relaxed = true)
        mockCommunityRepo = mockk(relaxed = true)
        mockUserRepo      = mockk(relaxed = true)

        // ── Stubs de fallback (any()) — se registran primero = menor prioridad ──
        // Los tests específicos los sobreescriben registrando el stub DESPUÉS.
        coEvery { mockBooksRepo.searchHybrid(any()) }    returns Result.success(emptyList())
        coEvery { mockUserRepo.getCurrentUserId() }      returns "uid_test"
        coEvery { mockUserRepo.getFavoriteGenres(any()) } returns emptyList()
        coEvery { mockUserRepo.getFavoriteIds(any()) }   returns Result.success(emptyList())
        coEvery { mockCommunityRepo.getMyFriendsIds() }  returns Result.success(emptySet())

        viewModel = RecommendedViewModel(
            repository    = mockBooksRepo,
            communityRepo = mockCommunityRepo,
            userRepo      = mockUserRepo
        )
    }

    /**
     * Prueba: loadData emite Resource.Success con los libros devueltos por searchHybrid.
     *
     * Flujo real:
     *   loadData(popularQuery, fallbackQuery)
     *     → fetchPopularBooks(popularQuery)
     *       → repository.searchHybrid(popularQuery)
     *       → _popularBooks.value = Resource.Success(books)
     */
    @Test
    fun `loadData emite Success en popularBooks cuando searchHybrid devuelve libros`() = runTest {
        val fakePopular = listOf(
            Book(id = "p1", title = "El Nombre del Viento"),
            Book(id = "p2", title = "Mistborn")
        )

        // Este stub se registra DESPUÉS del any() de setUp → tiene prioridad (LIFO)
        coEvery { mockBooksRepo.searchHybrid("bestsellers") } returns Result.success(fakePopular)

        viewModel.loadData(
            popularQuery        = "bestsellers",
            fallbackTastesQuery = "fiction"
        )

        advanceUntilIdle()

        val state = viewModel.popularBooks.first { it !is Resource.Loading }

        assertTrue("popularBooks debe ser Resource.Success", state is Resource.Success)
        assertEquals(
            "La lista debe coincidir con el mock",
            fakePopular,
            (state as Resource.Success).data
        )
    }

    /**
     * Prueba: loadData emite Resource.Error cuando searchHybrid falla.
     *
     * Flujo real:
     *   fetchPopularBooks()
     *     → repository.searchHybrid() → Result.failure(exception)
     *     → _popularBooks.value = Resource.Error(exception)
     *
     */
    @Test
    fun `loadData emite Error en popularBooks cuando searchHybrid falla`() = runTest {
        val exception = Exception("Error loading popular books")

        // Stub específico registrado DESPUÉS de setUp → prioridad LIFO
        coEvery { mockBooksRepo.searchHybrid("bestsellers") } returns Result.failure(exception)

        viewModel.loadData(
            popularQuery        = "bestsellers",
            fallbackTastesQuery = "fiction"
        )

        advanceUntilIdle()

        val state = viewModel.popularBooks.first { it !is Resource.Loading }

        assertTrue("popularBooks debe ser Resource.Error", state is Resource.Error)
        assertEquals(
            "El mensaje de error debe coincidir",
            "Error loading popular books",
            (state as Resource.Error).exception.message
        )
    }

    /**
     * Prueba: loadData sin amigos emite Resource.Success con lista vacía en friendsBooks.
     *
     * Flujo real:
     *   fetchFriendsFavorites()
     *     → communityRepo.getMyFriendsIds() → emptySet()
     *     → _friendsBooks.value = Resource.Success(emptyList())
     */
    @Test
    fun `loadData sin amigos emite Success con lista vacia en friendsBooks`() = runTest {
        // getMyFriendsIds ya devuelve emptySet() en setUp, no necesitamos override

        viewModel.loadData(
            popularQuery        = "bestsellers",
            fallbackTastesQuery = "fiction"
        )

        advanceUntilIdle()

        val state = viewModel.friendsBooks.first { it !is Resource.Loading }

        assertTrue("friendsBooks debe ser Resource.Success", state is Resource.Success)
        assertTrue(
            "La lista debe estar vacía si no hay amigos",
            (state as Resource.Success).data.isEmpty()
        )
    }

    /**
     * Prueba: loadData no recarga si isDataLoaded ya es true.
     *
     * Flujo real:
     *   loadData() → isDataLoaded = true
     *   Segunda llamada → if (isDataLoaded) return  → no llama a la API
     */
    @Test
    fun `loadData no recarga si ya se habian cargado datos`() = runTest {
        val fakeBooks = listOf(Book(id = "1", title = "Dune"))

        // Stub específico registrado después de setUp → tiene prioridad
        coEvery { mockBooksRepo.searchHybrid("bestsellers") } returns Result.success(fakeBooks)

        // Primera carga
        viewModel.loadData("bestsellers", "fiction")
        advanceUntilIdle()
        val stateAfterFirst = viewModel.popularBooks.value

        // Segunda llamada — isDataLoaded = true → no hace nada
        viewModel.loadData("otros_bestsellers", "otra_ficcion")
        advanceUntilIdle()
        val stateAfterSecond = viewModel.popularBooks.value

        assertEquals(
            "El estado no debe cambiar en una segunda llamada a loadData",
            stateAfterFirst,
            stateAfterSecond
        )
    }
}