package com.example.topbooks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.example.topbooks.ui.search.SearchViewModel

//TODO USAR TEXTO DE VALUES
@Composable
fun SearchBarCustom(
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit, //CALLBACK
    viewModel: SearchViewModel = viewModel()
) {
    var text by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    val results by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
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
                TextField(
                    value = text,
                    onValueChange = {
                        text = it
                        active = true
                        viewModel.onQueryChange(it)
                    },
                    placeholder = { Text(text = "Buscar libro...", color = iconGray) },
                    modifier = Modifier.weight(1f).height(55.dp),
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
                                Icon(Icons.Default.Close, contentDescription = "Borrar", tint = iconGray)
                            }
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = iconGray)
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // BOTÓN DE SCAN
                Box(
                    modifier = Modifier
                        .size(55.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clickable { onScanClick() }, // Acción
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

            if (active && (results.isNotEmpty() || isLoading)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 250.dp),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    if (isLoading) {
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(30.dp), color = Color(0xFFB9836B))
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
                                        Text(book.authors.firstOrNull() ?: "Desconocido", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
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