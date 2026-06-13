package com.example.topbooks.ui.club

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.data.model.Discussion
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorBackGroundGeneral
import com.example.topbooks.ui.theme.ColorTextPrimary
import com.example.topbooks.ui.theme.GuardianCity
import com.example.topbooks.utils.AvatarHelper
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

private val SUB_TEXT = Color(0xFF8D5B4C)
private val CARD_BG = Color.White
private val CARD_BORDER = Color(0xFFECDDD2)

@Composable
fun ClubDetailScreen(
    clubId: String,
    onBackClick: () -> Unit,
    onDiscussionClick: (String, String) -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: ClubDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDiscussionDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(clubId) {
        viewModel.loadClub(clubId)
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorArcMediumBrown)
                }
            }
            state.club == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Club no encontrado",
                        fontFamily = CenturyGotic,
                        fontSize = 14.sp,
                        color = SUB_TEXT
                    )
                }
            }
            else -> {
                val club = state.club!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    item { ClubHeader(club = club, onBookClick = { onBookClick(club.currentBookId) }) }

                    item {
                        DetailTabsRow(
                            selectedTab = selectedTab,
                            onTabClick = { selectedTab = it }
                        )
                    }

                    when (selectedTab) {
                        0 -> {
                            item { InfoSection(club = club) }
                        }
                        1 -> {
                            item {
                                DiscussionsHeader(
                                    canCreate = state.isMember,
                                    onCreateClick = { showCreateDiscussionDialog = true }
                                )
                            }
                            if (state.discussions.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Aún no hay discusiones. ¡Sé el primero en abrir una!",
                                            fontFamily = CenturyGotic,
                                            fontSize = 13.sp,
                                            color = SUB_TEXT
                                        )
                                    }
                                }
                            } else {
                                items(state.discussions, key = { it.id }) { discussion ->
                                    DiscussionCard(
                                        discussion = discussion,
                                        onClick = { onDiscussionClick(clubId, discussion.id) },
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        2 -> {
                            item { MembersSection(club = club) }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        MembershipButton(
                            isMember = state.isMember,
                            isCreator = state.isCreator,
                            isJoining = state.isJoining,
                            onClick = { viewModel.toggleMembership() },
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showCreateDiscussionDialog) {
        CreateDiscussionDialog(
            onDismiss = { showCreateDiscussionDialog = false },
            onConfirm = { title, chapter, isSpoiler ->
                viewModel.createDiscussion(title, chapter, isSpoiler) {
                    showCreateDiscussionDialog = false
                }
            }
        )
    }
}

@Composable
private fun ClubHeader(club: com.example.topbooks.data.model.Club, onBookClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = club.name,
            fontFamily = GuardianCity,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = ColorArcDarkBrown
        )

        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = SUB_TEXT,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${club.memberCount} miembros",
                fontFamily = CenturyGotic,
                fontSize = 12.sp,
                color = SUB_TEXT
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "· Creado por ${club.creatorName}",
                fontFamily = CenturyGotic,
                fontSize = 12.sp,
                color = SUB_TEXT
            )
        }

        if (club.currentBookTitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Libro actual",
                fontFamily = CenturyGotic,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = SUB_TEXT
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF6E6DD))
                    .clickable { onBookClick() }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (club.currentBookImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = club.currentBookImageUrl,
                        contentDescription = club.currentBookTitle,
                        modifier = Modifier
                            .size(width = 44.dp, height = 66.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 66.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ColorArcMediumBrown.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Book, null, tint = ColorArcMediumBrown, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = club.currentBookTitle,
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ColorTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = club.currentBookAuthor,
                        fontFamily = CenturyGotic,
                        fontSize = 12.sp,
                        color = SUB_TEXT,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTabsRow(selectedTab: Int, onTabClick: (Int) -> Unit) {
    val tabs = listOf(
        Triple("Info", Icons.Default.Info, 0),
        Triple("Discusiones", Icons.Default.Forum, 1),
        Triple("Miembros", Icons.Default.People, 2)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { (label, icon, index) ->
            val isActive = selectedTab == index
            val bgColor by animateColorAsState(
                if (isActive) Color(0xFF8D5B4C) else Color.White.copy(alpha = 0.6f),
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                if (isActive) ColorBackGroundGeneral else SUB_TEXT,
                label = "tabText"
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(bgColor)
                    .clickable { onTabClick(index) }
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    fontFamily = CenturyGotic,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InfoSection(club: com.example.topbooks.data.model.Club) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (club.description.isNotBlank()) {
            Text(
                text = "Descripción",
                fontFamily = CenturyGotic,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = ColorArcDarkBrown
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = club.description,
                fontFamily = CenturyGotic,
                fontSize = 13.sp,
                color = ColorTextPrimary,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "Frecuencia",
            fontFamily = CenturyGotic,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = ColorArcDarkBrown
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when (club.frequency) {
                "WEEKLY" -> "Semanal"
                "BIWEEKLY" -> "Quincenal"
                "MONTHLY" -> "Mensual"
                else -> club.frequency
            },
            fontFamily = CenturyGotic,
            fontSize = 13.sp,
            color = ColorTextPrimary
        )

        if (club.genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Géneros",
                fontFamily = CenturyGotic,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = ColorArcDarkBrown
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                club.genres.forEach { genre ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF6E6DD))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = genre,
                            fontFamily = CenturyGotic,
                            fontSize = 11.sp,
                            color = SUB_TEXT
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscussionsHeader(canCreate: Boolean, onCreateClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Discusiones",
            fontFamily = CenturyGotic,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = ColorArcDarkBrown,
            modifier = Modifier.weight(1f)
        )
        if (canCreate) {
            TextButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = ColorArcMediumBrown)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nueva", fontFamily = CenturyGotic, fontSize = 12.sp, color = ColorArcMediumBrown)
            }
        }
    }
}

@Composable
private fun DiscussionCard(discussion: Discussion, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CARD_BG)
            .border(1.dp, CARD_BORDER, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (discussion.isSpoiler) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Spoiler",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = discussion.title,
                fontFamily = CenturyGotic,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = ColorTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        if (discussion.chapter.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Capítulo: ${discussion.chapter}",
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = SUB_TEXT
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "por ${discussion.creatorName}",
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = SUB_TEXT,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = SUB_TEXT,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${discussion.messageCount}",
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = SUB_TEXT
            )
        }
    }
}

@Composable
private fun MembersSection(club: com.example.topbooks.data.model.Club) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "${club.memberCount} miembros",
            fontFamily = CenturyGotic,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = ColorArcDarkBrown
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Los perfiles de miembros se mostrarán aquí cuando se carguen.",
            fontFamily = CenturyGotic,
            fontSize = 12.sp,
            color = SUB_TEXT
        )
    }
}

@Composable
private fun MembershipButton(
    isMember: Boolean,
    isCreator: Boolean,
    isJoining: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isCreator) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE3F0D8))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4A8520), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Eres el creador de este club",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF4A8520)
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isJoining,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMember) Color(0xFFE57373) else ColorArcDarkBrown,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isJoining) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (isMember) Icons.Default.Logout else Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isMember) "Abandonar club" else "Unirse al club",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun CreateDiscussionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }
    var isSpoiler by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva discusión",
                fontFamily = GuardianCity,
                fontWeight = FontWeight.Bold,
                color = ColorArcDarkBrown
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título", fontFamily = CenturyGotic) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorArcMediumBrown)
                )
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("Capítulo (opcional)", fontFamily = CenturyGotic) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorArcMediumBrown)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = isSpoiler,
                        onCheckedChange = { isSpoiler = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contiene spoilers",
                        fontFamily = CenturyGotic,
                        fontSize = 13.sp,
                        color = ColorTextPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, chapter, isSpoiler) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Crear", fontFamily = CenturyGotic)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontFamily = CenturyGotic, color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}
