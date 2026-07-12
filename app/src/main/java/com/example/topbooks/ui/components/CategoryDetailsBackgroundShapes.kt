package com.example.topbooks.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.topbooks.ui.theme.*

/**
 * Componente contenedor (Wrapper) que dibuja un fondo con forma curva.
 * * TÉCNICA VISUAL: Utiliza un [Canvas] para dibujar un óvalo gigante que se sale de los
 * bordes de la pantalla, de modo que el usuario solo ve la parte superior del óvalo,
 * creando la ilusión de una "ola" o colina de fondo.
 *
 * @param content El contenido de la pantalla (UI) que se dibujará por encima de este fondo.
 */
@Composable
fun CategoryDetailContentBackgroundShape(
    content: @Composable () -> Unit
) {
    // Box principal que ocupa toda la pantalla con un color base oscuro
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackgorundComponente())
    ) {
        // Lienzo para dibujar la forma curva por detrás del contenido
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            drawOval(
                color = Color(0xFFD9AD9A),
                // Configuramos el tamaño del óvalo para que sea el doble de ancho que la pantalla
                size = Size(
                    width = canvasWidth * 2f,
                    // Aumentamos un poco la altura del óvalo para que la curva sea bonita
                    height = canvasHeight * 1.2f
                ),
                // Posicionamos el óvalo para que empiece fuera de la pantalla por la izquierda,
                // logrando que la "cumbre" de la curva quede centrada en la pantalla.
                topLeft = Offset(
                    x = -(canvasWidth * 0.5f),
                    // Desplazamos la curva hacia arriba (Antes 0.5f (mitad). Ahora 0.2f (más arriba)).
                    y = canvasHeight * 0.2f
                )
            )
        }

        // Renderizamos el contenido real de la pantalla por encima del Canvas
        content()
    }
}

// --- PREVIEW PARA VER EL DISEÑO ---
/**
 * Vista previa para Android Studio que permite visualizar exclusivamente el fondo curvo,
 * sin necesidad de cargar toda la pantalla de detalles de categoría.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SimpleOvalPreview() {
    CategoryDetailContentBackgroundShape {
        // Nada, solo para ver el fondo
    }
}