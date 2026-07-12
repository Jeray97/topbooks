package com.example.topbooks.ui.club

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.data.model.Club
import com.example.topbooks.data.model.ClubFrequency
import com.example.topbooks.data.repository.ClubRepository
import com.example.topbooks.data.repository.ClubRepositoryImpl
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorBackGroundGeneral
import com.example.topbooks.ui.theme.ColorTextPrimary
import com.example.topbooks.ui.theme.GuardianCity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateClubState(
    val isCreating: Boolean = false,
    val createSuccess: Boolean = false,
    val errorMessage: String? = null
)

class CreateClubViewModel(
    private val clubRepository: ClubRepository = ClubRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateClubState())
    val uiState: StateFlow<CreateClubState> = _uiState.asStateFlow()

    fun createClub(
        name: String,
        description: String,
        frequency: ClubFrequency,
        isPublic: Boolean,
        genres: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }
            val club = Club(
                name = name,
                description = description,
                frequency = frequency.name,
                isPublic = isPublic,
                genres = genres
            )
            clubRepository.createClub(club).fold(
                onSuccess = {
                    _uiState.update { it.copy(isCreating = false, createSuccess = true) }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isCreating = false, errorMessage = error.message) }
                }
            )
        }
    }
}

@Composable
fun CreateClubScreen(
    onBackClick: () -> Unit,
    onClubCreated: () -> Unit,
    viewModel: CreateClubViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf(ClubFrequency.MONTHLY) }
    var isPublic by remember { mutableStateOf(true) }
    var selectedGenres by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        containerColor = ColorBackGroundGeneral(),
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Crear club de lectura",
                fontFamily = GuardianCity,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = ColorArcDarkBrown()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nombre del club",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ColorTextPrimary()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: Club de fantasía épica", fontFamily = CenturyGotic) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorArcMediumBrown(),
                        unfocusedBorderColor = ColorArcMediumBrown().copy(alpha = 0.3f)
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Descripción",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ColorTextPrimary()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("¿De qué trata tu club?", fontFamily = CenturyGotic) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorArcMediumBrown(),
                        unfocusedBorderColor = ColorArcMediumBrown().copy(alpha = 0.3f)
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Frecuencia de lectura",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ColorTextPrimary()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FrequencyCard(
                        label = "Semanal",
                        icon = Icons.Default.Today,
                        isSelected = selectedFrequency == ClubFrequency.WEEKLY,
                        onClick = { selectedFrequency = ClubFrequency.WEEKLY },
                        modifier = Modifier.weight(1f)
                    )
                    FrequencyCard(
                        label = "Quincenal",
                        icon = Icons.Default.DateRange,
                        isSelected = selectedFrequency == ClubFrequency.BIWEEKLY,
                        onClick = { selectedFrequency = ClubFrequency.BIWEEKLY },
                        modifier = Modifier.weight(1f)
                    )
                    FrequencyCard(
                        label = "Mensual",
                        icon = Icons.Default.CalendarMonth,
                        isSelected = selectedFrequency == ClubFrequency.MONTHLY,
                        onClick = { selectedFrequency = ClubFrequency.MONTHLY },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Club público",
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ColorTextPrimary()
                    )
                    Text(
                        text = if (isPublic) "Cualquiera puede unirse" else "Solo con invitación",
                        fontFamily = CenturyGotic,
                        fontSize = 11.sp,
                        color = ColorArcDarkBrown()
                    )
                }
                Icon(
                    imageVector = if (isPublic) Icons.Default.Public else Icons.Default.Lock,
                    contentDescription = null,
                    tint = ColorArcMediumBrown(),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ColorArcMediumBrown()
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Géneros (opcional)",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ColorTextPrimary()
                )
                val allGenres = listOf(
                    "Romance", "Fantasía", "Thriller", "Ciencia ficción",
                    "Terror", "Histórica", "Misterio", "No ficción"
                )
                val rows = allGenres.chunked(4)
                rows.forEach { rowGenres ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowGenres.forEach { genre ->
                            val isSelected = genre in selectedGenres
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ColorArcMediumBrown() else Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) ColorArcMediumBrown() else ColorArcMediumBrown().copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedGenres = if (isSelected) {
                                            selectedGenres - genre
                                        } else {
                                            selectedGenres + genre
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = genre,
                                    fontFamily = CenturyGotic,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White else ColorTextPrimary(),
                                    maxLines = 1
                                )
                            }
                        }
                        repeat(4 - rowGenres.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.createClub(
                        name = name,
                        description = description,
                        frequency = selectedFrequency,
                        isPublic = isPublic,
                        genres = selectedGenres.toList()
                    ) { }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = name.isNotBlank() && !state.isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorArcDarkBrown(),
                    contentColor = Color.White,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Crear club",
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }

            if (state.createSuccess) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    onClubCreated()
                }
            }
        }
    }
}

@Composable
private fun FrequencyCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ColorArcMediumBrown() else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) ColorArcMediumBrown() else ColorArcMediumBrown().copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else ColorArcMediumBrown(),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = if (isSelected) Color.White else ColorTextPrimary()
            )
        }
    }
}
