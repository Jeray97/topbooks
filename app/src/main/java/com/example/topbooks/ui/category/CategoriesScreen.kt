package com.example.topbooks.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.topbooks.R
import com.example.topbooks.ui.components.CategoriesBackground
import com.example.topbooks.ui.components.SearchBarCustom
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.CategoryProvider
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

// --- CONSTANTES DE DISEÑO ---
/** Altura fija asignada a cada fila de categorías. */
private val ROW_HEIGHT = 130.dp
/** Distancia inicial desde la que empiezan a dibujarse las curvas de fondo. */
private val START_OFFSET = 145.dp

/**
 * Modelo de datos visual para representar una categoría en la pantalla.
 * @property name Nombre traducido a mostrar al usuario (ej. "Ciencia Ficción").
 * @property iconRes ID del recurso de la imagen (icono) de la categoría.
 * @property query Texto de búsqueda exacto que se enviará a las APIs (ej. "subject:science fiction").
 */
data class CategoryUi(val name: String, val iconRes: Int, val query: String)

/**
 * PANTALLA PRINCIPAL DE CATEGORÍAS (Stateful/Layout Composable)
 * * Muestra un grid curvo interactivo generado dinámicamente a partir del [CategoryProvider].
 *
 * @param onBackClick Acción al pulsar el botón de volver en la TopBar.
 * @param onCategoryClick Acción que se dispara al tocar un género. Envía el título y la query de búsqueda.
 * @param onBookClick Acción delegada desde la barra de búsqueda para abrir un libro concreto.
 * @param onScanClick Acción delegada desde la barra de búsqueda para abrir el escáner de códigos de barras.
 */
@Composable
fun CategoriesScreen(
    onBackClick: () -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit
) {
    // GENERAMOS LA LISTA DINÁMICAMENTE DESDE EL PROVIDER
    val categories = CategoryProvider.allCategories.map { code ->
        val catData = CategoryProvider.getCategoryResources(code)
        val catName = if (catData.nameRes != null) stringResource(id = catData.nameRes) else CategoryProvider.formatFallbackName(code)

        // Formateamos la query para que sea compatible con Google Books y Open Library ("subject:...")
        val querySubject = code.lowercase(Locale.ROOT).replace("_", " ")
        CategoryUi(name = catName, iconRes = catData.iconRes, query = "subject:$querySubject")
    }

    // Dividimos la lista en grupos de 4 para dibujar las filas del grid
    val rows = categories.chunked(4)

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val density = LocalDensity.current

    // ESTRUCTURA CON TOPBAR
    Scaffold(
        containerColor = ColorHeaderBeige, // Fondo base por si acaso
        topBar = {
            TopBar(onBackClick = onBackClick)
        }
    ) { paddingValues ->

        // Box contenedor que respeta el padding del Scaffold
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // 1. EL FONDO (Dibujado mediante Canvas para crear las ondas/curvas)
            CategoriesBackground(
                categoryCount = categories.size,
                columnCount = 4,
                rowHeight = ROW_HEIGHT,
                startOffset = START_OFFSET
            )

            // 2. CONTENIDO PRINCIPAL
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                SearchBarCustom(onBookClick = onBookClick, onScanClick = onScanClick)

                Text(
                    text = stringResource(R.string.categories_title),
                    fontFamily = CenturyGotic,
                    fontSize = 24.sp,
                    color = ColorTituloCategoriaDetalle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                )

                // --- GRID CURVO DINÁMICO ---
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ROW_HEIGHT),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        rowItems.forEachIndexed { colIndex, item ->

                            // Calculamos el desplazamiento vertical matemático para que el icono
                            // se sitúe exactamente sobre la línea curva dibujada en el fondo.
                            val curveOffset = calculateOffset(
                                colIndex = colIndex,
                                totalCols = 4,
                                widthDp = screenWidthDp,
                                heightDp = screenHeightDp,
                                density = density
                            )

                            // Aplicamos el desplazamiento calculado
                            Box(modifier = Modifier.offset(y = curveOffset)) {
                                CategoryItem(
                                    category = item,
                                    onClick = { onCategoryClick(item.name, item.query) }
                                )
                            }
                        }

                        // Rellenar huecos vacíos si la última fila tiene menos de 4 categorías
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.width(65.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

/**
 * Calcula el desplazamiento vertical (Offset Y) necesario para que un elemento
 * parezca estar apoyado sobre una curva elíptica convexa.
 * * * FUNCIONAMIENTO MATEMÁTICO:
 * Utiliza la ecuación estándar de la elipse: $ \frac{x^2}{a^2} + \frac{y^2}{b^2} = 1 $
 * Despejando 'y', calcula la altura exacta en la que se encuentra la elipse en un punto 'x' dado (la columna).
 * La diferencia entre el punto más alto (centro) y esta altura 'y' es el 'drop' (caída) que debemos aplicar.
 *
 * @param colIndex Índice de la columna actual (0, 1, 2 o 3).
 * @param totalCols Número total de columnas en la fila.
 * @param widthDp Ancho de la pantalla en dp.
 * @param heightDp Alto de la pantalla en dp.
 * @param density Densidad de píxeles del dispositivo actual.
 * @return El desplazamiento vertical en unidades [Dp].
 */
fun calculateOffset(
    colIndex: Int,
    totalCols: Int,
    widthDp: Dp,
    heightDp: Dp,
    density: Density
): Dp {
    return with(density) {
        val widthPx = widthDp.toPx()
        val heightPx = heightDp.toPx()

        // Geometría del fondo: Creamos una elipse imaginaria gigante
        val ovalWidth = widthPx * 3.0f
        val ovalHeight = heightPx * 2.0f

        val radiusX = ovalWidth / 2
        val radiusY = ovalHeight / 2

        // Cálculo de posición X del centro del icono
        val colWidth = widthPx / totalCols
        val xPos = (colIndex + 0.5f) * colWidth
        val distFromCenter = abs(xPos - (widthPx / 2))

        // Ecuación de la elipse (despejando la variable)
        val term = 1.0 - (distFromCenter * distFromCenter) / (radiusX * radiusX)

        val heightAtX = if (term > 0) (radiusY * sqrt(term)).toFloat() else 0f

        // Caída desde el punto más alto de la curva
        val drop = radiusY - heightAtX
        val baseMarginPx = 25.dp.toPx()

        (drop + baseMarginPx).toDp()
    }
}

/**
 * Componente visual que representa una única categoría dentro de la cuadrícula.
 * * Muestra un círculo blanco con el icono de la categoría y una "píldora" inferior con el nombre.
 */
@Composable
fun CategoryItem(
    category: CategoryUi,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        // Icono circular
        Box(
            modifier = Modifier
                .size(65.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = category.iconRes),
                contentDescription = category.name,
                tint = Color.Unspecified,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Píldora con el nombre
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = category.name,
                    color = Color(0xFFB9836B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CenturyGotic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun CategoriesCurvePreview() {
    TopBooksTheme {
        CategoriesScreen(
            onBackClick = {},
            onCategoryClick = { _, _ -> },
            onBookClick = {},
            onScanClick = {}
        )
    }
}