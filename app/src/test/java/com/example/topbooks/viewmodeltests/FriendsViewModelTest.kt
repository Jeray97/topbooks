package com.example.topbooks.ui.friends

import android.util.Log
import com.example.topbooks.MainDispatcherRule
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.CommunityRepository
import com.example.topbooks.data.repository.SocialFeedRepository
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.ui.profile.SimpleUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests para FriendsViewModel.
 *
 * CORRECCIONES APLICADAS:
 *
 * PROBLEMA 1 — android.util.Log not mocked
 * El ViewModel usa Log.d, Log.e, Log.w en loadInitialData() y refreshRecentActivity().
 * En tests JVM puro, android.util.Log no existe y lanza:
 *   RuntimeException: Method e in android.util.Log not mocked
 * SOLUCIÓN: mockkStatic(Log::class) + every { Log.X(...) } returns 0
 * antes de crear el ViewModel. Así todas las llamadas a Log devuelven 0
 * silenciosamente en lugar de explotar.
 *
 * PROBLEMA 2 — init { } lanza corrutinas inmediatamente
 * El ViewModel llama a loadInitialData() en init { }, así que todos los mocks
 * deben estar configurados ANTES de instanciar el ViewModel en setUp().
 * La MainDispatcherRule garantiza que Dispatchers.Main esté activo antes de @Before.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

    // CRÍTICO: Rule se aplica ANTES de @Before, garantizando que
    // Dispatchers.Main esté activo cuando setUp() crea el ViewModel.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockCommunityRepo: CommunityRepository
    private lateinit var mockUserRepo: UserRepository
    private lateinit var mockFeedRepo: SocialFeedRepository
    private lateinit var mockBooksRepo: BooksRepository
    private lateinit var viewModel: FriendsViewModel

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) }          returns 0
        every { Log.e(any(), any<String>()) }  returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) }  returns 0
        every { Log.i(any(), any()) }          returns 0

        mockCommunityRepo = mockk(relaxed = true)
        mockUserRepo      = mockk(relaxed = true)
        mockFeedRepo      = mockk(relaxed = true)
        mockBooksRepo     = mockk(relaxed = true)

        coEvery { mockUserRepo.getCurrentUserId() }    returns "uid_test"
        coEvery { mockUserRepo.getFriendsList(any()) } returns Result.success(emptyList())
        coEvery { mockCommunityRepo.getSuggestedUsers(any()) } returns Result.success(emptyList())

        viewModel = FriendsViewModel(
            communityRepository = mockCommunityRepo,
            userRepository      = mockUserRepo,
            feedRepository      = mockFeedRepo,
            booksRepository     = mockBooksRepo
        )
    }

    @After
    fun tearDown() {
        // Limpiamos el mock estático de Log para no contaminar otros tests
        unmockkStatic(Log::class)
    }

    /**
     * Prueba: loadInitialData carga la lista de amigos correctamente en uiState.
     *
     * Flujo real:
     *   init → loadInitialData()
     *     → userRepository.getFriendsList(uid)
     *     → _uiState.update { myFriends = socialFriends, friendsIds = ... }
     */
    @Test
    fun `loadInitialData carga lista de amigos en uiState`() = runTest {
        val fakeFriends = listOf(
            SimpleUser(uid = "uid_amigo1", name = "Ana García",   photo = "capibara_1"),
            SimpleUser(uid = "uid_amigo2", name = "Carlos López", photo = "capibara_2")
        )

        coEvery { mockUserRepo.getFriendsList("uid_test") } returns Result.success(fakeFriends)

        // Recreamos el ViewModel para que init {} use el stub actualizado
        viewModel = FriendsViewModel(
            communityRepository = mockCommunityRepo,
            userRepository      = mockUserRepo,
            feedRepository      = mockFeedRepo,
            booksRepository     = mockBooksRepo
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("myFriends debe tener 2 elementos", 2, state.myFriends.size)
        assertEquals("uid_amigo1", state.myFriends[0].uid)
        assertEquals("Ana García", state.myFriends[0].displayName)
        assertTrue("isFriend debe ser true para los amigos cargados", state.myFriends[0].isFriend)
    }

    /**
     * Prueba: loadInitialData sin sesión activa no modifica el estado.
     *
     * Flujo real:
     *   loadInitialData()
     *     → val currentUserId = userRepository.getCurrentUserId() ?: return@launch
     *     → Si null, retorna sin modificar el estado
     */
    @Test
    fun `loadInitialData sin sesion activa no modifica el estado`() = runTest {
        coEvery { mockUserRepo.getCurrentUserId() } returns null

        viewModel = FriendsViewModel(
            communityRepository = mockCommunityRepo,
            userRepository      = mockUserRepo,
            feedRepository      = mockFeedRepo,
            booksRepository     = mockBooksRepo
        )

        advanceUntilIdle()

        assertTrue(
            "myFriends debe estar vacío si no hay sesión",
            viewModel.uiState.value.myFriends.isEmpty()
        )
    }

    /**
     * Prueba: onSearchQueryChanged con query en blanco limpia searchResults.
     *
     * Flujo real:
     *   onSearchQueryChanged("")
     *     → if (query.isBlank()) → searchResults = emptyList(), isSearching = false
     */
    @Test
    fun `onSearchQueryChanged con query en blanco limpia resultados`() = runTest {
        viewModel.onSearchQueryChanged("")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("searchResults debe estar vacío", state.searchResults.isEmpty())
        assertFalse("isSearching debe ser false", state.isSearching)
    }

    /**
     * Prueba: toggleFriend aplica actualización optimista en friendsIds antes de
     * que la red confirme la operación.
     *
     * Flujo real:
     *   toggleFriend(user)
     *     → updateUserFriendStatus(uid, !isFriend)  ← actualiza estado LOCAL inmediatamente
     *     → viewModelScope.launch { userRepository.toggleFriendship(...) }
     */
    @Test
    fun `toggleFriend aplica actualizacion optimista en friendsIds`() = runTest {
        coEvery {
            mockUserRepo.toggleFriendship(any(), any(), any(), any(), any())
        } returns Result.success(true)

        val fakeUser = SocialUser(uid = "uid_nuevo", displayName = "Pedro", isFriend = false)

        viewModel.toggleFriend(fakeUser)
        advanceUntilIdle()

        assertTrue(
            "friendsIds debe contener el uid del nuevo amigo tras toggleFriend",
            viewModel.uiState.value.friendsIds.contains("uid_nuevo")
        )
    }
}