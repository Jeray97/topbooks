package com.example.topbooks.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * VIEWMODEL DEL FEED COMUNIDAD
 *
 * Mantiene el estado de:
 *  - Tab activa (Comunidad / Amigos / Top)
 *  - Lista de posts cargados según la tab
 *  - Story-bar de amigos lectores
 *  - Estados de like/save (optimistic UI: actualiza local antes de confirmar)
 *
 * Por ahora consume MockCommunityRepository (datos en memoria). Cuando
 * conectemos Firestore, sustituiremos el repositorio por uno real sin
 * cambiar la API pública del ViewModel.
 */
class CommunityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityFeedUiState())
    val uiState: StateFlow<CommunityFeedUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    /** Carga story-bar + posts de la tab inicial (Amigos por defecto). */
    private fun loadInitialData() {
        loadStories()
        selectTab(FeedTab.FRIENDS)
    }

    /** Recarga la story-bar (amigos leyendo ahora). */
    private fun loadStories() {
        viewModelScope.launch {
            MockCommunityRepository.getStories().collect { items ->
                _uiState.update { it.copy(stories = items) }
            }
        }
    }

    /**
     * Cambia la tab activa y recarga el feed correspondiente.
     * Marca isLoading = true mientras llegan los datos del repo (mock o real).
     */
    fun selectTab(tab: FeedTab) {
        _uiState.update { it.copy(activeTab = tab, isLoading = true) }
        viewModelScope.launch {
            MockCommunityRepository.getFeed(tab).collect { posts ->
                _uiState.update {
                    it.copy(
                        posts = posts,
                        isLoading = false,
                        newPostsCountToday = MockCommunityRepository.getNewPostsCountToday()
                    )
                }
            }
        }
    }

    /**
     * Optimistic-UI: actualizamos el post inmediatamente en el StateFlow para
     * que la animación del corazón sea instantánea, sin esperar a Firestore.
     */
    fun toggleLike(post: Post) {
        val updated = MockCommunityRepository.toggleLike(post)
        _uiState.update { state ->
            state.copy(posts = state.posts.map { if (it.id == post.id) updated else it })
        }
    }

    fun toggleSave(post: Post) {
        val updated = MockCommunityRepository.toggleSave(post)
        _uiState.update { state ->
            state.copy(posts = state.posts.map { if (it.id == post.id) updated else it })
        }
    }
}