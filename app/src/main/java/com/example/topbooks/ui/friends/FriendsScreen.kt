package com.example.topbooks.ui.friends

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    onNavigateToClubs: () -> Unit = {},
    viewModel: FriendsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.friends_title),
            fontFamily = GuardianCity,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text(stringResource(R.string.friends_search_hint), color = MaterialTheme.colorScheme.outlineVariant) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.friends_action_clear), tint = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { onNavigateToClubs() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clubes de lectura",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Lee y discute con otros lectores",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                val myFriends = uiState.myFriends
                SocialSection(
                    title = stringResource(R.string.friends_section_my_friends),
                    isEmpty = myFriends.isEmpty(),
                    emptyMessage = stringResource(R.string.friends_empty_my_friends)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(myFriends) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable { onNavigateToProfile(user.uid) }
                            ) {
                                UserAvatarItem(user.photoUrl)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = user.displayName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
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

                SocialSection(
                    title = stringResource(R.string.friends_section_suggestions),
                    isEmpty = uiState.suggestedUsers.isEmpty(),
                    emptyMessage = stringResource(R.string.friends_empty_suggestions)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(uiState.suggestedUsers) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable { onNavigateToProfile(user.uid) }
                            ) {
                                UserAvatarItem(user.photoUrl)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = user.displayName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
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

                val recentInteractions = uiState.recentInteractions
                SocialSection(
                    title = stringResource(R.string.friends_section_interactions),
                    isEmpty = recentInteractions.isEmpty(),
                    emptyMessage = stringResource(R.string.friends_empty_interactions)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentInteractions.forEach { interaction ->
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
fun SearchResultsList(
    results: List<SocialUser>,
    isSearching: Boolean,
    onFriendAction: (SocialUser) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    if (isSearching) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            CircularProgressIndicator(color = LoginColors.Primary, modifier = Modifier.padding(top = 20.dp))
        }
    } else if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text(stringResource(R.string.friends_search_empty), color = LoginColors.Outline, modifier = Modifier.padding(top = 20.dp))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToProfile(user.uid) }
                        .background(LoginColors.Surface, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatarItem(user.photoUrl, size = 48.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = user.displayName, fontWeight = FontWeight.SemiBold, color = LoginColors.Primary, modifier = Modifier.weight(1f))

                    IconButton(onClick = { onFriendAction(user) }) {
                        Icon(
                            imageVector = if (user.isFriend) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (user.isFriend) LoginColors.SecondaryContainer else LoginColors.Primary,
                            modifier = Modifier.size(28.dp)
                        )
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
    emptyMessage: String = stringResource(id = R.string.friends_section_loading_default),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LoginColors.SurfaceDim,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = title,
                color = LoginColors.Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        color = LoginColors.OnSurfaceVariant,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                content()
            }
        }
    }
}

@Composable
fun InteractionItem(interaction: Interaction, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LoginColors.Surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            UserAvatarItem(photoUrl = interaction.userPhoto, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))

            val text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = LoginColors.Primary)) { append(interaction.userName) }
                append(stringResource(R.string.friends_interaction_format, interaction.actionText))
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = LoginColors.SurfaceTint)) { append(interaction.bookTitle) }
            }
            Text(text = text, fontSize = 13.sp, color = LoginColors.OnSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
fun UserAvatarItem(photoUrl: String, size: androidx.compose.ui.unit.Dp = 72.dp) {
    val imageModifier = Modifier
        .size(size)
        .clip(CircleShape)
        .border(2.dp, LoginColors.Surface, CircleShape)
        .clip(CircleShape)

    if (photoUrl.startsWith("http")) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.capibara_1)
        )
    } else {
        val resourceId = if (photoUrl.isEmpty()) R.drawable.capibara_1 else AvatarHelper.getDrawableId(photoUrl)
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop
        )
    }
}
