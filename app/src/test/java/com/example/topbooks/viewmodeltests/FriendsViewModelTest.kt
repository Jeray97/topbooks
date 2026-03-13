package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.CommunityRepository
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.ui.friends.FriendsViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

    private val communityRepository = mockk<CommunityRepository>()
    private val userRepository = mockk<UserRepository>()
    private lateinit var viewModel: FriendsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Mock para la carga inicial requerida en el init{}
        coEvery { communityRepository.getMyFriendsIds() } returns Result.success(emptySet())
        coEvery { userRepository.getCurrentUserId() } returns "miUid"
        coEvery { communityRepository.getSuggestedUsers(any()) } returns Result.success(emptyList())

        viewModel = FriendsViewModel(communityRepository, userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `onSearchQueryChanged limpia los resultados si se borra el texto`() = runTest {
        // GIVEN: Un texto vacío
        val query = ""

        // WHEN
        viewModel.onSearchQueryChanged(query)
        advanceUntilIdle()

        // THEN: No hay peticiones a la base de datos y se vacía el estado
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
        assertFalse(viewModel.uiState.value.isSearching)
        coVerify(exactly = 0) { communityRepository.searchUsers(any()) }
    }
}