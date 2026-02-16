package com.example.topbooks.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.topbooks.utils.AvatarHelper

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

    // Estados para los diálogos
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // DIÁLOGO AVATAR
    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("Elige tu avatar", fontFamily = CenturyGotic, color = ColorArcDarkBrown) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(AvatarHelper.avatars) { (name, resId) ->
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (userProfile.photoUrl == name) 3.dp else 1.dp,
                                    color = if (userProfile.photoUrl == name) ColorArcMediumBrown else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.updateAvatar(name)
                                    showAvatarDialog = false
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarDialog = false }) { Text("Cancelar", color = ColorArcDarkBrown) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // DIÁLOGO EDITAR PERFIL (NOMBRE Y BIO)
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userProfile.displayName,
            currentBio = userProfile.bio,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName, newBio ->
                viewModel.updateProfileData(newName, newBio)
                showEditProfileDialog = false
            }
        )
    }

    ProfileContent(
        userProfile = userProfile,
        onSettingsClick = onNavigateToSettings,
        onBookClick = onNavigateToDetail,
        onAvatarClick = { showAvatarDialog = true },
        onEditProfileClick = { showEditProfileDialog = true } // Nuevo evento
    )
}

@Composable
fun ProfileContent(
    userProfile: UserProfile,
    onSettingsClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onEditProfileClick: () -> Unit // Nuevo parámetro
) {
    Scaffold(
        topBar = {
            Box(contentAlignment = Alignment.CenterEnd) {
                TopBar(onBackClick = {})
                IconButton(onClick = onSettingsClick, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Default.Settings, "Configuración", tint = ColorArcDarkBrown, modifier = Modifier.size(28.dp))
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
            Text("Tu perfil", fontFamily = CenturyGotic, fontSize = 24.sp, color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))

            // --- INFO USUARIO ---
            Row(verticalAlignment = Alignment.Top) {
                // Avatar
                Box(modifier = Modifier.clickable { onAvatarClick() }) {
                    val photo = userProfile.photoUrl ?: "capibara_1"
                    val avatarModifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                        .clip(CircleShape)

                    if (photo.startsWith("http")) {
                        AsyncImage(model = photo, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = avatarModifier)
                    } else {
                        Image(painter = painterResource(id = AvatarHelper.getDrawableId(photo)), contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = avatarModifier)
                    }

                    Icon(Icons.Default.Edit, "Editar", tint = ColorArcMediumBrown, modifier = Modifier.align(Alignment.BottomEnd).background(Color.White, CircleShape).padding(4.dp).size(16.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Nombre + Icono Editar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onEditProfileClick() } // Clic en el nombre también abre editor
                    ) {
                        Text(
                            text = userProfile.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = CenturyGotic
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Perfil",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Biografía:", color = Color.White.copy(0.8f), fontSize = 14.sp)
                    Text(userProfile.bio, color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Estadísticas
            StatBar(title = "Amigos", value = "${userProfile.friendsCount}")
            Spacer(modifier = Modifier.height(12.dp))
            StatBar(title = "Libros completados", value = "${userProfile.booksCompleted}")

            Spacer(modifier = Modifier.height(24.dp))

            // Grid de Tarjetas (Igual que antes)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardCard("Tus favoritos") {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (userProfile.favoriteCovers.isEmpty()) {
                                Image(painterResource(R.drawable.cat_resenas_icon), null, Modifier.size(30.dp, 45.dp), alpha = 0.5f)
                            } else {
                                userProfile.favoriteCovers.forEachIndexed { i, url ->
                                    val id = userProfile.favoriteIds.getOrNull(i)
                                    AsyncImage(
                                        model = url, contentDescription = null,
                                        modifier = Modifier.size(30.dp, 45.dp).clip(RoundedCornerShape(4.dp)).clickable { if(id!=null) onBookClick(id) },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                    DashboardCard("Tus marcadores") {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Favorite, null, tint = Color.Gray)
                            Icon(Icons.Default.Favorite, null, tint = Color(0xFFFFD54F))
                            Icon(Icons.Default.Favorite, null, tint = Color.Gray)
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardCard("Tus reseñas") {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F))
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F))
                            Icon(Icons.Default.Star, null, tint = Color.Gray)
                        }
                    }
                    DashboardCard("Tus comentarios") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Favorite, null, tint = Color(0xFFE57373))
                            Icon(Icons.Default.AccountCircle, null, tint = Color(0xFF9575CD))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- NUEVO COMPONENTE: DIÁLOGO DE EDICIÓN DE TEXTO ---
@Composable
fun EditProfileDialog(
    currentName: String,
    currentBio: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil", fontFamily = CenturyGotic, color = ColorArcDarkBrown) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorArcMediumBrown,
                        focusedLabelColor = ColorArcMediumBrown
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biografía") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorArcMediumBrown,
                        focusedLabelColor = ColorArcMediumBrown
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, bio) },
                colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = ColorArcDarkBrown)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun StatBar(title: String, value: String) {
    Surface(color = Color(0xFFBCAAA4), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(45.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(title, color = ColorArcDarkBrown, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = ColorArcDarkBrown, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardCard(title: String, iconContent: @Composable () -> Unit) {
    Surface(color = ColorHeaderBeige, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(95.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = ColorArcDarkBrown, fontFamily = CenturyGotic, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) { iconContent() }
        }
    }
}