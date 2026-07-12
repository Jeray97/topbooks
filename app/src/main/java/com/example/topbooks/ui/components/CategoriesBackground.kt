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

/**
 * Componente visual que dibuja el fondo curvo/ondulado para la pantalla de Categorías.
 * * TÉCNICA VISUAL: Utiliza un [Canvas] para dibujar múltiples óvalos gigantes superpuestos,
 * desplazados verticalmente, creando un efecto de capas que se adapta a las filas de contenido.
 *
 * @param modifier Modificador opcional para el contenedor principal.
 * @param categoryCount Número total de categorías a mostrar (para calcular cuántas olas dibujar).
 * @param columnCount Número de columnas en la cuadrícula (por defecto 4).
 * @param rowHeight Altura de cada fila en [Dp] (sirve para calcular la separación entre olas).
 * @param startOffset Desplazamiento vertical inicial en [Dp] donde empezará a dibujarse la primera ola.
 */
@Composable
fun CategoriesBackground(
    modifier: Modifier = Modifier,
    categoryCount: Int,
    columnCount: Int = 4,
    rowHeight: Dp,
    startOffset: Dp
) {
    val density = LocalDensity.current

    // Lista de colores que se irán alternando para cada capa/ola
    val colorCycle = listOf(ColorArcDarkBrown(), ColorArcMediumBrown(), ColorArcLightBeige(), ColorArcWhite())

    // Calculamos las filas necesarias (redondeando hacia arriba con ceil)
    val rows = ceil(categoryCount / columnCount.toFloat()).toInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorHeaderBeige()) // Color de fondo base (cielo)
    ) {
        // Lienzo de dibujo de bajo nivel
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Convertimos Dp a Píxeles para poder usarlos en las coordenadas del Canvas
            val rowHeightPx = with(density) { rowHeight.toPx() }
            val startOffsetPx = with(density) { startOffset.toPx() }

            // Configuración del Óvalo Gigante: Lo hacemos 3 veces más ancho que la pantalla
            // para que la curva visible sea muy suave y poco pronunciada.
            val ovalWidth = width * 3.0f
            val ovalHeight = height * 2.0f

            // Dibujamos una ola por cada fila de contenido
            // Se dibuja el bucle de atrás hacia adelante visualmente (las de abajo tapan a las de arriba)
            for (i in 0 until rows + 1) { // +1 extra para asegurar que el fondo cubra hasta abajo

                // Ciclo de colores: Si es la última franja extra de relleno, forzamos un color oscuro
                val color = if (i >= rows) Color(0xFF8D5B4C) else colorCycle[i % colorCycle.size]

                // CÁLCULO EXACTO: Posición Y (altura) de la ola basada en el desplazamiento y la altura de la fila
                val topPos = startOffsetPx + (i * rowHeightPx)

                drawOval(
                    color = color,
                    // Desplazamos la "X" hacia la izquierda para que el centro del óvalo coincida con el centro de la pantalla
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

/**
 * Función de previsualización para comprobar el dibujado del fondo curvo
 * sin necesidad de cargar toda la pantalla o lógica de categorías.
 */
@Preview(showBackground = true, widthDp = 411, heightDp = 891, name = "Fondo 4 Columnas")
@Composable
fun Background4x4Preview() {
    // Simulamos 16 categorías en 4 columnas -> Debería pintar 4 franjas de colores intercalados
    CategoriesBackground(
        categoryCount = 16,
        columnCount = 4,
        rowHeight = 130.dp,
        startOffset = 180.dp
    )
}