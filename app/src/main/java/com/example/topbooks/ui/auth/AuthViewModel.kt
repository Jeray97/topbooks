package com.example.topbooks.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.AuthRepository
import com.example.topbooks.data.repository.AuthRepositoryImpl
import com.example.topbooks.utils.Resource
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    //Instanciamos el repositorio
    private val repository: AuthRepository = AuthRepositoryImpl()

    // _authState es mutable (nosotros lo cambiamos internamente)
    // authState es inmutable (la vista solo lo "observa")
    private val _authState = MutableStateFlow<Resource<Boolean>>(Resource.Idle)
    val authState: StateFlow<Resource<Boolean>> = _authState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = repository.currentUser
    /**
     * Intenta iniciar sesión con email y contraseña
     */
    fun login(email: String, pass: String) {
        //Lanzamos una corrutina para no bloquear la pantalla
        viewModelScope.launch {
            _authState.value = Resource.Loading //Avisamos a UI que muestre cargando

            val result = repository.login(email, pass)

            //Actualizamos el estado segun el repositorio
            if(result.isSuccess) {
                _authState.value = Resource.Success(true)
            } else {
                _authState.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error desconocido"))
            }
        }
    }

    /**
     * Registra un nuevo usuario
     */
    fun register(name: String, email: String, pass: String){
        viewModelScope.launch {
            _authState.value = Resource.Loading

            val result = repository.register(name, email, pass)

            if(result.isSuccess) {
                _authState.value = Resource.Success(true)
            } else {
                _authState.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error al registrar"))
            }
        }
    }

    // Método para resetear el estado (útil si navegamos fuera y volvemos)
    fun clearState() {
        _authState.value = Resource.Idle
    }

    /**
     * Cierra la sesión en Firebase y resetea el estado local.
     * Es vital llamar a esto desde el botón de Logout en la Home.
     */
    fun signOut() {
        repository.logout()
        _authState.value = Resource.Idle
    }
}