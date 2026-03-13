package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.reviews.SingleCommentViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SingleCommentViewModelTest {

    private val feedRepo = mockk<SocialFeedRepository>()
    private val userRepo = mockk<UserRepository>()
    private val booksRepo = mockk<BooksRepository>()
    private val authRepo = mockk<AuthRepository>()

    private lateinit var viewModel: SingleCommentViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0

        viewModel = SingleCommentViewModel(feedRepo, userRepo, booksRepo, authRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `sendReply con texto vacio o solo espacios no hace nada`() = runTest {
        // GIVEN
        val text = "    " // Texto inválido

        // WHEN
        viewModel.sendReply(text, onSuccess = {})
        advanceUntilIdle()

        // THEN: No se envía a Firebase y no se marca como guardando
        assertFalse(viewModel.uiState.value.isSendingReply)
        coVerify(exactly = 0) { feedRepo.addReply(any(), any(), any(), any()) }
    }
}