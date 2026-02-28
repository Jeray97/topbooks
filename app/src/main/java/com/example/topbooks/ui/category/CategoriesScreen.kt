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

// --- CONSTANTES ---
private val ROW_HEIGHT = 130.dp
private val START_OFFSET = 145.dp

data class CategoryUi(val name: String, val iconRes: Int, val query: String)

@Composable
fun CategoriesScreen(
    onBackClick: () -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit
) {
    // 🔥 GENERAMOS LA LISTA DINÁMICAMENTE DESDE EL PROVIDER
    val categories = CategoryProvider.allCategories.map { code ->
        val catData = CategoryProvider.getCategoryResources(code)
        val catName = if (catData.nameRes != null) stringResource(id = catData.nameRes) else CategoryProvider.formatFallbackName(code)
        val querySubject = code.lowercase(Locale.ROOT).replace("_", " ")
        CategoryUi(name = catName, iconRes = catData.iconRes, query = "subject:$querySubject")
    }

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

            // 1. EL FONDO (Alineado con el área segura)
            CategoriesBackground(
                categoryCount = categories.size,
                columnCount = 4,
                rowHeight = ROW_HEIGHT,
                startOffset = START_OFFSET
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                SearchBarCustom(onBookClick = onBookClick, onScanClick = onScanClick)

                Text(
                    text = "Categorías",
                    fontFamily = CenturyGotic,
                    fontSize = 24.sp,
                    color = ColorTituloCategoriaDetalle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                )

                // --- GRID ---
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ROW_HEIGHT),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        rowItems.forEachIndexed { colIndex, item ->

                            // Calculamos el desplazamiento
                            val curveOffset = calculateOffset(
                                colIndex = colIndex,
                                totalCols = 4,
                                widthDp = screenWidthDp,
                                heightDp = screenHeightDp,
                                density = density
                            )

                            // Aplicamos el desplazamiento
                            Box(modifier = Modifier.offset(y = curveOffset)) {
                                CategoryItem(
                                    category = item,
                                    onClick = { onCategoryClick(item.name, item.query) }
                                )
                            }
                        }

                        // Rellenar huecos
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
 * Función auxiliar con corrección de tipos (Double -> Float)
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

        // Geometría del fondo
        val ovalWidth = widthPx * 3.0f
        val ovalHeight = heightPx * 2.0f

        val radiusX = ovalWidth / 2
        val radiusY = ovalHeight / 2

        // Cálculo de posición X
        val colWidth = widthPx / totalCols
        val xPos = (colIndex + 0.5f) * colWidth
        val distFromCenter = abs(xPos - (widthPx / 2))

        // Ecuación de la elipse
        val term = 1.0 - (distFromCenter * distFromCenter) / (radiusX * radiusX)

        val heightAtX = if (term > 0) (radiusY * sqrt(term)).toFloat() else 0f

        val drop = radiusY - heightAtX
        val baseMarginPx = 25.dp.toPx()

        (drop + baseMarginPx).toDp()
    }
}

@Composable
fun CategoryItem(
    category: CategoryUi,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
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