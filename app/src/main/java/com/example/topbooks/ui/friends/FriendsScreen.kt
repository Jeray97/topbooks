package com.example.topbooks.ui.friends

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.R
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper

// Modelo para interacciones
data class Interaction(
    val userPhoto: String = "",
    val userName: String = "",
    val actionText: String = "",
    val bookTitle: String = ""
)

@Composable
fun FriendsScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToActivity: () -> Unit,
    viewModel: FriendsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Comentado para evitar crasheos, descomenta cuando lo añadas al ViewModel
    /* LaunchedEffect(Unit) {
        viewModel.refreshRecentActivity()
    } */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackGroundGeneral)
            .padding(16.dp)
    ) {
        Text(
            text = "Encuentra amigos",
            fontFamily = CenturyGotic,
            fontSize = 28.sp,
            color = ColorArcMediumBrown,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) }, // Ajustado al VM
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Buscar amigos...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Borrar", tint = Color.Gray)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = ColorArcMediumBrown
            )
        )

        if (uiState.searchQuery.isNotEmpty()) {
            SearchResultsList(
                results = uiState.searchResults,
                isSearching = uiState.isSearching,
                onFriendAction = { user -> viewModel.toggleFriend(user) },
                onNavigateToProfile = onNavigateToProfile
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Mis amigos
                val dummyMyFriends = emptyList<SocialUser>() // Ajuste temporal para que rederice
                SocialSection(
                    title = "Mis amigos",
                    isEmpty = dummyMyFriends.isEmpty(),
                    emptyMessage = "Aún no tienes amigos añadidos"
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(dummyMyFriends) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(75.dp)
                                    .clickable { onNavigateToProfile(user.uid) }
                            ) {
                                UserAvatarItem(user.photoUrl)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = user.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Con tus mismos gustos (Usamos suggestedUsers que sí existe en tu VM)
                SocialSection(
                    title = "Con tus mismos gustos",
                    isEmpty = uiState.suggestedUsers.isEmpty(),
                    emptyMessage = "No hay sugerencias por ahora"
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(uiState.suggestedUsers) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(75.dp)
                                    .clickable { onNavigateToProfile(user.uid) }
                            ) {
                                UserAvatarItem(user.photoUrl)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = user.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interacciones recientes
                val dummyInteractions = emptyList<Interaction>() // Ajuste temporal
                SocialSection(
                    title = "Interacciones recientes",
                    isEmpty = dummyInteractions.isEmpty(),
                    emptyMessage = "Añade amigos para ver su actividad"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dummyInteractions.forEach { interaction ->
                            InteractionItem(
                                interaction = interaction,
                                onClick = onNavigateToActivity
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun SearchResultsList(results: List<SocialUser>, isSearching: Boolean, onFriendAction: (SocialUser) -> Unit, onNavigateToProfile: (String) -> Unit) {
    if (isSearching) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            CircularProgressIndicator(color = ColorArcMediumBrown, modifier = Modifier.padding(top = 20.dp))
        }
    } else if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text("No se encontraron usuarios", color = Color.Gray, modifier = Modifier.padding(top = 20.dp))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { user ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToProfile(user.uid) }.background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatarItem(user.photoUrl)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = user.displayName, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onFriendAction(user) }) {
                        Icon(imageVector = if (user.isFriend) Icons.Default.Check else Icons.Default.Add, contentDescription = null, tint = if (user.isFriend) Color(0xFF4CAF50) else ColorArcMediumBrown, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SocialSection(title: String, isEmpty: Boolean, emptyMessage: String = "Buscando usuarios...", content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = ColorArcMediumBrown, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = Color.White, fontSize = 20.sp, fontFamily = CenturyGotic, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            if (isEmpty) {
                Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                    Text(emptyMessage, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            } else { content() }
        }
    }
}

@Composable
fun InteractionItem(interaction: Interaction, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            UserAvatarItem(photoUrl = interaction.userPhoto, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            val text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = ColorArcDarkBrown)) { append(interaction.userName) }
                append(" ${interaction.actionText} ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFB9836B))) { append(interaction.bookTitle) }
            }
            Text(text = text, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 16.sp)
        }
    }
}

@Composable
fun UserAvatarItem(photoUrl: String, size: androidx.compose.ui.unit.Dp = 70.dp) {
    val imageModifier = Modifier.size(size).clip(CircleShape).background(Color.White).padding(2.dp).clip(CircleShape)
    if (photoUrl.startsWith("http")) {
        AsyncImage(model = photoUrl, contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Crop, error = painterResource(id = R.drawable.capibara_1))
    } else {
        val resourceId = if (photoUrl.isEmpty()) R.drawable.capibara_1 else AvatarHelper.getDrawableId(photoUrl)
        Image(painter = painterResource(id = resourceId), contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Crop)
    }
}