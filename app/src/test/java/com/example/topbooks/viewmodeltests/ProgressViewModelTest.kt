package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.progress.ProgressViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.collections.emptySet

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private val progressRepo = mockk<ProgressRepository>()
    private val userRepo = mockk<UserRepository>()
    private val booksRepo = mockk<BooksRepository>()
    private val journalRepo = mockk<JournalRepository>()

    private lateinit var viewModel: ProgressViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0

        // Configuramos mocks para que el init{} del ViewModel no rompa el test
        coEvery { userRepo.getCurrentUserId() } returns "miUID"
        coEvery { progressRepo.getReadBooks(any()) } returns Result.success(emptyList())
        coEvery { progressRepo.getBookmarks(any()) } returns Result.success(emptyList())
        coEvery { userRepo.getFavoriteCovers(any(), any()) } returns Result.success(emptyList())
        coEvery { userRepo.getFavoriteIds(any()) } returns Result.success(emptySet<String>()) as Result<List<String>>
        coEvery { journalRepo.getAllJournals(any()) } returns Result.success(emptyList())

        viewModel = ProgressViewModel(progressRepo, userRepo, booksRepo, journalRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadProgressData unifica todas las listas y apaga isLoading`() = runTest {
        // GIVEN: Los datos ya están preparados en el setup

        // WHEN
        viewModel.loadProgressData()
        advanceUntilIdle()

        // THEN
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.read)
        assertNotNull(viewModel.uiState.value.pending)
    }
}