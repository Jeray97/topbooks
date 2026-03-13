package com.example.topbooks.ui.search

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.ui.components.SearchBarCustom
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

/**
 * PRUEBA DE INTEGRACIÓN: UI + ViewModel + Mock Repository
 * * Verificamos el flujo completo de búsqueda incluyendo el manejo de tiempos asíncronos.
 */
class SearchScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun flujoDeBusquedaMuestraResultadosCorrectamente() {
        // GIVEN
        val mockRepository = mockk<BooksRepository>()
        val libro1 = Book(id = "1", title = "El Quijote", authors = listOf("Cervantes"))
        val listaSimulada = listOf(libro1)

        coEvery { mockRepository.searchHybrid(any<String>()) } returns Result.success(listaSimulada)

        val viewModel = SearchViewModel(mockRepository)

        composeTestRule.setContent {
            SearchBarCustom(
                viewModel = viewModel,
                onBookClick = { },
                onScanClick = { }
            )
        }

        // 1. Escribimos en el buscador
        composeTestRule.onNodeWithTag("search_input").performTextInput("Quijote")

        // 2. TÉCNICA: Esperamos a que el debounce termine y aparezca el resultado
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("El Quijote").fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Verificamos
        composeTestRule.onNodeWithText("El Quijote").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cervantes").assertIsDisplayed()
    }

    @Test
    fun flujoDeBusquedaMuestraIndicadorDeCarga() {
        // GIVEN
        val mockRepository = mockk<BooksRepository>()

        // Mock con retraso de 5s para que nos de tiempo a ver el spinner
        coEvery { mockRepository.searchHybrid(any<String>()) } coAnswers {
            delay(5000)
            Result.success(emptyList<Book>())
        }

        val viewModel = SearchViewModel(mockRepository)

        composeTestRule.setContent {
            SearchBarCustom(
                viewModel = viewModel,
                onBookClick = {},
                onScanClick = {}
            )
        }

        // 1. Disparamos la búsqueda
        composeTestRule.onNodeWithTag("search_input").performTextInput("Buscando")

        // 2. TÉCNICA CRÍTICA: Esperamos a que aparezca el spinner (tras los 800ms de debounce)
        // pero NO usamos waitForIdle para no esperar a que la carga de 5s termine.
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("loading_spinner").fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Ahora sí, el componente debe estar visible
        composeTestRule.onNodeWithTag("loading_spinner").assertIsDisplayed()
    }
}