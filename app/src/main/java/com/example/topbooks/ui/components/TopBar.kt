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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.topbooks.ui.theme.ColorHeaderBeige
import com.example.topbooks.ui.theme.ColorTextPrimary
import com.example.topbooks.ui.theme.ColorTitleCategoryDetail
import com.example.topbooks.ui.theme.ColorTitleContentCategoryDetail
import com.example.topbooks.ui.theme.GuardianCity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "TopBooks",
                // 2. Usamos fuente y color de texto
                fontFamily = GuardianCity,
                color = ColorTitleCategoryDetail,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Left

            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = ColorTextPrimary // La flecha del color del texto
                )
            }
        },
        // Hacemos la barra transparente para que se vea el fondo general
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = ColorHeaderBeige
        )
    )
}