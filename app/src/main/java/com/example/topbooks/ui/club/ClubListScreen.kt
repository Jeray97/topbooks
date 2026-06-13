package com.example.topbooks.ui.club

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.data.model.Club
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorBackGroundGeneral
import com.example.topbooks.ui.theme.ColorTextPrimary
import com.example.topbooks.ui.theme.GuardianCity

private val SUB_TEXT = Color(0xFF8D5B4C)
private val CARD_BG = Color.White
private val CARD_BORDER = Color(0xFFECDDD2)
private val TAB_INACTIVE_BG = Color.White.copy(alpha = 0.6f)
private val TAB_ACTIVE_BG = Color(0xFF8D5B4C)

@Composable
fun ClubListScreen(
    onClubClick: (String) -> Unit,
    onCreateClubClick: () -> Unit,
    viewModel: ClubListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClubClick,
                containerColor = ColorArcDarkBrown,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Crear club", fontFamily = CenturyGotic) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)) {
                    Text(
                        text = "Clubes de lectura",
                        fontFamily = GuardianCity,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = ColorArcDarkBrown
                    )
                    Text(
                        text = "Lee, comparte y discute con otros lectores",
                        fontFamily = CenturyGotic,
                        fontSize = 12.sp,
                        color = SUB_TEXT,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            item {
                ClubTabsRow(
                    activeTab = state.activeTab,
                    onTabClick = { viewModel.selectTab(it) }
                )
            }

            if (state.errorMessage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${state.errorMessage}",
                            fontFamily = CenturyGotic,
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ColorArcMediumBrown)
                    }
                }
            } else {
                val clubs = when (state.activeTab) {
                    ClubListTab.MY_CLUBS -> state.myClubs
                    ClubListTab.EXPLORE -> state.publicClubs
                }

                if (clubs.isEmpty()) {
                    item {
                        val message = if (state.activeTab == ClubListTab.MY_CLUBS)
                            "Aún no te has unido a ningún club. ¡Explora y encuentra tu tribu!"
                        else
                            "Aún no hay clubes públicos disponibles."

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = message,
                                fontFamily = CenturyGotic,
                                fontSize = 13.sp,
                                color = SUB_TEXT,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                } else {
                    items(clubs, key = { it.id }) { club ->
                        ClubCard(
                            club = club,
                            onClick = { onClubClick(club.id) },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubTabsRow(activeTab: ClubListTab, onTabClick: (ClubListTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ClubTabPill(
            label = "Mis clubes",
            icon = Icons.Default.Groups,
            isActive = activeTab == ClubListTab.MY_CLUBS,
            modifier = Modifier.weight(1f),
            onClick = { onTabClick(ClubListTab.MY_CLUBS) }
        )
        ClubTabPill(
            label = "Explorar",
            icon = Icons.Default.Explore,
            isActive = activeTab == ClubListTab.EXPLORE,
            modifier = Modifier.weight(1f),
            onClick = { onTabClick(ClubListTab.EXPLORE) }
        )
    }
}

@Composable
private fun ClubTabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isActive) TAB_ACTIVE_BG else TAB_INACTIVE_BG,
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        if (isActive) ColorBackGroundGeneral else SUB_TEXT,
        label = "tabText"
    )

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
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

@Composable
private fun ClubCard(club: Club, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CARD_BG)
            .border(1.dp, CARD_BORDER, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (club.currentBookImageUrl.isNotBlank()) {
                AsyncImage(
                    model = club.currentBookImageUrl,
                    contentDescription = club.currentBookTitle,
                    modifier = Modifier
                        .size(width = 48.dp, height = 72.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 72.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ColorArcMediumBrown.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = ColorArcMediumBrown,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = club.name,
                    fontFamily = GuardianCity,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ColorTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (club.currentBookTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📖 ${club.currentBookTitle}",
                        fontFamily = CenturyGotic,
                        fontSize = 12.sp,
                        color = SUB_TEXT,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = SUB_TEXT,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${club.memberCount} miembros",
                        fontFamily = CenturyGotic,
                        fontSize = 11.sp,
                        color = SUB_TEXT
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = SUB_TEXT,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatFrequency(club.frequency),
                        fontFamily = CenturyGotic,
                        fontSize = 11.sp,
                        color = SUB_TEXT
                    )
                }
            }
        }

        if (club.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = club.description,
                fontFamily = CenturyGotic,
                fontSize = 12.sp,
                color = ColorTextPrimary.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
        }

        if (club.genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                club.genres.take(3).forEach { genre ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF6E6DD))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = genre,
                            fontFamily = CenturyGotic,
                            fontSize = 10.sp,
                            color = SUB_TEXT
                        )
                    }
                }
            }
        }
    }
}

private fun formatFrequency(frequency: String): String {
    return when (frequency) {
        "WEEKLY" -> "Semanal"
        "BIWEEKLY" -> "Quincenal"
        "MONTHLY" -> "Mensual"
        else -> frequency
    }
}
