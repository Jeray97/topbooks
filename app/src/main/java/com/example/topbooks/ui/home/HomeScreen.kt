package com.example.topbooks.ui.home


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.topbooks.ui.theme.*
import com.example.topbooks.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

@Composable
fun HomeScreen() {

    //Estructura principal con Scroll vertical
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackGroundGeneral) // Usamos tu color del tema
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        //1. Titulo del header
        Text(
            text = "TopBooks",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextPrimary, // Usamos tu color del tema
            modifier = Modifier.padding(bottom = 16.dp)
        )

        //2. Barra de búsqueda
        SearchBarCustom()

        Spacer(modifier = Modifier.height(24.dp))

        //3. Seccion de categorias
        SectionContainer(
            title = stringResource(R.string.section_categories),
            backgroundColor = ColorBackGroundCategorySection // Color específico
        ) {
            CategoryRow()
        }

        Spacer(modifier = Modifier.height(16.dp))

        //4. Sección Recomendados
        SectionContainer(
            title = stringResource(R.string.section_recommended),
            backgroundColor = ColorBackGroundRecommendedSection // Color específico
        ) {
            BookPlaceholderRow()
        }

        Spacer(modifier = Modifier.height(16.dp))

        //5. Sección Favoritos
        SectionContainer(
            title = stringResource(R.string.section_friends_favorites),
            backgroundColor = ColorBackGroundFavoritesSection // Color específico
        ) {
            BookPlaceholderRow()
        }

        Spacer(modifier = Modifier.height(80.dp)) // Espacio para el BottomNav
    }

}

@Composable
fun SearchBarCustom() {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = {Text(stringResource(R.string.search_hint), color = Color.Gray)},
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .background(Color.White, RoundedCornerShape(8.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            trailingIcon = {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.desc_search_icon), tint = Color.Gray)
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .width(56.dp)
                .height(56.dp) // Aseguramos altura cuadrada
                .background(Color.White, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ){
            Icon(painter = painterResource(R.drawable.icon_codigodebarras), contentDescription = stringResource(R.string.desc_scan_icon), tint = Color.Unspecified)
        }
    }
}

@Composable
fun SectionContainer(
    title: String,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                // Usamos el icono con su ruta correcta
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.desc_arrow_forward),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun CategoryRow() {
    val categories = listOf(
        stringResource(R.string.category_romance) to Icons.Default.Favorite,
        stringResource(R.string.category_mystery) to Icons.Default.Search,
        stringResource(R.string.category_horror) to Icons.Default.Warning,
        stringResource(R.string.category_fantasy) to Icons.Default.Star
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories) { (name, icon) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = ColorBackGroundCategorySection, // Usamos el color de la sección
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        color = ColorBackGroundCategorySection, // Usamos el color de la sección
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BookPlaceholderRow() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(5) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}