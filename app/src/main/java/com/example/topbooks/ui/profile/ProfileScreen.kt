package com.example.topbooks.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.R
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*

// 1. PANTALLA PRINCIPAL (Conectada a datos)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onLogout: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

    // Llamamos al contenido visual pasándole los datos reales
    ProfileContent(
        userProfile = userProfile,
        onLogout = {
            viewModel.signOut()
            onLogout()
        },
        onBookClick = onNavigateToDetail
    )
}

// 2. CONTENIDO VISUAL
@Composable
fun ProfileContent(
    userProfile: UserProfile,
    onLogout: () -> Unit,
    onBookClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            Box(contentAlignment = Alignment.CenterEnd) {
                TopBar(onBackClick = {})

                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = ColorArcDarkBrown,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = ColorArcDarkBrown
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tu perfil",
                fontFamily = CenturyGotic,
                fontSize = 24.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- INFO USUARIO ---
            Row(verticalAlignment = Alignment.Top) {
                Image(
                    painter = painterResource(id = R.drawable.capibara_1),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = userProfile.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = CenturyGotic
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Biografía:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = userProfile.bio,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ESTADÍSTICAS ---
            StatBar(title = "Amigos", value = "${userProfile.friendsCount}")
            Spacer(modifier = Modifier.height(12.dp))
            StatBar(title = "Libros completados", value = "${userProfile.booksCompleted}")

            Spacer(modifier = Modifier.height(24.dp))

            // --- GRID DE TARJETAS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardCard(
                        title = "Tus favoritos",
                        iconContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (userProfile.favoriteCovers.isEmpty()) {
                                    Image(
                                        painter = painterResource(id = R.drawable.cat_resenas_icon),
                                        contentDescription = "Sin favoritos",
                                        modifier = Modifier
                                            .size(30.dp, 45.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        alpha = 0.5f,
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    userProfile.favoriteCovers.forEachIndexed { index, url ->
                                        val bookId = userProfile.favoriteIds.getOrNull(index)

                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(url)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Portada",
                                            modifier = Modifier
                                                .size(30.dp, 45.dp) // Tamaño proporcional a libro
                                                .clip(RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp)
                                                .clickable {
                                                    if(bookId != null) onBookClick(bookId)
                                                },
                                            contentScale = ContentScale.Crop,
                                            // Imagen de error por si la URL falla
                                            error = painterResource(R.drawable.icon_codigodebarras)
                                        )
                                    }
                                }
                            }
                        }
                    )
                    DashboardCard(
                        title = "Tus marcadores",
                        iconContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Favorite, null, tint = Color.Gray)
                                Icon(Icons.Default.Favorite, null, tint = Color(0xFFFFD54F))
                                Icon(Icons.Default.Favorite, null, tint = Color.Gray)
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardCard(
                        title = "Tus reseñas",
                        iconContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F))
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F))
                                Icon(Icons.Default.Star, null, tint = Color.Gray)
                            }
                        }
                    )
                    DashboardCard(
                        title = "Tus comentarios",
                        iconContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Favorite, null, tint = Color(0xFFE57373))
                                Icon(Icons.Default.AccountCircle, null, tint = Color(0xFF9575CD))
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- COMPONENTES VISUALES ---

@Composable
fun StatBar(title: String, value: String) {
    Surface(
        color = Color(0xFFBCAAA4),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(45.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, color = ColorArcDarkBrown, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = ColorArcDarkBrown, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    iconContent: @Composable () -> Unit
) {
    Surface(
        color = ColorHeaderBeige,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(95.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = ColorArcDarkBrown,
                fontFamily = CenturyGotic,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                iconContent()
            }
        }
    }
}


// --- PREVIEW ---
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    TopBooksTheme {
        ProfileContent(
            userProfile = UserProfile(
                displayName = "Jeray Reyes",
                bio = "Me apasiona leer, los libros de fantasía y misterio son mis favoritos!",
                friendsCount = 42,
                booksCompleted = 12
            ),
            onLogout = {},
            onBookClick = {}
        )
    }
}