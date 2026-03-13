package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.scanner.ScannerViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    private val booksRepo = mockk<BooksRepository>()
    private lateinit var viewModel: ScannerViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        viewModel = ScannerViewModel(booksRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `onIsbnDetected actualiza foundBook si el libro existe`() = runTest {
        // GIVEN
        val mockBook = mockk<Book>(relaxed = true)
        coEvery { booksRepo.getBooks(any(), any(), any(), any(), any()) } returns Result.success(listOf(mockBook))

        // WHEN
        viewModel.onIsbnDetected("9781234567890")
        advanceUntilIdle()

        // THEN
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.foundBook)
        assertNull(viewModel.uiState.value.notFoundIsbn)
    }
}