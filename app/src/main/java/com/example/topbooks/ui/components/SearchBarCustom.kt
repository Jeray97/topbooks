package com.example.topbooks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.R
import com.example.topbooks.ui.community.SearchFilter
import com.example.topbooks.ui.search.SearchViewModel
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorTextPrimary

/**
 * Componente reutilizable de Barra de Búsqueda con autocompletado y botón de escáner.
 */
@Composable
fun SearchBarCustom(
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    var text by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    val results by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()

    val focusManager = LocalFocusManager.current
    val iconGray = Color(0xFF9E9E9E)

    Box(modifier = Modifier.fillMaxWidth().zIndex(1f)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // CAMPO DE TEXTO (Buscador)
                TextField(
                    value = text,
                    onValueChange = {
                        text = it
                        active = true
                        viewModel.onQueryChange(it)
                    },
                    placeholder = {
                        Text(
                            text = when (searchFilter) {
                                SearchFilter.GENERAL -> stringResource(R.string.search_hint)
                                SearchFilter.TITLE -> "Buscar por título..."
                                SearchFilter.AUTHOR -> "Buscar por autor..."
                                SearchFilter.ISBN -> "Buscar por ISBN..."
                                SearchFilter.SERIES -> "Buscar por saga..."
                            },
                            color = iconGray
                        )
                    },
                    // CORRECCIÓN: La etiqueta debe ir aquí para que el test pueda escribir texto
                    modifier = Modifier
                        .weight(1f)
                        .height(55.dp)
                        .testTag("search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Black
                    ),
                    trailingIcon = {
                        if (text.isNotEmpty()) {
                            IconButton(onClick = {
                                text = ""
                                viewModel.clearResults()
                                active = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.desc_clear_icon), tint = iconGray)
                            }
                        } else {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.desc_search_icon), tint = iconGray)
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(55.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clickable { onScanClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_codigodebarras),
                        contentDescription = stringResource(id = R.string.desc_scan_icon),
                        modifier = Modifier.size(24.dp),
                        tint = iconGray
                    )
                }
            }

            // Chips de filtro de búsqueda
            if (active || text.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SearchFilterChip(
                        label = "General",
                        icon = Icons.Default.Search,
                        isSelected = searchFilter == SearchFilter.GENERAL,
                        onClick = {
                            viewModel.setSearchFilter(SearchFilter.GENERAL)
                            if (text.length >= 3) viewModel.onQueryChange(text)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SearchFilterChip(
                        label = "Título",
                        icon = Icons.Default.Title,
                        isSelected = searchFilter == SearchFilter.TITLE,
                        onClick = {
                            viewModel.setSearchFilter(SearchFilter.TITLE)
                            if (text.length >= 3) viewModel.onQueryChange(text)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SearchFilterChip(
                        label = "Autor",
                        icon = Icons.Default.Person,
                        isSelected = searchFilter == SearchFilter.AUTHOR,
                        onClick = {
                            viewModel.setSearchFilter(SearchFilter.AUTHOR)
                            if (text.length >= 3) viewModel.onQueryChange(text)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SearchFilterChip(
                        label = "ISBN",
                        icon = Icons.Default.Numbers,
                        isSelected = searchFilter == SearchFilter.ISBN,
                        onClick = {
                            viewModel.setSearchFilter(SearchFilter.ISBN)
                            if (text.length >= 3) viewModel.onQueryChange(text)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SearchFilterChip(
                        label = "Saga",
                        icon = Icons.Default.CollectionsBookmark,
                        isSelected = searchFilter == SearchFilter.SERIES,
                        onClick = {
                            viewModel.setSearchFilter(SearchFilter.SERIES)
                            if (text.length >= 3) viewModel.onQueryChange(text)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (active && (results.isNotEmpty() || isLoading)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 250.dp),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    if (isLoading) {
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            // CORRECCIÓN: Añadida etiqueta para que el test verifique el estado de carga
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("loading_spinner"),
                                color = Color(0xFFB9836B)
                            )
                        }
                    } else {
                        LazyColumn {
                            items(results) { book ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onBookClick(book.id)
                                            active = false
                                            focusManager.clearFocus()
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Card(shape = RoundedCornerShape(4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(book.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(40.dp, 60.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(book.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(book.authors.firstOrNull() ?: stringResource(R.string.book_unknown_author), fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                                    }
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chip de filtro de búsqueda reutilizable.
 */
@Composable
private fun SearchFilterChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) ColorArcMediumBrown else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) ColorArcMediumBrown else ColorArcMediumBrown.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else ColorArcMediumBrown,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontFamily = CenturyGotic,
                fontSize = 9.sp,
                color = if (isSelected) Color.White else ColorTextPrimary,
                maxLines = 1
            )
        }
    }
}