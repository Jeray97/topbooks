package com.example.topbooks.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.topbooks.R

// 1. Definición de familias
val GuardianCity = FontFamily(
    Font(R.font.guardian_city_font, FontWeight.Normal)
)

val CenturyGotic = FontFamily(
    Font(R.font.century_gothic_font, FontWeight.Normal)
)

// 2. Configuración de Tipografía
val Typography = Typography(
    // Títulos: Aquí usamos GuardianCity porque queremos que destaque
    titleLarge = TextStyle(
        fontFamily = GuardianCity,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    // Cuerpo de texto (Inputs, descripciones, etc):
    // Usamos CenturyGotic
    bodyLarge = TextStyle(
        fontFamily = CenturyGotic,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // Etiquetas (Labels de los text fields)
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)