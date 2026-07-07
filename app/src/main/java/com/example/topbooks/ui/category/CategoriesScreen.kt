package com.example.topbooks.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.topbooks.R
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.CategoryProvider
import java.util.Locale

data class CategoryUi(val name: String, val iconRes: Int, val query: String)

private val CATEGORY_COLORS = listOf(
    Pair(Color(0xFF4A1D1D), Color(0xFFC3817F)),
    Pair(Color(0xFFE9E2D0), Color(0xFF1E1C10)),
    Pair(Color(0xFFEAE1DD), Color(0xFF524343)),
    Pair(Color(0xFFFFDCC3), Color(0xFF2F1500)),
    Pair(Color(0xFFFFDAD6), Color(0xFF93000A)),
)

@Composable
fun CategoriesScreen(
    onBackClick: () -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit
) {
    val categories = CategoryProvider.allCategories.map { code ->
        val catData = CategoryProvider.getCategoryResources(code)
        val catName = if (catData.nameRes != null) stringResource(id = catData.nameRes) else CategoryProvider.formatFallbackName(code)
        val querySubject = code.lowercase(Locale.ROOT).replace("_", " ")
        CategoryUi(name = catName, iconRes = catData.iconRes, query = "subject:$querySubject")
    }

    Scaffold(
        containerColor = LoginColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = LoginColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = GuardianCity,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoginColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.categories_title),
                fontFamily = GuardianCity,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = LoginColors.Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Explora todos los géneros disponibles",
                fontSize = 16.sp,
                color = LoginColors.OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { category ->
                    CategoryItem(
                        category = category,
                        onClick = { onCategoryClick(category.name, category.query) }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: CategoryUi,
    onClick: () -> Unit
) {
    val colorIndex = category.name.hashCode().let { kotlin.math.abs(it) } % CATEGORY_COLORS.size
    val (iconBg, iconFg) = CATEGORY_COLORS[colorIndex]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(LoginColors.SurfaceContainerLow)
            .border(1.dp, LoginColors.OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = category.iconRes),
                contentDescription = category.name,
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = category.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = LoginColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
