package com.example.topbooks.ui.friends

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.example.topbooks.R
import com.example.topbooks.ui.theme.*

@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Buscar amigos...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
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
                onAddFriend = { user -> viewModel.addFriend(user) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                SocialSection(
                    title = "Amigos de tus amigos",
                    isEmpty = uiState.friendsOfFriends.isEmpty(),
                    emptyMessage = "Sin amigos todavía"
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.friendsOfFriends) { user ->
                            UserAvatarItem(user.photoUrl)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SocialSection(
                    title = "Con tus mismos gustos",
                    isEmpty = uiState.sameTastes.isEmpty()
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.sameTastes) { user ->
                            UserAvatarItem(user.photoUrl)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SocialSection(
                    title = "Interacciones recientes",
                    isEmpty = uiState.recentInteractions.isEmpty()
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.recentInteractions) { interaction ->
                            UserAvatarItem(interaction.userPhoto)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SearchResultsList(
    results: List<SocialUser>,
    isSearching: Boolean,
    onAddFriend: (SocialUser) -> Unit
) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatarItem(user.photoUrl)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = user.displayName,
                        fontWeight = FontWeight.Bold,
                        color = ColorArcDarkBrown,
                        modifier = Modifier.weight(1f)
                    )

                    if (user.isFriend) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Ya es amigo",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.padding(end = 12.dp).size(28.dp)
                        )
                    } else {
                        IconButton(onClick = { onAddFriend(user) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Añadir amigo",
                                tint = ColorArcMediumBrown,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialSection(
    title: String,
    isEmpty: Boolean,
    emptyMessage: String = "Buscando usuarios...",
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ColorArcMediumBrown,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = CenturyGotic,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isEmpty) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emptyMessage, color = Color.White.copy(alpha = 0.7f))
                }
            } else {
                content()
            }
        }
    }
}

@Composable
fun UserAvatarItem(photoUrl: String) {
    val imageModifier = Modifier
        .size(70.dp)
        .clip(CircleShape)
        .background(Color.White)
        .padding(2.dp)
        .clip(CircleShape)

    if (photoUrl.isEmpty()) {
        Image(
            painter = painterResource(id = R.drawable.capibara_1),
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop
        )
    } else {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop
        )
    }
}