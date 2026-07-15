package com.example.topbooks.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

object AthenaeumNoirColors {
    // Fondos: invertidos de beige claro a marrón muy oscuro
    val Surface = Color(0xFF1A1210)
    val SurfaceDim = Color(0xFF1A1210)
    val SurfaceBright = Color(0xFF3D2F2A)
    val SurfaceContainerLowest = Color(0xFF140E0C)
    val SurfaceContainerLow = Color(0xFF231A17)
    val SurfaceContainer = Color(0xFF2A1F1C)
    val SurfaceContainerHigh = Color(0xFF352824)
    val SurfaceContainerHighest = Color(0xFF40312D)
    
    // Textos: invertidos de marrón oscuro a beige claro
    val OnSurface = Color(0xFFF6E6DD)
    val OnSurfaceVariant = Color(0xFFD9AD9A)
    val InverseSurface = Color(0xFFF6E6DD)
    val InverseOnSurface = Color(0xFF2A1F1C)
    
    // Bordes y contornos
    val Outline = Color(0xFF8D5B4C)
    val OutlineVariant = Color(0xFF4A3530)
    val SurfaceTint = Color(0xFFC89B8C)

    // Primary: invertido de marrón oscuro a beige claro
    val Primary = Color(0xFFC89B8C)
    val OnPrimary = Color(0xFF1A1210)
    val PrimaryContainer = Color(0xFF8D5B4C)
    val OnPrimaryContainer = Color(0xFFF6E6DD)
    val InversePrimary = Color(0xFF6B4438)

    // Secondary
    val Secondary = Color(0xFFB9836B)
    val OnSecondary = Color(0xFF1A1210)
    val SecondaryContainer = Color(0xFF5D4037)
    val OnSecondaryContainer = Color(0xFFD9AD9A)

    // Tertiary
    val Tertiary = Color(0xFFCEB5A5)
    val OnTertiary = Color(0xFF2A1F1C)
    val TertiaryContainer = Color(0xFF6B4438)
    val OnTertiaryContainer = Color(0xFFEAAF9B)

    // Error (mantener similar)
    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)

    // Fixed colors
    val PrimaryFixed = Color(0xFFD9AD9A)
    val PrimaryFixedDim = Color(0xFFC89B8C)
    val OnPrimaryFixed = Color(0xFF1A1210)
    val OnPrimaryFixedVariant = Color(0xFF6B4438)

    val SecondaryFixed = Color(0xFFCEB5A5)
    val SecondaryFixedDim = Color(0xFFB9836B)
    val OnSecondaryFixed = Color(0xFF1A1210)
    val OnSecondaryFixedVariant = Color(0xFF5D4037)

    val TertiaryFixed = Color(0xFFD9B6A6)
    val TertiaryFixedDim = Color(0xFFCEB5A5)
    val OnTertiaryFixed = Color(0xFF1A1210)
    val OnTertiaryFixedVariant = Color(0xFF6B4438)

    // Background principal
    val Background = Color(0xFF1A1210)
    val OnBackground = Color(0xFFF6E6DD)
    val SurfaceVariant = Color(0xFF40312D)

    // Secciones de fondo: invertidas de beige/marrón claro a marrón oscuro
    val BackgroundCategorySection = Color(0xFF231A17)
    val BackgroundRecommendedSection = Color(0xFF2A1F1C)
    val BackgroundFavoritesSection = Color(0xFF352824)

    val RecommendedPopularBooks = Color(0xFF352824)
    val RecommendedTastesBooks = Color(0xFF2A1F1C)
    val RecommendedFriendsBooks = Color(0xFF231A17)

    val CategoryDetailContentBackground = Color(0xFF2A1F1C)
    val CategoryTitle = Color(0xFFD9AD9A)
    val ComponentBackground = Color(0xFF231A17)

    // Arcos y elementos decorativos: invertidos
    val HeaderBeige = Color(0xFF1A1210)
    val ArcDarkBrown = Color(0xFFC89B8C)
    val ArcMediumBrown = Color(0xFF8D5B4C)
    val ArcLightBeige = Color(0xFF1A1210)
    val ArcWhite = Color(0xFF231A17)

    val SurfaceTextRegister = Color(0xFFD9AD9A)
    val ConditionOk = Color(0xFF74BA32)

    // Textos: invertidos de marrón oscuro a beige claro
    val TextPrimary = Color(0xFFF6E6DD)
    val TitleTopBooks = Color(0xFFC89B8C)
    val TitleCategoryDetail = Color(0xFFD9AD9A)

    // Journal colors (mantener vivos)
    val JournalStar = Color(0xFFFFD54F)
    val JournalRomance = Color(0xFFFF4081)
    val JournalHappy = Color(0xFFFF9800)
    val JournalSad = Color(0xFF2979FF)
    val JournalSpicy = Color(0xFFD50000)
}

