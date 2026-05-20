package com.ai.wardrobe.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.ai.wardrobe.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage  = "com.google.android.gms",
    certificates     = R.array.com_google_android_gms_fonts_certs
)

private val BodoniModaFont    = GoogleFont("Bodoni Moda")
private val HankenGroteskFont = GoogleFont("Hanken Grotesk")

val BodoniModa = FontFamily(
    Font(googleFont = BodoniModaFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = BodoniModaFont, fontProvider = provider, weight = FontWeight.Bold)
)

val HankenGrotesk = FontFamily(
    Font(googleFont = HankenGroteskFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = HankenGroteskFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = HankenGroteskFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = HankenGroteskFont, fontProvider = provider, weight = FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = BodoniModa,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.4.sp,
        letterSpacing = (-0.32).sp
    ),
    displayMedium = TextStyle(
        fontFamily = BodoniModa,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 31.2.sp,
        letterSpacing = 4.8.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BodoniModa,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BodoniModa,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.2.sp
    )
)
