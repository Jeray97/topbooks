package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.JournalRepository
import com.example.topbooks.ui.book.ReadingJournalViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingJournalViewModelTest {

    private val journalRepository = mockk<JournalRepository>()
    private lateinit var viewModel: ReadingJournalViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        viewModel = ReadingJournalViewModel(journalRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `deleteJournal en caso de exito marca saveSuccess a true para cerrar la pantalla`() = runTest {
        // GIVEN
        coEvery { journalRepository.deleteJournal(any()) } returns Result.success(true)

        // WHEN
        viewModel.deleteJournal("libro123")
        advanceUntilIdle()

        // THEN: La UI sabe que tiene que navegar hacia atrás
        assertTrue(viewModel.uiState.value.saveSuccess)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `loadJournal gestiona errores y los muestra en el estado`() = runTest {
        // GIVEN
        val errorMsg = "No se pudo cargar el diario"
        coEvery { journalRepository.getJournal(any()) } returns Result.failure(Exception(errorMsg))

        // WHEN
        viewModel.loadJournal("libro123")
        advanceUntilIdle()

        // THEN
        assertEquals(errorMsg, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoadingJournal)
    }
}