object LightThemeColors {
    val Background = Color(0xFFF6E6DD)
    val BackgroundCategorySection = Color(0xFFB9836B)
    val BackgroundRecommendedSection = Color(0xFFD9AD9A)
    val BackgroundFavoritesSection = Color(0xFFCEB5A5)

    val RecommendedPopularBooks = Color(0xFFEAAF9B)
    val RecommendedTastesBooks = Color(0xFFD9B6A6)
    val RecommendedFriendsBooks = Color(0xFFA66953)

    val CategoryDetailContentBackground = Color(0xFFD9AD9A)
    val CategoryTitle = Color(0xFF91604B)
    val ComponentBackground = Color(0xFFF6E6DD)

    val HeaderBeige = Color(0xFFF6E6DD)
    val ArcDarkBrown = Color(0xFF8D5B4C)
    val ArcMediumBrown = Color(0xFFC89B8C)
    val ArcLightBeige = Color(0xFFF6E6DD)
    val ArcWhite = Color(0xFFF5ECE9)

    val SurfaceTextRegister = Color(0xFFB9836B)
    val ConditionOk = Color(0xFF74BA32)

    val TextPrimary = Color(0xFF5D4037)
    val TitleTopBooks = Color(0xFF91604B)
    val TitleCategoryDetail = Color(0xFF8E5D48)

    val JournalStar = Color(0xFFFFD54F)
    val JournalRomance = Color(0xFFFF4081)
    val JournalHappy = Color(0xFFFF9800)
    val JournalSad = Color(0xFF2979FF)
    val JournalSpicy = Color(0xFFD50000)
}

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

data class AppColors(
    val background: Color,
    val backgroundCategorySection: Color,
    val backgroundRecommendedSection: Color,
    val backgroundFavoritesSection: Color,
    val recommendedPopularBooks: Color,
    val recommendedTastesBooks: Color,
    val recommendedFriendsBooks: Color,
    val categoryDetailContentBackground: Color,
    val categoryTitle: Color,
    val componentBackground: Color,
    val headerBeige: Color,
    val arcDarkBrown: Color,
    val arcMediumBrown: Color,
    val arcLightBeige: Color,
    val arcWhite: Color,
    val surfaceTextRegister: Color,
    val conditionOk: Color,
    val textPrimary: Color,
    val titleTopBooks: Color,
    val titleCategoryDetail: Color,
    val journalStar: Color,
    val journalRomance: Color,
    val journalHappy: Color,
    val journalSad: Color,
    val journalSpicy: Color
)

val DarkAppColors = AppColors(
    background = AthenaeumNoirColors.Background,
    backgroundCategorySection = AthenaeumNoirColors.BackgroundCategorySection,
    backgroundRecommendedSection = AthenaeumNoirColors.BackgroundRecommendedSection,
    backgroundFavoritesSection = AthenaeumNoirColors.BackgroundFavoritesSection,
    recommendedPopularBooks = AthenaeumNoirColors.RecommendedPopularBooks,
    recommendedTastesBooks = AthenaeumNoirColors.RecommendedTastesBooks,
    recommendedFriendsBooks = AthenaeumNoirColors.RecommendedFriendsBooks,
    categoryDetailContentBackground = AthenaeumNoirColors.CategoryDetailContentBackground,
    categoryTitle = AthenaeumNoirColors.CategoryTitle,
    componentBackground = AthenaeumNoirColors.ComponentBackground,
    headerBeige = AthenaeumNoirColors.HeaderBeige,
    arcDarkBrown = AthenaeumNoirColors.ArcDarkBrown,
    arcMediumBrown = AthenaeumNoirColors.ArcMediumBrown,
    arcLightBeige = AthenaeumNoirColors.ArcLightBeige,
    arcWhite = AthenaeumNoirColors.ArcWhite,
    surfaceTextRegister = AthenaeumNoirColors.SurfaceTextRegister,
    conditionOk = AthenaeumNoirColors.ConditionOk,
    textPrimary = AthenaeumNoirColors.TextPrimary,
    titleTopBooks = AthenaeumNoirColors.TitleTopBooks,
    titleCategoryDetail = AthenaeumNoirColors.TitleCategoryDetail,
    journalStar = AthenaeumNoirColors.JournalStar,
    journalRomance = AthenaeumNoirColors.JournalRomance,
    journalHappy = AthenaeumNoirColors.JournalHappy,
    journalSad = AthenaeumNoirColors.JournalSad,
    journalSpicy = AthenaeumNoirColors.JournalSpicy
)

