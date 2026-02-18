package com.example.topbooks.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.User
import com.example.topbooks.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _friendState = MutableStateFlow<Resource<User>>(Resource.Loading)
    val friendState: StateFlow<Resource<User>> = _friendState.asStateFlow()

    fun loadFriendProfile(userId: String) {
        viewModelScope.launch {
            _friendState.value = Resource.Loading
            try {
                val doc = db.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        _friendState.value = Resource.Success(user)
                    } else {
                        _friendState.value = Resource.Error(Exception("Error al convertir datos"))
                    }
                } else {
                    _friendState.value = Resource.Error(Exception("Usuario no encontrado"))
                }
            } catch (e: Exception) {
                _friendState.value = Resource.Error(e)
            }
        }
    }
}