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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.starry.myne.R
import com.starry.myne.helpers.weakHapticFeedback
import com.starry.myne.ui.common.placeholder.placeholder
import custom.AppTheme
import custom.components.Text
import custom.components.card.CardDefaults
import custom.components.card.OutlinedCard

@Composable
fun BookItemCard(
    title: String,
    author: String,
    language: String,
    subjects: String,
    coverImageUrl: String?,
    loadingEffect: Boolean = false,
    onClick: () -> Unit
) {
    val view = LocalView.current

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        onClick = {
            view.weakHapticFeedback()
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.background,
            contentColor = AppTheme.colors.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Cover
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppTheme.colors.elevation)
                    .placeholder(isLoading = loadingEffect)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(coverImageUrl)
                        .crossfade(true).build(),
                    placeholder = painterResource(id = R.drawable.placeholder_cat),
                    contentDescription = stringResource(id = R.string.cover_image_desc),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = AppTheme.colors.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .placeholder(isLoading = loadingEffect)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = author,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = AppTheme.colors.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .placeholder(isLoading = loadingEffect)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Meta Info Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = language,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.primary,
                        modifier = Modifier
                            .background(
                                AppTheme.colors.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .placeholder(isLoading = loadingEffect)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = subjects,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = AppTheme.colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .weight(1f)
                            .placeholder(isLoading = loadingEffect)
                    )
                }
            }
        }
    }
}

//
//@Composable
//fun BookItemCard(
//    title: String,
//    author: String,
//    language: String,
//    subjects: String,
//    coverImageUrl: String?,
//    loadingEffect: Boolean = false,
//    onClick: () -> Unit
//) {
//    val view = LocalView.current
//    OutlinedCard(
//        modifier = Modifier
//            .height(160.dp)
//            .fillMaxWidth(),
//        onClick = {
//            view.weakHapticFeedback()
//            onClick()
//        },
//        colors = CardDefaults.cardColors(
//            containerColor = AppTheme.colors.surface,
//            contentColor = AppTheme.colors.onSurface
//        ),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = 2.dp,
//            pressedElevation = 4.dp
//        ),
//        shape = RoundedCornerShape(6.dp)
//    ) {
//        Row(modifier = Modifier
//            .fillMaxSize()
//            .background(AppTheme.colors.background)) {
//            Box(
//                modifier = Modifier
//                    .weight(1.5f)
//                    .padding(10.dp)
//                    .clip(RoundedCornerShape(6.dp))
//                    .placeholder(isLoading = loadingEffect)
//            ) {
//                AsyncImage(
//                    model = ImageRequest.Builder(LocalContext.current).data(coverImageUrl)
//                        .crossfade(true).build(),
//                    placeholder = painterResource(id = R.drawable.placeholder_cat),
//                    contentDescription = stringResource(id = R.string.cover_image_desc),
//                    modifier = Modifier.fillMaxSize(),
//                    contentScale = ContentScale.Crop
//                )
//            }
//
//            Column(
//                modifier = Modifier
//                    .weight(3f)
//                    .fillMaxHeight()
//            ) {
//                Spacer(modifier = Modifier.weight(1f))
//
//                Text(
//                    text = title,
//                    modifier = Modifier
//                        .padding(start = 12.dp, end = 8.dp, top = 2.dp)
//                        .fillMaxWidth()
//                        .placeholder(isLoading = loadingEffect),
//                    fontStyle = AppTheme.typography.h2.fontStyle,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Bold,
//                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis,
//                    color = AppTheme.colors.onSurface,
//                )
//
//                Text(
//                    text = author,
//                    modifier = Modifier
//                        .padding(start = 12.dp, end = 8.dp)
//                        .placeholder(isLoading = loadingEffect)
//                        .offset(y = (-4).dp),
//                    color = AppTheme.colors.onSurface,
//                    maxLines = 2,
//                    lineHeight = 20.sp,
//                    fontStyle = AppTheme.typography.body2.fontStyle,
//                    fontSize = 14.sp,
//                )
//
//                Text(
//                    text = language,
//                    modifier = Modifier
//                        .padding(start = 12.dp, end = 8.dp)
//                        .placeholder(isLoading = loadingEffect),
//                    color = AppTheme.colors.onSurface,
//                    fontSize = 15.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    fontStyle = AppTheme.typography.body2.fontStyle,
//
//                    )
//
//                Text(
//                    text = subjects,
//                    modifier = Modifier
//                        .padding(start = 12.dp, end = 8.dp, bottom = 2.dp)
//                        .placeholder(isLoading = loadingEffect),
//                    color = AppTheme.colors.onSurface,
//                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis,
//                    fontStyle = AppTheme.typography.body2.fontStyle,
//                    fontSize = 13.sp,
//                    lineHeight = 18.sp
//                )
//
//                Spacer(modifier = Modifier.weight(1f))
//            }
//        }
//    }
//}

@Composable
fun BookItemShimmerLoader() {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .padding(start = 8.dp, end = 8.dp),
            columns = GridCells.Adaptive(295.dp)
        ) {
            items(16) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BookItemCard(
                        title = "Crime and Punishment",
                        author = "Fyodor Dostoyevsky",
                        language = "English",
                        subjects = "Crime, Psychological aspects, Fiction",
                        coverImageUrl = "No",
                        loadingEffect = true,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@ExperimentalCoilApi
@ExperimentalMaterial3Api
@Preview
@Composable
fun BookCardPreview() {
    BookItemCard(
        title = "Crime and Punishment",
        author = "Fyodor Dostoyevsky",
        language = "English",
        subjects = "Crime, Psychological aspects, Fiction",
        coverImageUrl = "https://www.gutenberg.org/cache/epub/2554/pg2554.cover.medium.jpg",
        onClick = {}
    )
}
