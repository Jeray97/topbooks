package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.home.RecommendedViewModel
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
class RecommendedViewModelTest {

    private val booksRepo = mockk<BooksRepository>()
    private val communityRepo = mockk<CommunityRepository>()
    private val userRepo = mockk<UserRepository>()

    private lateinit var viewModel: RecommendedViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0

        viewModel = RecommendedViewModel(booksRepo, communityRepo, userRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadData actualiza los libros populares en caso de exito`() = runTest {
        // GIVEN
        coEvery { booksRepo.getBooks(any(), any(), any(), any(), any()) } returns Result.success(emptyList())
        coEvery { userRepo.getCurrentUserId() } returns null
        coEvery { communityRepo.getMyFriendsIds() } returns Result.success(emptySet())

        // WHEN
        viewModel.loadData("Populares", "Ficción")
        advanceUntilIdle()

        // THEN
        assertTrue(viewModel.popularBooks.value is Resource.Success)
    }
}