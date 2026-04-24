package com.starry.myne.ui.screens.reader.detail

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.annotation.ExperimentalCoilApi
import com.starry.myne.MainActivity
import com.starry.myne.R
import com.starry.myne.database.progress.ProgressData
import com.starry.myne.helpers.NetworkObserver
import com.starry.myne.helpers.getActivity
import com.starry.myne.helpers.weakHapticFeedback
import com.starry.myne.ui.common.BookDetailTopUI
import com.starry.myne.ui.common.CustomTopAppBar
import com.starry.myne.ui.common.ProgressDots
import com.starry.myne.ui.common.simpleVerticalScrollbar
import com.starry.myne.ui.screens.reader.main.activities.ReaderActivity
import com.starry.myne.ui.screens.reader.main.activities.ReaderConstants
import custom.AppTheme
import custom.components.Icon
import custom.components.Text
import custom.components.card.CardDefaults
import custom.components.card.OutlinedCard

@Composable
fun ReaderDetailScreen(
    libraryItemId: String,
    navController: NavController,
    networkStatus: NetworkObserver.Status
) {
    LocalContext.current
    val viewModel: ReaderDetailViewModel = hiltViewModel()
    val state = viewModel.state

    LaunchedEffect(true) {
        viewModel.loadEbookData(libraryItemId, networkStatus)
    }

    Crossfade(targetState = state.isLoading, label = "ReaderDetailCrossFade") { isLoading ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ProgressDots()
                        Text(
                            text = "Loading Book...",
                            color = AppTheme.colors.onBackground,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            state.error != null -> {
                ErrorState(
                    message = stringResource(id = R.string.error),
                    onRetry = {
                        viewModel.loadEbookData(libraryItemId, networkStatus)
                    },
                    onBack = { navController.navigateUp() }
                )
            }

            else -> {
                val readerData =
                    viewModel.progressData?.collectAsState(initial = null)?.value
                ReaderDetailScaffold(
                    libraryItemId = libraryItemId,
                    progressData = readerData,
                    state = state,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun ReaderDetailScaffold(
    libraryItemId: String,
    progressData: ProgressData?,
    state: ReaderDetailScreenState,
    navController: NavController
) {
    val context = LocalContext.current
    val settingsVM = (context.getActivity() as MainActivity).settingsViewModel

    Scaffold(
        topBar = {
            CustomTopAppBar(
                headerText = stringResource(id = R.string.reader_detail_header)
            ) { navController.navigateUp() }
        },
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(visible = true) {
                ExtendedFloatingActionButton(
                    text = {
                        Text(
                            text = stringResource(
                                id = if (progressData != null)
                                    R.string.continue_reading_button
                                else R.string.start_reading_button
                            )
                        )
                    },
                    onClick = {
                        val intent = Intent(context, ReaderActivity::class.java).apply {
                            putExtra(ReaderConstants.EXTRA_LIBRARY_ITEM_ID, libraryItemId.toInt())
                        }
                        context.startActivity(intent)
                    },
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_reader_fab_button),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.padding(end = 10.dp, bottom = 8.dp),
                    containerColor = AppTheme.colors.background,
                    contentColor = AppTheme.colors.onBackground
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .padding(paddingValues)
        ) {
            BookDetailTopUI(
                title = state.title,
                authors = state.authors,
                imageData = state.coverImage,
                currentThemeMode = settingsVM.getCurrentTheme(),
                showReaderBackground = true,
                progressPercent = if (state.hasProgressSaved)
                    progressData?.getProgressPercent(state.chapters.size) else ""
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                thickness = 1.dp,
                color = AppTheme.colors.surface
            )

            Text(
                text = "Chapters",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AppTheme.colors.onBackground,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
            )

            val lazyListState = rememberLazyListState()
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.simpleVerticalScrollbar(
                    lazyListState, color = AppTheme.colors.primary
                )
            ) {
                items(
                    count = state.chapters.size,
                    key = { idx -> state.chapters[idx].title.hashCode() }
                ) { idx ->
                    ChapterItem(
                        number = idx + 1,
                        chapterTitle = state.chapters[idx].title,
                        onClick = {
                            val intent = Intent(context, ReaderActivity::class.java).apply {
                                putExtra(
                                    ReaderConstants.EXTRA_LIBRARY_ITEM_ID,
                                    libraryItemId.toInt()
                                )
                                putExtra(ReaderConstants.EXTRA_CHAPTER_IDX, idx)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterItem(number: Int, chapterTitle: String, onClick: () -> Unit) {
    val view = LocalView.current
    OutlinedCard(
        onClick = {
            view.weakHapticFeedback()
            onClick()
        },
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp)
        ) {
            Text(
                text = "$number.",
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                modifier = Modifier.weight(1f),
                text = chapterTitle,
                color = AppTheme.colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_right_arrow),
                contentDescription = null,
                tint = AppTheme.colors.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = AppTheme.colors.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row {
                ExtendedFloatingActionButton(
                    text = { Text("Retry") },
                    onClick = onRetry,
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) }
                )
                ExtendedFloatingActionButton(
                    text = { Text("Back") },
                    onClick = onBack,
                    icon = { Icon(Icons.Filled.ArrowBack, contentDescription = null) },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

//
//@Composable
//fun ReaderDetailScreen(
//    libraryItemId: String, navController: NavController, networkStatus: NetworkObserver.Status
//) {
//    val context = LocalContext.current
//    val viewModel: ReaderDetailViewModel = hiltViewModel()
//    val state = viewModel.state
//
//    LaunchedEffect(key1 = true) { viewModel.loadEbookData(libraryItemId, networkStatus) }
//
//    Crossfade(targetState = state.isLoading, label = "ReaderDetailLoadingCrossFade") { isLoading ->
//        if (isLoading) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(bottom = 65.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                ProgressDots()
//            }
//        } else {
//            if (state.error != null) {
//                stringResource(id = R.string.error).toToast(context)
//                navController.navigateUp()
//            } else {
//                // Collect saved reader progress for the current book.
//                val readerData = viewModel.progressData?.collectAsState(initial = null)?.value
//                ReaderDetailScaffold(
//                    libraryItemId = libraryItemId,
//                    progressData = readerData,
//                    state = state,
//                    navController = navController
//                )
//            }
//
//        }
//    }
//}
//
//@Composable
//private fun ReaderDetailScaffold(
//    libraryItemId: String,
//    progressData: ProgressData?,
//    state: ReaderDetailScreenState,
//    navController: NavController
//) {
//    val context = LocalContext.current
//    val settingsVM = (context.getActivity() as MainActivity).settingsViewModel
//
//    Scaffold(topBar = {
//        CustomTopAppBar(headerText = stringResource(id = R.string.reader_detail_header)) {
//            navController.navigateUp()
//        }
//    }, floatingActionButton = {
//        ExtendedFloatingActionButton(
//            text = { Text(text = stringResource(id = if (progressData != null) R.string.continue_reading_button else R.string.start_reading_button)) },
//            onClick = {
//                val intent = Intent(context, ReaderActivity::class.java)
//                intent.putExtra(
//                    ReaderConstants.EXTRA_LIBRARY_ITEM_ID, libraryItemId.toInt()
//                )
//                context.startActivity(intent)
//            },
//            icon = {
//                Icon(
//                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_reader_fab_button),
//                    contentDescription = null
//                )
//            },
//            modifier = Modifier.padding(end = 10.dp, bottom = 8.dp),
//            containerColor = AppTheme.colors.background, contentColor = AppTheme.colors.onBackground
//        )
//    }) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(AppTheme.colors.background)
//                .padding(it)
//        ) {
//            BookDetailTopUI(
//                title = state.title,
//                authors = state.authors,
//                imageData = state.coverImage,
//                currentThemeMode = settingsVM.getCurrentTheme(),
//                showReaderBackground = true,
//                progressPercent = if (state.hasProgressSaved)
//                    progressData?.getProgressPercent(state.chapters.size) else ""
//            )
//
//            HorizontalDivider(
//                modifier = Modifier.padding(
//                    start = 20.dp, end = 20.dp, top = 2.dp, bottom = 2.dp
//                ),
//                thickness = 2.dp,
//                color = AppTheme.colors.surface
//            )
//
//            val lazyListState = rememberLazyListState()
//            LazyColumn(
//                state = lazyListState, modifier = Modifier.simpleVerticalScrollbar(
//                    lazyListState, color = AppTheme.colors.primary
//                )
//            ) {
//                items(state.chapters.size) { idx ->
//                    val chapter = state.chapters[idx]
//                    ChapterItem(chapterTitle = chapter.title, onClick = {
//                        val intent = Intent(context, ReaderActivity::class.java)
//                        intent.putExtra(
//                            ReaderConstants.EXTRA_LIBRARY_ITEM_ID, libraryItemId.toInt()
//                        )
//                        intent.putExtra(ReaderConstants.EXTRA_CHAPTER_IDX, idx)
//                        context.startActivity(intent)
//                    })
//                }
//            }
//        }
//    }
//}
//
@Composable
private fun ChapterItem(chapterTitle: String, onClick: () -> Unit) {
    val view = LocalView.current
    OutlinedCard(
        onClick = {
            view.weakHapticFeedback()
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface
        ),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                modifier = Modifier
                    .weight(3f)
                    .padding(start = 12.dp),
                text = chapterTitle,
                color = AppTheme.colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Icon(
                modifier = Modifier
                    .size(15.dp)
                    .weight(0.4f),
                painter = painterResource(id = R.drawable.ic_right_arrow),
                contentDescription = null,
                tint = AppTheme.colors.onSurface
            )
        }
    }

}


@ExperimentalCoilApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun EpubDetailScreenPV() {
    ReaderDetailScreen("", rememberNavController(), NetworkObserver.Status.Available)
    //ReaderError(rememberNavController())
}