val LightAppColors = AppColors(
    background = LightThemeColors.Background,
    backgroundCategorySection = LightThemeColors.BackgroundCategorySection,
    backgroundRecommendedSection = LightThemeColors.BackgroundRecommendedSection,
    backgroundFavoritesSection = LightThemeColors.BackgroundFavoritesSection,
    recommendedPopularBooks = LightThemeColors.RecommendedPopularBooks,
    recommendedTastesBooks = LightThemeColors.RecommendedTastesBooks,
    recommendedFriendsBooks = LightThemeColors.RecommendedFriendsBooks,
    categoryDetailContentBackground = LightThemeColors.CategoryDetailContentBackground,
    categoryTitle = LightThemeColors.CategoryTitle,
    componentBackground = LightThemeColors.ComponentBackground,
    headerBeige = LightThemeColors.HeaderBeige,
    arcDarkBrown = LightThemeColors.ArcDarkBrown,
    arcMediumBrown = LightThemeColors.ArcMediumBrown,
    arcLightBeige = LightThemeColors.ArcLightBeige,
    arcWhite = LightThemeColors.ArcWhite,
    surfaceTextRegister = LightThemeColors.SurfaceTextRegister,
    conditionOk = LightThemeColors.ConditionOk,
    textPrimary = LightThemeColors.TextPrimary,
    titleTopBooks = LightThemeColors.TitleTopBooks,
    titleCategoryDetail = LightThemeColors.TitleCategoryDetail,
    journalStar = LightThemeColors.JournalStar,
    journalRomance = LightThemeColors.JournalRomance,
    journalHappy = LightThemeColors.JournalHappy,
    journalSad = LightThemeColors.JournalSad,
    journalSpicy = LightThemeColors.JournalSpicy
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

@Composable
fun ColorBackGroundGeneral() = LocalAppColors.current.background
@Composable
fun ColorBackGroundCategorySection() = LocalAppColors.current.backgroundCategorySection
@Composable
fun ColorBackGroundRecommendedSection() = LocalAppColors.current.backgroundRecommendedSection
@Composable
fun ColorBackGroundFavoritesSection() = LocalAppColors.current.backgroundFavoritesSection

@Composable
fun ColorRecommededPopularBooks() = LocalAppColors.current.recommendedPopularBooks
@Composable
fun ColorRecommendedTastesBooks() = LocalAppColors.current.recommendedTastesBooks
@Composable
fun ColorRecommendedFriendsBooks() = LocalAppColors.current.recommendedFriendsBooks

@Composable
fun ColorCategoryDetailContentBackgroundShape() = LocalAppColors.current.categoryDetailContentBackground
@Composable
fun ColorTitleCategoryDetail() = LocalAppColors.current.categoryTitle
@Composable
fun ColorBackgorundComponente() = LocalAppColors.current.componentBackground

@Composable
fun ColorHeaderBeige() = LocalAppColors.current.headerBeige
@Composable
fun ColorArcDarkBrown() = LocalAppColors.current.arcDarkBrown
@Composable
fun ColorArcMediumBrown() = LocalAppColors.current.arcMediumBrown
@Composable
fun ColorArcLightBeige() = LocalAppColors.current.arcLightBeige
@Composable
fun ColorArcWhite() = LocalAppColors.current.arcWhite

@Composable
fun ColorSurfaceTextRegister() = LocalAppColors.current.surfaceTextRegister
@Composable
fun ColorConditionOk() = LocalAppColors.current.conditionOk

@Composable
fun ColorTextPrimary() = LocalAppColors.current.textPrimary
@Composable
fun ColorTituloTopBooks() = LocalAppColors.current.titleTopBooks
@Composable
fun ColorTituloCategoriaDetalle() = LocalAppColors.current.titleCategoryDetail

@Composable
fun ColorJournalStar() = LocalAppColors.current.journalStar
@Composable
fun ColorJournalRomance() = LocalAppColors.current.journalRomance
@Composable
fun ColorJournalHappy() = LocalAppColors.current.journalHappy
@Composable
fun ColorJournalSad() = LocalAppColors.current.journalSad
@Composable
fun ColorJournalSpicy() = LocalAppColors.current.journalSpicy
