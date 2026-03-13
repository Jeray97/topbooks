package com.example.topbooks

import android.util.Log
import com.example.topbooks.data.network.BooksApiService
import com.example.topbooks.data.network.RetrofitClient
import com.example.topbooks.data.repository.BooksRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BooksRepositoryTest {

    private lateinit var repository: BooksRepository

    // Mocks para las dependencias internas
    private val mockFirestore = mockk<FirebaseFirestore>()
    private val mockCollection = mockk<CollectionReference>()
    private val mockSnapshot = mockk<QuerySnapshot>()

    private val mockApiService = mockk<BooksApiService>()

    @Before
    fun setup() {
        // 1. Interceptamos el Log de Android
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // 2. Interceptamos la creación de Firebase
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockFirestore

        // Preparamos el camino "feliz" falso para Firebase por defecto
        every { mockFirestore.collection("books") } returns mockCollection
        // Hacemos que la llamada a .get().await() devuelva un snapshot vacío
        coEvery { mockCollection.get() } returns mockk {
            every { isSuccessful } returns true
            every { result } returns mockSnapshot
            every { exception } returns null
        }
        every { mockSnapshot.documents } returns emptyList()

        // 3. Interceptamos la creación de Retrofit
        mockkStatic(RetrofitClient::class)
        every { RetrofitClient.instance } returns mockApiService

        // 4. Ahora sí, instanciamos el repositorio
        repository = BooksRepository()
    }

    @After
    fun tearDown() {
        // Limpiamos estática después de cada test
        unmockkAll()
    }

    @Test
    fun `searchHybrid devuelve lista vacia cuando las APIs fallan y firebase no tiene datos`() = runTest {
        // GIVEN: Simulamos que al intentar buscar en Google Books, falla la conexión
        val query = "Android"
        val networkError = RuntimeException("Timeout error simulado")

        // Le decimos al mock de la API que lance un error al llamar a searchBooksGoogle
        coEvery {
            mockApiService.searchBooksGoogle(any(), any(), any(), any(), any(), any())
        } throws networkError

        // Hacemos que OpenLibrary también falle
        coEvery {
            mockApiService.searchBooksOpenLibrary(any(), any())
        } throws networkError

        // WHEN: Ejecutamos el méto-do híbrido
        val result = repository.searchHybrid(query)

        // THEN: Comprobamos que el repositorio manejó el error y devolvió success (con 0 libros)
        assertTrue("El resultado debería ser un Success silencioso", result.isSuccess)

        val list = result.getOrNull()
        assertEquals(0, list?.size) // La lista debe estar vacía porque Firebase tampoco tenía datos
    }
}