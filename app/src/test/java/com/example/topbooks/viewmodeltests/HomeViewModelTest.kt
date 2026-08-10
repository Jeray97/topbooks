package com.example.topbooks.viewmodeltests

import android.util.Log
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.home.HomeViewModel
import com.example.topbooks.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val booksRepo = mockk<BooksRepository>()
    private val communityRepo = mockk<CommunityRepository>()
    private val userRepo = mockk<UserRepository>()

    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        // Protegemos el test de las llamadas estáticas a Firebase que hay dentro de HomeViewModel
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance().currentUser } returns null

        mockkStatic(FirebaseFirestore::class)
        val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockFirestore

        viewModel = HomeViewModel(booksRepo, communityRepo, userRepo, null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadData carga libros por categoria correctamente`() = runTest {
        // GIVEN
        coEvery { booksRepo.getBooks(any(), any(), any(), any(), any()) } returns Result.success(emptyList())
        coEvery { communityRepo.getMyFriendsIds() } returns Result.success(emptySet())

        // WHEN
        viewModel.loadData("Fiction", "Bestseller")
        advanceUntilIdle()

        // THEN
        val state = viewModel.categoryBooks.value
        assertTrue("El estado de las categorías debe ser Success", state is Resource.Success)
    }
}