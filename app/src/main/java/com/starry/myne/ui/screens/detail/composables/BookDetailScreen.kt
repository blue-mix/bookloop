package com.starry.myne.ui.screens.detail.composables

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionResult
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.starry.myne.MainActivity
import com.starry.myne.R
import com.starry.myne.helpers.Utils
import com.starry.myne.helpers.book.BookUtils
import com.starry.myne.helpers.getActivity
import com.starry.myne.helpers.weakHapticFeedback
import com.starry.myne.ui.common.BookDetailTopUI
import com.starry.myne.ui.common.NetworkError
import com.starry.myne.ui.common.ProgressDots
import com.starry.myne.ui.screens.detail.viewmodels.BookDetailViewModel
import custom.AppTheme
import custom.components.Icon
import custom.components.Text
import custom.components.VerticalDivider
import custom.components.card.CardDefaults
import custom.components.card.OutlinedCard
import custom.components.progressindicators.LinearProgressIndicator
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import custom.components.HorizontalDivider

@Composable
fun BookDetailScreen(
    bookId: String, navController: NavController
) {
    val context = LocalContext.current
    val viewModel: BookDetailViewModel = hiltViewModel()
    val state = viewModel.state

    val snackBarHostState = remember { SnackbarHostState() }
    Scaffold(
        containerColor = AppTheme.colors.background,
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        snackbarHost = { SnackbarHost(snackBarHostState) },
        content = { paddingValues ->
            LaunchedEffect(key1 = true, block = {
                if (state.isLoading) viewModel.getBookDetails(bookId)
            })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(AppTheme.colors.background)
            ) {
                BookDetailTopBar(onBackClicked = {
                    navController.navigateUp()
                }, onShareClicked = {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(
                        Intent.EXTRA_TEXT, "https://www.gutenberg.org/ebooks/$bookId"
                    )
                    val chooser = Intent.createChooser(
                        intent, context.getString(R.string.share_intent_header)
                    )
                    context.startActivity(chooser)
                })

                Crossfade(
                    targetState = state.isLoading,
                    label = "BookDetailLoadingCrossFade"
                ) { isLoading ->
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 65.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ProgressDots()
                        }
                    } else {
                        if (state.error != null) {
                            NetworkError(onRetryClicked = {
                                viewModel.getBookDetails(bookId)
                            })
                        } else {
                            BookDetailContents(
                                viewModel = viewModel,
                                navController = navController,
                                snackBarHostState = snackBarHostState
                            )
                        }
                    }

                }
            }
        })
}

