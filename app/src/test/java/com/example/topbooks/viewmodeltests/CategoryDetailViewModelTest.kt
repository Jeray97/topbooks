package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.ui.category.CategoryDetailViewModel
import com.example.topbooks.utils.Resource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryDetailViewModelTest {

    private val booksRepository = mockk<BooksRepository>()
    private lateinit var viewModel: CategoryDetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        viewModel = CategoryDetailViewModel(booksRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `fetchBooksByCategory en caso de exito actualiza el estado a Success`() = runTest {
        // GIVEN: El repositorio devuelve una lista vacía (éxito sin datos)
        coEvery { booksRepository.getBooks(any(), any(), any(), any(), any()) } returns Result.success(emptyList())

        // WHEN
        viewModel.fetchBooksByCategory("subject:Romance")
        advanceUntilIdle()

        // THEN: El estado final debe ser Success
        val state = viewModel.booksState.value
        assertTrue("Debería ser un estado de éxito", state is Resource.Success)
    }

    @Test
    fun `fetchBooksByCategory en caso de fallo de red actualiza el estado a Error`() = runTest {
        // GIVEN: El repositorio falla (ej. sin internet)
        val error = Exception("Sin conexión")
        coEvery { booksRepository.getBooks(any(), any(), any(), any(), any()) } returns Result.failure(error)

        // WHEN
        viewModel.fetchBooksByCategory("subject:Romance")
        advanceUntilIdle()

        // THEN
        val state = viewModel.booksState.value
        assertTrue("Debería ser un estado de error", state is Resource.Error)
    }
}