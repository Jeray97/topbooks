package com.example.topbooks

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.BookItem
import com.example.topbooks.data.model.VolumeInfo
import com.example.topbooks.data.repository.AuthRepository
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.ui.auth.AuthViewModel
import com.example.topbooks.ui.home.HomeViewModel
import com.example.topbooks.utils.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TopBooksUnitTests {

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private val authRepo = mockk<AuthRepository>(relaxed = true)
    private val booksRepo = mockk<BooksRepository>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- GRUPO 1: AuthViewModel ---

    @Test // UT-01
    fun `login success updates state to Success`() = runTest {
        // GIVEN: Inyectamos el repo falso en el constructor
        val viewModel = AuthViewModel(authRepo)
        coEvery { authRepo.login("test@ok.com", "123") } returns Result.success(true)

        // WHEN
        viewModel.login("test@ok.com", "123")
        advanceUntilIdle()

        // THEN: Comprobamos que el estado es Success
        assertTrue(viewModel.authState.value is Resource.Success)
    }

    @Test // UT-02
    fun `login failure updates state to Error`() = runTest {
        val viewModel = AuthViewModel(authRepo)
        coEvery { authRepo.login(any(), any()) } returns Result.failure(Exception("Error Red"))

        viewModel.login("bad@mail.com", "000")
        advanceUntilIdle()

        // THEN: Comprobamos que el estado es Error
        assertTrue(viewModel.authState.value is Resource.Error)
    }

    @Test // UT-03
    fun `register success updates state`() = runTest {
        val viewModel = AuthViewModel(authRepo)
        coEvery { authRepo.register(any(), any(), any()) } returns Result.success(true)

        viewModel.register("Pepe", "pepe@test.com", "123456")
        advanceUntilIdle()

        assertTrue(viewModel.authState.value is Resource.Success)
    }

    @Test // UT-05
    fun `signOut resets state and calls repo`() = runTest {
        val viewModel = AuthViewModel(authRepo)

        viewModel.signOut()

        verify { authRepo.logout() }
        assertTrue(viewModel.authState.value is Resource.Idle)
    }

    // --- GRUPO 2: HomeViewModel ---

    @Test // UT-06
    fun `category search success populates list`() = runTest {
        // GIVEN
        val viewModel = HomeViewModel(booksRepo)
        val fakeList = listOf(Book("1", "Libro Test", listOf("Autor"), "Desc", "", "2025"))

        // Simulamos que el repo devuelve éxito cuando buscamos "subject:romance"
        coEvery { booksRepo.getBooks("subject:romance") } returns Result.success(fakeList)

        // WHEN
        viewModel.onCategorySelected("romance")
        advanceUntilIdle()

        // THEN: Accedemos a 'categoryBooks'
        val state = viewModel.categoryBooks.value

        // 1. Verificamos que sea Success
        assertTrue(state is Resource.Success)

        // 2. Verificamos que tenga 1 libro dentro
        assertEquals(1, (state as Resource.Success).data.size)
        assertEquals("Libro Test", state.data[0].title)
    }

    @Test // UT-09
    fun `category search error updates state`() = runTest {
        val viewModel = HomeViewModel(booksRepo)
        coEvery { booksRepo.getBooks(any()) } returns Result.failure(Exception("Error 500"))

        viewModel.onCategorySelected("romance")
        advanceUntilIdle()

        // THEN: Verificamos que 'categoryBooks' esté en estado Error
        val state = viewModel.categoryBooks.value
        assertTrue(state is Resource.Error)
        assertEquals("Error 500", (state as Resource.Error).exception.message)
    }

    // --- GRUPO 3: Mapeadores (Data Logic) ---

    @Test // UT-10
    fun `mapper handles null values correctly`() {
        // GIVEN
        val dirtyItem = BookItem(
            id = null,
            volumeInfo = VolumeInfo(title = null, authors = null, description = null, imageLinks = null, "")
        )

        // WHEN
        val cleanBook = dirtyItem.toDomain()

        // THEN
        assertEquals("unknown_id", cleanBook.id) // "unknown_id" si es null
        assertEquals("Sin título", cleanBook.title) // "Sin título"
    }
}