@Composable
private fun BookDetailContents(
    viewModel: BookDetailViewModel,
    navController: NavController,
    snackBarHostState: SnackbarHostState
) {
    val view = LocalView.current
    val context = LocalContext.current
    val settingsVM = (context.getActivity() as MainActivity).settingsViewModel

    val state = viewModel.state
    val coroutineScope = rememberCoroutineScope()
    // Get book details for this bookId.
    val book = remember { state.bookSet.books.first() }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        val authors = remember { BookUtils.getAuthorsAsString(book.authors) }

        BookDetailTopUI(
            title = book.title,
            authors = authors,
            imageData = state.extraInfo.coverImage.ifEmpty { book.formats.imagejpeg },
            currentThemeMode = settingsVM.getCurrentTheme()
        )

        val pageCount = remember {
            if (state.extraInfo.pageCount > 0) {
                state.extraInfo.pageCount.toString()
            } else {
                context.getString(R.string.not_applicable)
            }
        }
        var buttonText by remember { mutableStateOf("") }

        // Update button text based on download status.
        LaunchedEffect(key1 = true) {
            buttonText = if (viewModel.bookDownloader.isBookCurrentlyDownloading(book.id)) {
                context.getString(R.string.cancel)
            } else {
                when (state.bookLibraryItem) {
                    null -> context.getString(R.string.download_book_button)
                    else -> context.getString(R.string.read_book_button)
                }
            }
        }

        var progressState by remember { mutableFloatStateOf(0f) }
        var showProgressBar by remember { mutableStateOf(false) }

        // Callable which updates book details screen button.
        val updateBtnText: (Int?) -> Unit = { downloadStatus ->
            buttonText = when (downloadStatus) {
                DownloadManager.STATUS_RUNNING -> {
                    showProgressBar = true
                    context.getString(R.string.cancel)
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    showProgressBar = false
                    context.getString(R.string.read_book_button)
                }

                else -> {
                    showProgressBar = false
                    context.getString(R.string.download_book_button)
                }
            }
        }

        // Check if this book is in downloadQueue.
        if (viewModel.bookDownloader.isBookCurrentlyDownloading(book.id)) {
            progressState =
                viewModel.bookDownloader.getRunningDownload(book.id)?.progress?.collectAsState()?.value!!
            LaunchedEffect(key1 = progressState, block = {
                updateBtnText(viewModel.bookDownloader.getRunningDownload(book.id)?.status)
            })
        }

        MiddleBar(
            bookLang = BookUtils.getLanguagesAsString(book.languages),
            pageCount = pageCount,
            downloadCount = Utils.prettyCount(book.downloadCount),
            progressValue = progressState,
            buttonText = buttonText,
            showProgressBar = showProgressBar
        ) {
            when (buttonText) {
                context.getString(R.string.read_book_button) -> {
                    /**
                     *  Library item could be null if we reload the screen
                     *  while some download was running, in that case we'll
                     *  de-attach from our old state where download function
                     *  will update library item and our new state will have
                     *  no library item, i.e. null.
                     */
                    if (state.bookLibraryItem == null) {
                        viewModel.reFetchLibraryItem(
                            bookId = book.id,
                            onComplete = { libraryItem ->
                                BookUtils.openBookFile(
                                    context = context,
                                    internalReader = viewModel.getInternalReaderSetting(),
                                    libraryItem = libraryItem,
                                    navController = navController
                                )
                            }
                        )
                    } else {
                        BookUtils.openBookFile(
                            context = context,
                            internalReader = viewModel.getInternalReaderSetting(),
                            libraryItem = state.bookLibraryItem,
                            navController = navController
                        )
                    }
                }

                context.getString(R.string.download_book_button) -> {
                    view.weakHapticFeedback()
                    viewModel.downloadBook(
                        book = book,
                        downloadProgressListener = { downloadProgress, downloadStatus ->
                            progressState = downloadProgress
                            updateBtnText(downloadStatus)
                        })
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.download_started),
                        )
                    }
                }

                context.getString(R.string.cancel) -> {
                    viewModel.bookDownloader.cancelDownload(
                        viewModel.bookDownloader.getRunningDownload(book.id)?.downloadId
                    )
                }
            }
        }

        Text(
            text = stringResource(id = R.string.book_synopsis),
            modifier = Modifier.padding(start = 13.dp, end = 8.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.onBackground,
        )


        state.bookSummary?.let { summary ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                ) {
                    // Header
                    Text(
                        text = "📖 AI Summary",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.onSurface,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Key Points Section
                    Text(
                        text = "Key Points",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.onSurface,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.onSurface,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = summary.final_summary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp,
                        color = AppTheme.colors.onSurface.copy(alpha = 0.95f),
                    )


                    Spacer(modifier = Modifier.height(18.dp))

                    // Divider for clarity
                   HorizontalDivider(
                        thickness = 1.dp,
                        color = AppTheme.colors.onSurface.copy(alpha = 0.08f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        summary.summary_points.forEach { point ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 14.sp,
                                    color = AppTheme.colors.primary,
                                    modifier = Modifier.padding(end = 6.dp, top = 2.dp)
                                )
                                Text(
                                    text = point,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 20.sp,
                                    color = AppTheme.colors.onSurface.copy(alpha = 0.9f),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Read Time Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .background(
                                color = AppTheme.colors.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⏱ ~${summary.read_time_hours} hrs",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = AppTheme.colors.primary,
                        )
                    }
                }
            }
        }

        val synopsis = state.extraInfo.description.ifEmpty { null }
        if (synopsis != null) {
            Text(
                text = synopsis,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontWeight = FontWeight.Normal,
                color = AppTheme.colors.onBackground,
            )
        } else {
            NoSynopsisUI()
        }

// 👇 Buy links section
        val summary = state.bookSummary
        if (summary?.buy_links?.amazon != null || summary?.buy_links?.goodreads != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = AppTheme.colors.onBackground.copy(alpha = 0.2f)
                )

                Text(
                    text = "Explore More",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.colors.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                summary.buy_links.amazon.let { amazonUrl ->
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, amazonUrl.toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Amazon",
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text("Buy on Amazon")
                    }
                }

                summary.buy_links.goodreads.let { goodreadsUrl ->
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, goodreadsUrl.toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Goodreads",
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text("View on Goodreads")
                    }
                }
            }
        }

    }
}

