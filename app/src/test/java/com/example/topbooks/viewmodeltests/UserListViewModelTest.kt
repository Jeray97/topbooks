package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.profile.UserListViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

    private val progressRepo = mockk<ProgressRepository>()
    private val feedRepo = mockk<SocialFeedRepository>()
    private val communityRepo = mockk<CommunityRepository>()
    private val userRepo = mockk<UserRepository>()
    private val booksRepo = mockk<BooksRepository>()
    private val journalRepo = mockk<JournalRepository>()

    private lateinit var viewModel: UserListViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0

        viewModel = UserListViewModel(progressRepo, feedRepo, communityRepo, userRepo, booksRepo, journalRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadList read carga la lista de libros leidos`() = runTest {
        // GIVEN
        coEvery { progressRepo.getReadBooks(any()) } returns Result.success(emptyList())

        // WHEN
        viewModel.loadList("read", "user123")
        advanceUntilIdle()

        // THEN
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.readBooks)
    }
}