package com.example.topbooks.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// COLORES POR DEFECTO (Material 3)
// ============================================================================
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ============================================================================
// MODO OSCURO (Tema actual - Tonos marrones cálidos)
// ============================================================================
object DarkThemeColors {
    // Fondos
    val Background = Color(0xFFF6E6DD)
    val BackgroundCategorySection = Color(0xFFB9836B)
    val BackgroundRecommendedSection = Color(0xFFD9AD9A)
    val BackgroundFavoritesSection = Color(0xFFCEB5A5)
    
    // Secciones Home
    val RecommendedPopularBooks = Color(0xFFEAAF9B)
    val RecommendedTastesBooks = Color(0xFFD9B6A6)
    val RecommendedFriendsBooks = Color(0xFFA66953)
    
    // Categorías
    val CategoryDetailContentBackground = Color(0xFFD9AD9A)
    val CategoryTitle = Color(0xFF91604B)
    val ComponentBackground = Color(0xFFF6E6DD)
    
    // Arcos y bordes
    val HeaderBeige = Color(0xFFF6E6DD)
    val ArcDarkBrown = Color(0xFF8D5B4C)
    val ArcMediumBrown = Color(0xFFC89B8C)
    val ArcLightBeige = Color(0xFFF6E6DD)
    val ArcWhite = Color(0xFFF5ECE9)
    
    // Registro
    val SurfaceTextRegister = Color(0xFFB9836B)
    val ConditionOk = Color(0xFF74BA32)
    
    // Textos
    val TextPrimary = Color(0xFF5D4037)
    val TitleTopBooks = Color(0xFF91604B)
    val TitleCategoryDetail = Color(0xFF8E5D48)
    
    // Diario de lectura
    val JournalStar = Color(0xFFFFD54F)
    val JournalRomance = Color(0xFFFF4081)
    val JournalHappy = Color(0xFFFF9800)
    val JournalSad = Color(0xFF2979FF)
    val JournalSpicy = Color(0xFFD50000)
}

// ============================================================================
// MODO CLARO (Placeholder - El usuario definirá estos colores después)
// ============================================================================
object LightThemeColors {
    // Fondos
    val Background = Color(0xFFFAFAFA)
    val BackgroundCategorySection = Color(0xFFE0E0E0)
    val BackgroundRecommendedSection = Color(0xFFEEEEEE)
    val BackgroundFavoritesSection = Color(0xFFE8E8E8)
    
    // Secciones Home
    val RecommendedPopularBooks = Color(0xFFD0D0D0)
    val RecommendedTastesBooks = Color(0xFFC8C8C8)
    val RecommendedFriendsBooks = Color(0xFFB0B0B0)
    
    // Categorías
    val CategoryDetailContentBackground = Color(0xFFEEEEEE)
    val CategoryTitle = Color(0xFF424242)
    val ComponentBackground = Color(0xFFF5F5F5)
    
    // Arcos y bordes
    val HeaderBeige = Color(0xFFF5F5F5)
    val ArcDarkBrown = Color(0xFF212121)
    val ArcMediumBrown = Color(0xFF757575)
    val ArcLightBeige = Color(0xFFF5F5F5)
    val ArcWhite = Color(0xFFFAFAFA)
    
    // Registro
    val SurfaceTextRegister = Color(0xFF757575)
    val ConditionOk = Color(0xFF4CAF50)
    
    // Textos
    val TextPrimary = Color(0xFF212121)
    val TitleTopBooks = Color(0xFF424242)
    val TitleCategoryDetail = Color(0xFF424242)
    
    // Diario de lectura (mismos colores para ambos temas)
    val JournalStar = Color(0xFFFFD54F)
    val JournalRomance = Color(0xFFFF4081)
    val JournalHappy = Color(0xFFFF9800)
    val JournalSad = Color(0xFF2979FF)
    val JournalSpicy = Color(0xFFD50000)
}

// ============================================================================
// COLORES LOGIN (Nuevo diseño - Tonos elegantes cálidos)
// ============================================================================
object LoginColors {
    val Background = Color(0xFFFFF8F5)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceDim = Color(0xFFE1D8D5)
    val SurfaceContainer = Color(0xFFF5ECE8)
    val SurfaceContainerLow = Color(0xFFFBF2EE)
    val SurfaceContainerHigh = Color(0xFFEFE6E3)
    val SurfaceTint = Color(0xFF874F4D)
    val Primary = Color(0xFF30090A)
    val PrimaryContainer = Color(0xFF4A1D1D)
    val PrimaryFixedDim = Color(0xFFFDB4B1)
    val OnPrimary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFFE932C)
    val OnSurface = Color(0xFF1F1B19)
    val OnSurfaceVariant = Color(0xFF524343)
    val OutlineVariant = Color(0xFFD7C2C0)
    val Outline = Color(0xFF847372)
}

// ============================================================================
// ALIAS PARA COMPATIBILIDAD (Mantener código existente funcionando)
// ============================================================================
// Estos alias permiten que el código existente siga funcionando sin cambios
// mientras se migra gradualmente al nuevo sistema de temas
val ColorBackGroundGeneral get() = DarkThemeColors.Background
val ColorBackGroundCategorySection get() = DarkThemeColors.BackgroundCategorySection
val ColorBackGroundRecommendedSection get() = DarkThemeColors.BackgroundRecommendedSection
val ColorBackGroundFavoritesSection get() = DarkThemeColors.BackgroundFavoritesSection

val ColorRecommededPopularBooks get() = DarkThemeColors.RecommendedPopularBooks
val ColorRecommendedTastesBooks get() = DarkThemeColors.RecommendedTastesBooks
val ColorRecommendedFriendsBooks get() = DarkThemeColors.RecommendedFriendsBooks

val ColorCategoryDetailContentBackgroundShape get() = DarkThemeColors.CategoryDetailContentBackground
val ColorTitleCategoryDetail get() = DarkThemeColors.CategoryTitle
val ColorBackgorundComponente get() = DarkThemeColors.ComponentBackground

val ColorHeaderBeige get() = DarkThemeColors.HeaderBeige
val ColorArcDarkBrown get() = DarkThemeColors.ArcDarkBrown
val ColorArcMediumBrown get() = DarkThemeColors.ArcMediumBrown
val ColorArcLightBeige get() = DarkThemeColors.ArcLightBeige
val ColorArcWhite get() = DarkThemeColors.ArcWhite

val ColorSurfaceTextRegister get() = DarkThemeColors.SurfaceTextRegister
val ColorConditionOk get() = DarkThemeColors.ConditionOk

val ColorTextPrimary get() = DarkThemeColors.TextPrimary
val ColorTituloTopBooks get() = DarkThemeColors.TitleTopBooks
val ColorTituloCategoriaDetalle get() = DarkThemeColors.TitleCategoryDetail

val ColorJournalStar get() = DarkThemeColors.JournalStar
val ColorJournalRomance get() = DarkThemeColors.JournalRomance
val ColorJournalHappy get() = DarkThemeColors.JournalHappy
val ColorJournalSad get() = DarkThemeColors.JournalSad
val ColorJournalSpicy get() = DarkThemeColors.JournalSpicy

