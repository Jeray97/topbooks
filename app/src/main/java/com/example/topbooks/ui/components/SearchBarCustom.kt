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

/**
 * Componente reutilizable de Barra de Búsqueda con autocompletado y botón de escáner.
 * * TÉCNICA VISUAL: Utiliza un menú desplegable "flotante" que muestra los resultados
 * en tiempo real sin salir de la pantalla actual.
 *
 * @param onBookClick Callback que se ejecuta al seleccionar un libro de los resultados. Devuelve el ID del libro.
 * @param onScanClick Callback que se ejecuta al pulsar el icono del código de barras.
 * @param viewModel Instancia del [SearchViewModel] que maneja la lógica de búsqueda en las APIs.
 */
@Composable
fun SearchBarCustom(
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit, // CALLBACK PARA EL ESCÁNER
    viewModel: SearchViewModel = viewModel()
) {
    // --- ESTADOS LOCALES ---
    var text by remember { mutableStateOf("") } // Texto escrito por el usuario
    var active by remember { mutableStateOf(false) } // Controla si el dropdown de resultados debe mostrarse

    // --- ESTADOS DEL VIEWMODEL ---
    val results by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Gestor de foco para poder ocultar el teclado por código
    val focusManager = LocalFocusManager.current

    // Paleta de colores local
    val iconGray = Color(0xFF9E9E9E)

    // Usamos zIndex(1f) para asegurar que el dropdown se dibuje por ENCIMA
    // de cualquier otro elemento de la pantalla (ej. Carruseles de la Home).
    Box(modifier = Modifier.fillMaxWidth().zIndex(1f)) {
        Column {
            // --- FILA SUPERIOR: INPUT + BOTÓN ESCÁNER ---
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
                        active = true // Despliega la lista de resultados al empezar a escribir
                        viewModel.onQueryChange(it) // Dispara la búsqueda en el ViewModel
                    },
                    placeholder = { Text(text = stringResource(R.string.search_hint), color = iconGray) },
                    modifier = Modifier.weight(1f).height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent, // Quita la línea inferior por defecto de Material
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Black
                    ),
                    trailingIcon = {
                        // Cambia la lupa por una 'X' si hay texto escrito
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

                // BOTÓN DE ESCÁNER DE CÓDIGOS DE BARRAS
                Box(
                    modifier = Modifier
                        .size(55.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clickable { onScanClick() }, // Ejecuta el callback
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

            // --- DROPDOWN FLOTANTE DE RESULTADOS ---
            // Solo se muestra si el buscador está activo y hay resultados (o está cargando)
            if (active && (results.isNotEmpty() || isLoading)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 250.dp), // Altura máxima para que no tape toda la pantalla
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    if (isLoading) {
                        // Indicador de carga
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(30.dp), color = Color(0xFFB9836B))
                        }
                    } else {
                        // Lista de libros encontrados
                        LazyColumn {
                            items(results) { book ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // 1. Ejecutamos la acción (navegar al libro)
                                            onBookClick(book.id)
                                            // 2. Cerramos el dropdown
                                            active = false
                                            // 3. Ocultamos el teclado del móvil
                                            focusManager.clearFocus()
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Miniatura de la portada
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

                                    // Datos del libro (Título y Autor)
                                    Column {
                                        Text(book.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(book.authors.firstOrNull() ?: stringResource(R.string.book_unknown_author), fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                                    }
                                }
                                // Separador entre elementos de la lista
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}