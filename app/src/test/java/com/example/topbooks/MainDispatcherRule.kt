package com.example.topbooks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit Rule que sustituye Dispatchers.Main por un TestDispatcher controlable.
 *
 * Es necesaria en TODOS los tests de ViewModels porque viewModelScope.launch { }
 * usa Dispatchers.Main internamente, que no existe en el entorno JVM puro (sin Android).
 *
 * Uso:
 *   @get:Rule
 *   val mainDispatcherRule = MainDispatcherRule()
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}