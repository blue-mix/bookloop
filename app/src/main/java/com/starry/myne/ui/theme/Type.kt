package com.starry.myne.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.starry.myne.R


val poppinsFont = FontFamily(
    fonts = listOf(
        Font(
            resId = R.font.poppins_black,
            weight = FontWeight.Black,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.poppins_bold,
            weight = FontWeight.Bold,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.poppins_extrabold,
            weight = FontWeight.ExtraBold,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.poppins_extralight,
            weight = FontWeight.ExtraLight,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.poppins_light,
            weight = FontWeight.Light,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.poppins_medium,
            weight = FontWeight.Medium,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.poppins_regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.poppins_semibold,
            weight = FontWeight.SemiBold,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.poppins_thin,
            weight = FontWeight.Thin,
            style = FontStyle.Normal
        ),
    )
)

val pacificoFont = FontFamily(Font(R.font.pacifico_regular))

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = poppinsFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Proportional,
            trim = LineHeightStyle.Trim.Both
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = true)
    )
)
