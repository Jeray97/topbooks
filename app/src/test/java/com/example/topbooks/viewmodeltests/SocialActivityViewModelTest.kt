package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.friends.SocialActivityViewModel
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
class SocialActivityViewModelTest {

    private val feedRepository = mockk<SocialFeedRepository>()
    private val communityRepository = mockk<CommunityRepository>()
    private val userRepository = mockk<UserRepository>()
    private val booksRepository = mockk<BooksRepository>()

    private lateinit var viewModel: SocialActivityViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        coEvery { communityRepository.getMyFriendsIds() } returns Result.success(emptySet())

        viewModel = SocialActivityViewModel(feedRepository, communityRepository, userRepository, booksRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadActivityFeed captura excepciones inesperadas y devuelve Resource Error`() = runTest {
        // GIVEN: El servicio de comunidad se cae y lanza una excepción dura
        val exception = RuntimeException("Fallo catastrófico del servidor")
        coEvery { communityRepository.getMyFriendsIds() } throws exception

        // WHEN
        viewModel.loadActivityFeed()
        advanceUntilIdle()

        // THEN: El ViewModel protege la app y envuelve el fallo en la clase Resource
        val state = viewModel.uiState.value
        assertTrue(state is Resource.Error)
        assertEquals("Fallo catastrófico del servidor", (state as Resource.Error).exception.message)
    }
}