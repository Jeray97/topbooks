package com.example.topbooks.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import com.example.topbooks.ui.theme.*

@Composable
fun CategoryDetailContentBackgroundShape(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackgorundComponente)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            drawOval(
                color = ColorCategoryDetailContentBackgroundShape,
                size = Size(
                    width = canvasWidth * 2f,
                    // Aumentamos un poco la altura del óvalo para que la curva sea bonita
                    height = canvasHeight * 1.2f
                ),
                topLeft = Offset(
                    x = -(canvasWidth * 0.5f),
                    // Antes 0.5f (mitad). Ahora 0.25f (más arriba).
                    y = canvasHeight * 0.2f
                )
            )
        }

        content()
    }
}

// --- PREVIEW PARA QUE VEAS CÓMO QUEDA ---
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SimpleOvalPreview() {
    CategoryDetailContentBackgroundShape {
        // Nada, solo para ver el fondo
    }
}