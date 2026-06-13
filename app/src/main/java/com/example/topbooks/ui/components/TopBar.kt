package com.example.topbooks.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.topbooks.R
import com.example.topbooks.ui.theme.ColorHeaderBeige
import com.example.topbooks.ui.theme.ColorTextPrimary
import com.example.topbooks.ui.theme.ColorTitleCategoryDetail
import com.example.topbooks.ui.theme.GuardianCity

/**
 * Componente visual estandarizado para la barra superior de navegación.
 * * Utiliza [CenterAlignedTopAppBar] de Material 3 para asegurar que el título
 * quede siempre perfectamente centrado.
 *
 * @param onBackClick Acción que se ejecuta al pulsar el botón (flecha) de volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onBackClick: () -> Unit,
    title: String? = null
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title ?: stringResource(R.string.app_name),
                fontFamily = GuardianCity,
                color = ColorTitleCategoryDetail,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Left
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                // TÉCNICA DE ACCESIBILIDAD: 'AutoMirrored' hace que la flecha apunte
                // a la derecha si el sistema del usuario usa un idioma RTL (Right-to-Left, ej: Árabe)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.desc_back_icon),
                    tint = ColorTextPrimary
                )
            }
        },
        // Configuramos el color de fondo para que coincida con el tema general de las pantallas
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = ColorHeaderBeige
        )
    )
}