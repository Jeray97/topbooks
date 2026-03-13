package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.reviews.ReviewsViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsViewModelTest {

    private val feedRepo = mockk<SocialFeedRepository>()
    private val communityRepo = mockk<CommunityRepository>()
    private val userRepo = mockk<UserRepository>()
    private val booksRepo = mockk<BooksRepository>()
    private val authRepo = mockk<AuthRepository>()

    private lateinit var viewModel: ReviewsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0

        coEvery { communityRepo.getMyFriendsIds() } returns Result.success(emptySet())
        coEvery { feedRepo.getCommunityComments(any()) } returns Result.success(emptyList())

        viewModel = ReviewsViewModel(feedRepo, communityRepo, userRepo, booksRepo, authRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadSocialFeed enriquece y filtra las listas correctamente sin amigos`() = runTest {
        // GIVEN: Setup devuelve 0 amigos y 0 comentarios

        // WHEN
        viewModel.loadSocialFeed(bookId = null)
        advanceUntilIdle()

        // THEN
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.friendsReviews.isEmpty())
        assertTrue(viewModel.uiState.value.communityReviews.isEmpty())
    }
}