@Composable
private fun MiddleBar(
    bookLang: String,
    pageCount: String,
    downloadCount: String,
    progressValue: Float,
    buttonText: String,
    showProgressBar: Boolean,
    onButtonClick: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = progressValue,
        label = "download progress bar"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = showProgressBar) {
            if (progressValue > 0f) {
                // Determinate progress bar.
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(start = 14.dp, end = 14.dp, top = 6.dp)
                        .clip(RoundedCornerShape(40.dp)),
                    color = AppTheme.colors.background,
                )
            } else {
                // Indeterminate progress bar.
                LinearProgressIndicator(
                    color = AppTheme.colors.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(start = 14.dp, end = 14.dp, top = 6.dp)
                        .clip(RoundedCornerShape(40.dp))
                )
            }
        }

        OutlinedCard(
            modifier = Modifier
                .height(90.dp)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppTheme.colors.background

            )
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_book_language),
                            contentDescription = null,
                            tint = AppTheme.colors.onBackground,
                            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, end = 4.dp)
                        )
                        Text(
                            text = bookLang,
                            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 4.dp),
                            fontSize = 16.sp,

                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.onBackground,
                        )
                    }

                }
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .width(2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_book_pages),
                            contentDescription = null,
                            tint = AppTheme.colors.onBackground,
                            modifier = Modifier.padding(top = 13.dp, bottom = 15.dp, end = 4.dp)
                        )
                        Text(
                            text = pageCount,
                            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 4.dp),
                            fontSize = 16.sp,

                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.onBackground,
                        )
                    }
                }
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .width(2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_book_downloads),
                            contentDescription = null,
                            tint = AppTheme.colors.onBackground,
                            modifier = Modifier.padding(top = 15.dp, bottom = 13.dp, end = 4.dp)
                        )
                        Text(
                            text = downloadCount,
                            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 4.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.onBackground
                        )
                    }
                }
            }
        }

        OutlinedCard(
            onClick = { onButtonClick() },
            modifier = Modifier
                .height(75.dp)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppTheme.colors.background
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = buttonText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.onBackground,
                )
            }
        }
    }
}

@Composable
private fun BookDetailTopBar(
    onBackClicked: () -> Unit, onShareClicked: () -> Unit
) {
    val view = LocalView.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.background)
                .clickable {
                    view.weakHapticFeedback()
                    onBackClicked()
                }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(id = R.string.back_button_desc),
                tint = AppTheme.colors.onBackground,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(id = R.string.book_detail_header),
            modifier = Modifier.padding(bottom = 2.dp),
            color = AppTheme.colors.onBackground,
            fontStyle = AppTheme.typography.h2.fontStyle,
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .padding(22.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.background)
                .clickable {
                    view.weakHapticFeedback()
                    onShareClicked()
                }) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(id = R.string.back_button_desc),
                tint = AppTheme.colors.onBackground,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun NoSynopsisUI() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val compositionResult: LottieCompositionResult =
            rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(R.raw.synopis_not_found_lottie)
            )
        val progressAnimation by animateLottieCompositionAsState(
            compositionResult.value,
            isPlaying = true,
            iterations = LottieConstants.IterateForever,
            speed = 1f
        )

        Spacer(modifier = Modifier.weight(2f))
        LottieAnimation(
            composition = compositionResult.value,
            progress = { progressAnimation },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(200.dp),
            enableMergePaths = true
        )

        Text(
            text = stringResource(id = R.string.book_synopsis_not_found),
            modifier = Modifier.padding(14.dp),
            fontWeight = FontWeight.Medium,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
@Preview(showBackground = true)
fun BookDetailScreenPreview() {
    BookDetailScreen(
        bookId = "0",
        navController = rememberNavController(),
    )
}