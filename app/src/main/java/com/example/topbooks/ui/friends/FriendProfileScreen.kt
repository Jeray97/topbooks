package com.example.topbooks.ui.friends

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.data.model.User
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper
import com.example.topbooks.utils.Resource

@Composable
fun FriendProfileScreen(
    userId: String,
    onBackClick: () -> Unit,
    viewModel: FriendProfileViewModel = viewModel()
) {
    val state by viewModel.friendState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadFriendProfile(userId)
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val resource = state) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ColorArcMediumBrown
                    )
                }
                is Resource.Error -> {
                    Text(
                        text = "No se pudo cargar el perfil",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                }
                is Resource.Success -> {
                    FriendProfileContent(user = resource.data)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun FriendProfileContent(user: User) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- AVATAR ---
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, ColorArcMediumBrown, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (user.photoURL.startsWith("http")) {
                AsyncImage(
                    model = user.photoURL,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = AvatarHelper.getDrawableId(user.photoURL)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- NOMBRE ---
        Text(
            text = user.displayName,
            fontFamily = GuardianCity,
            fontSize = 28.sp,
            color = ColorTituloTopBooks,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- ESTADÍSTICAS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatBox("Amigos", user.friendsCount.toString())
            StatBox("Reseñas", user.reviewsCount.toString())
            StatBox("Leídos", user.booksCompleted.toString())
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- TARJETA DE GUSTOS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Géneros Favoritos",
                    fontWeight = FontWeight.Bold,
                    color = ColorArcDarkBrown,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (user.favoriteGenres.isEmpty()) {
                    Text("Aún no ha seleccionado géneros.", fontSize = 14.sp, color = Color.Gray)
                } else {
                    Text(
                        text = user.favoriteGenres.joinToString(", "),
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorArcDarkBrown
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}