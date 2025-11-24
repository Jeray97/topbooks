package com.example.topbooks.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.topbooks.ui.auth.AuthViewModel
import com.example.topbooks.ui.auth.LoginScreen
import com.example.topbooks.ui.auth.RegisterScreen
import com.example.topbooks.ui.home.HomeScreen
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()

    // Verificamos si ya hay usuario logueado al arrancar
    // Si authViewModel.currentUser no es nulo, el destino inicial debería ser "home"
    // Pero por simplicidad, dejamos que el NavHost decida o redirigimos.
    val startDestination = if (authViewModel.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        // RUTA: LOGIN
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Navegar a Home y borrar el historial para no volver al Login con "Atrás"
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // RUTA: REGISTRO
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack() // Volver atrás
                }
            )
        }

        // RUTA: HOME
        composable("home") {
            HomeScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}