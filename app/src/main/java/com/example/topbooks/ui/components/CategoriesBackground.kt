package com.example.topbooks.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcLightBeige
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorArcWhite
import com.example.topbooks.ui.theme.ColorHeaderBeige
import kotlin.math.ceil



@Composable
fun CategoriesBackground(
    modifier: Modifier = Modifier,
    categoryCount: Int,
    columnCount: Int = 4,
    rowHeight: Dp,
    startOffset: Dp
) {
    val density = LocalDensity.current
    val colorCycle = listOf(ColorArcDarkBrown, ColorArcMediumBrown, ColorArcLightBeige, ColorArcWhite)

    // Calculamos filas necesarias
    val rows = ceil(categoryCount / columnCount.toFloat()).toInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorHeaderBeige)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Convertimos Dp a Píxeles para dibujar
            val rowHeightPx = with(density) { rowHeight.toPx() }
            val startOffsetPx = with(density) { startOffset.toPx() }

            // Configuración del Óvalo Gigante (para que la curva sea suave)
            val ovalWidth = width * 3.0f
            val ovalHeight = height * 2.0f

            // Dibujamos una ola por cada fila de contenido
            for (i in 0 until rows + 1) { // +1 para asegurar que el fondo cubra abajo

                // Ciclo de colores
                val color = if (i >= rows) ColorArcDarkBrown else colorCycle[i % colorCycle.size]

                // CÁLCULO EXACTO: Posición basada en la altura de la fila
                val topPos = startOffsetPx + (i * rowHeightPx)

                drawOval(
                    color = color,
                    topLeft = Offset(
                        x = -(ovalWidth - width) / 2,
                        y = topPos
                    ),
                    size = Size(width = ovalWidth, height = ovalHeight)
                )
            }
        }
    }
}



@Preview(showBackground = true, widthDp = 411, heightDp = 891, name = "Fondo 4 Columnas")
@Composable
fun Background4x4Preview() {
    // Simulamos 16 categorías en 4 columnas -> Debería pintar 4 franjas de colores
    CategoriesBackground(
        categoryCount = 16,
        columnCount = 4,
        rowHeight = 130.dp,
        startOffset = 180.dp
    )
}