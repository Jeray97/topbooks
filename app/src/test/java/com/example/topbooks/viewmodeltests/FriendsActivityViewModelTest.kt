package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.friends.FriendsActivityViewModel
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
class FriendsActivityViewModelTest {

    private val feedRepository = mockk<SocialFeedRepository>()
    private val communityRepository = mockk<CommunityRepository>()
    private val userRepository = mockk<UserRepository>()
    private val booksRepository = mockk<BooksRepository>()

    private lateinit var viewModel: FriendsActivityViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Arrancamos de forma limpia sin que pete el INIT del viewModel
        coEvery { communityRepository.getMyFriendsIds() } returns Result.success(emptySet())

        viewModel = FriendsActivityViewModel(feedRepository, communityRepository, userRepository, booksRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadActivityFeed devuelve una lista vacia sin fallos si el usuario no tiene amigos`() = runTest {
        // GIVEN: El repositorio confirma que la lista de amigos está vacía
        coEvery { communityRepository.getMyFriendsIds() } returns Result.success(emptySet())

        // WHEN
        viewModel.loadActivityFeed()
        advanceUntilIdle()

        // THEN: El estado es un éxito controlado sin tener que hacer llamadas costosas
        val state = viewModel.uiState.value
        assertTrue("El estado debe ser Success", state is Resource.Success)
        assertTrue("La lista debe estar vacía", (state as Resource.Success).data.isEmpty())

        // Comprobamos que no hizo llamadas inútiles buscando libros
        coVerify(exactly = 0) { booksRepository.getBookDetail(any()) }
    }
}