package com.starry.myne.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.starry.myne.R
import com.starry.myne.ui.screens.settings.viewmodels.ThemeMode
import custom.AppTheme
import custom.components.Text

@Composable
fun BookDetailTopUI(
    title: String,
    authors: String,
    imageData: Any?,
    currentThemeMode: ThemeMode,
    showReaderBackground: Boolean = false,
    progressPercent: String? = null
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        // Blurred/Background image
        AsyncImage(
            model = imageData ?: R.drawable.book_details_bg,
            contentDescription = null,
            alpha = 0.6f,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppTheme.colors.background.copy(alpha = 0.95f),
                            Color.Transparent,
                            AppTheme.colors.background
                        ),
                        startY = 50f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover
            val imageBackground =
                if (currentThemeMode == ThemeMode.Dark) AppTheme.colors.onSurface
                else AppTheme.colors.elevation

            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(170.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(imageBackground)
                    .shadow(12.dp, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageData)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = R.drawable.placeholder_cat),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Book Info
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = authors,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = AppTheme.colors.onBackground.copy(alpha = 0.85f)
                )

                if (!progressPercent.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$progressPercent% Completed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AppTheme.colors.elevation)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercent.toFloatOrNull()?.div(100f) ?: 0f)
                                .clip(RoundedCornerShape(50))
                                .background(AppTheme.colors.primary)
                        )
                    }
                }
            }
        }
    }
}

//@Composable
//fun BookDetailTopUI(
//    title: String,
//    authors: String,
//    imageData: Any?,
//    currentThemeMode: ThemeMode,
//    showReaderBackground: Boolean = false,
//    progressPercent: String? = null
//) {
//    val context = LocalContext.current
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(235.dp)
//    ) {
//        AsyncImage(
//            model = if (showReaderBackground) R.drawable.book_reader_bg else R.drawable.book_details_bg,
//            contentDescription = null,
//            alpha = 0.35f,
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(
//                    Brush.verticalGradient(
//                        colors = listOf(
//                            AppTheme.colors.background,
//                            Color.Transparent,
//                            AppTheme.colors.background
//                        ), startY = 8f
//                    )
//                )
//        )
//
//        Row(modifier = Modifier.fillMaxSize()) {
//
//            Box(
//                modifier = Modifier
//                    .fillMaxHeight()
//                    .padding(16.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                val imageBackground =
//                    if (currentThemeMode == ThemeMode.Dark) {
//                        AppTheme.colors.onSurface
//                    } else {
//                        AppTheme.colors.elevation
//                    }
//                Box(
//                    modifier = Modifier
//                        .shadow(24.dp)
//                        .clip(RoundedCornerShape(8.dp))
//                        .background(imageBackground)
//                ) {
//                    AsyncImage(
//                        model = ImageRequest.Builder(context)
//                            .data(imageData)
//                            .crossfade(true).build(),
//                        placeholder = painterResource(id = R.drawable.placeholder_cat),
//                        contentDescription = null,
//                        modifier = Modifier
//                            .width(118.dp)
//                            .height(160.dp),
//                        contentScale = ContentScale.Crop
//                    )
//                }
//            }
//
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.Center,
//            ) {
//                Spacer(Modifier.height(32.dp))
//                Text(
//                    text = title,
//                    modifier = Modifier
//                        .padding(horizontal = 12.dp)
//                        .fillMaxWidth(),
//                    fontSize = 22.sp,
//                    maxLines = 3,
//                    lineHeight = 28.sp,
//                    overflow = TextOverflow.Ellipsis,
//                    fontWeight = FontWeight.SemiBold,
//                    color = AppTheme.colors.onBackground,
//                )
//
//                Text(
//                    text = authors,
//                    modifier = Modifier.padding(
//                        start = 12.dp, end = 8.dp, top = 2.dp
//                    ),
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium,
//                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis,
//                    color = AppTheme.colors.onBackground,
//                )
//
//                if (!progressPercent.isNullOrBlank()) {
//                    Text(
//                        text = "$progressPercent% Completed",
//                        modifier = Modifier.padding(start = 12.dp, end = 8.dp),
//                        fontSize = 15.sp,
//                        fontWeight = FontWeight.Medium,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis,
//                        color = AppTheme.colors.onBackground,
//                    )
//                }
//                Spacer(modifier = Modifier.height(50.dp))
//            }
//        }
//    }
//}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    BookDetailTopUI(
        title = "The Complete works of william shakespeare",
        authors = "F. Scott Fitzgerald",
        imageData = null,
        currentThemeMode = ThemeMode.Dark
    )
}