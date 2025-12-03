package com.example.topbooks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.topbooks.R

@Composable
fun SearchBarCustom() {
    var text by remember { mutableStateOf("") }

    // Color aproximado del fondo beige de tu imagen
    val BackgroundBeige = Color(0xFFF9EAE1)
    val IconGray = Color(0xFF9E9E9E) // Un gris suave para los iconos

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = {
                Text(text = "Search...", color = IconGray)
            },
            // El modificador solo controla tamaño y peso
            modifier = Modifier
                .weight(1f)
                .height(50.dp), // Un poco más compacto para verse elegante

            shape = RoundedCornerShape(12.dp),

            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = Color.Black
            ),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = IconGray,
                    modifier = Modifier.size(24.dp)
                )
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Botón de código de barras
        Box(
            modifier = Modifier
                .size(50.dp) // Misma altura que el TextField
                .background(Color.White, RoundedCornerShape(12.dp)), // Misma forma
            contentAlignment = Alignment.Center
        ) {
            Icon(
                // Asegúrate de tener este recurso o usa un icono por defecto temporalmente
                painter = painterResource(id = R.drawable.icon_codigodebarras),
                contentDescription = stringResource(id = R.string.desc_scan_icon),
                modifier = Modifier.size(24.dp),
                tint = IconGray
            )
        }
    }
}