package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.book.BookDetailViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {

    private val booksRepository = mockk<BooksRepository>()
    private val progressRepository = mockk<ProgressRepository>()
    private val userRepository = mockk<UserRepository>()
    private val authRepository = mockk<AuthRepository>()

    private lateinit var viewModel: BookDetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        viewModel = BookDetailViewModel(booksRepository, progressRepository, userRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `toggleFavorite deshace el cambio de UI (Rollback) si falla la subida a Firebase`() = runTest {
        // GIVEN
        val mockBook = mockk<Book>(relaxed = true)

        // Asumimos que inicialmente no es favorito y la red falla al intentarlo
        coEvery { booksRepository.ensureBookExists(any()) } just Runs
        coEvery { progressRepository.toggleFavorite(any(), any()) } throws Exception("Error de red")

        // WHEN: El usuario pulsa el corazón
        viewModel.toggleFavorite(mockBook)

        // THEN: La corrutina atrapa el fallo y devuelve el botón a su estado original (false)
        advanceUntilIdle()
        assertFalse("El botón debió volver a su estado normal (no favorito)", viewModel.uiState.value.isFavorite)
    }
}