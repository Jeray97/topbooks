package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.tutorial.TutorialViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TutorialViewModelTest {

    private val booksRepo = mockk<BooksRepository>()
    private val userRepo = mockk<UserRepository>()
    private lateinit var viewModel: TutorialViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0

        viewModel = TutorialViewModel(booksRepo, userRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `toggleGenre anade el genero al estado y lanza busqueda`() = runTest {
        // GIVEN
        coEvery { booksRepo.getBooks(any(), any(), any(), any(), any()) } returns Result.success(emptyList())

        // Aseguramos que está vacío al principio
        assertTrue(viewModel.uiState.value.selectedGenres.isEmpty())

        // WHEN
        viewModel.toggleGenre("Fantasía")
        advanceUntilIdle()

        // THEN
        assertTrue(viewModel.uiState.value.selectedGenres.contains("Fantasía"))
        coVerify(exactly = 1) { booksRepo.getBooks(any(), any(), any(), any(), any()) }
    }
}