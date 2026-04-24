package com.starry.myne.ui.screens.library.composables

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.psoffritti.taptargetcompose.TapTargetCoordinator
import com.psoffritti.taptargetcompose.TapTargetStyle
import com.psoffritti.taptargetcompose.TextDefinition
import com.starry.myne.BuildConfig
import com.starry.myne.MainActivity
import com.starry.myne.R
import com.starry.myne.database.library.LibraryItem
import com.starry.myne.helpers.Constants
import com.starry.myne.helpers.book.BookUtils
import com.starry.myne.helpers.getActivity
import com.starry.myne.helpers.isScrollingUp
import com.starry.myne.helpers.weakHapticFeedback
import com.starry.myne.ui.common.CustomTopAppBar
import com.starry.myne.ui.common.NoBooksAvailable
import com.starry.myne.ui.navigation.Screens
import com.starry.myne.ui.screens.library.viewmodels.LibraryViewModel
import com.starry.myne.ui.screens.main.bottomNavPadding
import com.starry.myne.ui.screens.settings.viewmodels.SettingsViewModel
import com.starry.myne.ui.screens.settings.viewmodels.ThemeMode
import custom.AppTheme
import custom.components.Button
import custom.components.ButtonVariant
import custom.components.Icon
import custom.components.Text
import custom.components.VerticalDivider
import custom.components.card.CardDefaults
import custom.components.card.OutlinedCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavController) {
    val view = LocalView.current
    val context = LocalContext.current
    val viewModel: LibraryViewModel = hiltViewModel()

    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val showImportDialog = remember { mutableStateOf(false) }
    val importBookLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            // If no files are selected, return.
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            // Show dialog to indicate import process.
            showImportDialog.value = true
            // Start books import.
            viewModel.importBooks(
                context = context,
                fileUris = uris,
                onComplete = {
                    // Hide dialog and show success message.
                    showImportDialog.value = false
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.epub_imported),
                            actionLabel = context.getString(R.string.ok),
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                onError = {
                    // Hide dialog and show error message.
                    showImportDialog.value = false
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.error),
                            actionLabel = context.getString(R.string.ok),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }

    val showTapTargets = remember { mutableStateOf(false) }
    LaunchedEffect(key1 = viewModel.showOnboardingTapTargets.value) {
        delay(500) // Delay to prevent flickering
        showTapTargets.value = viewModel.showOnboardingTapTargets.value
    }

    TapTargetCoordinator(
        showTapTargets = showTapTargets.value,
        onComplete = { viewModel.onboardingComplete() }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackBarHostState) },
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .padding(bottom = bottomNavPadding),
            topBar = {
                CustomTopAppBar(
                    headerText = stringResource(id = R.string.library_header),
                    iconRes = R.drawable.ic_nav_library
                )
            },
            floatingActionButton = {
                val density = LocalDensity.current
                AnimatedVisibility(
                    visible = !showImportDialog.value && lazyListState.isScrollingUp(),
                    enter = slideInVertically {
                        with(density) { 40.dp.roundToPx() }
                    } + fadeIn(),
                    exit = fadeOut(
                        animationSpec = keyframes {
                            this.durationMillis = 120
                        }
                    )
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            view.weakHapticFeedback()
                            importBookLauncher.launch(arrayOf(Constants.EPUB_MIME_TYPE))
                        },
                        modifier = Modifier.tapTarget(
                            precedence = 0,
                            title = TextDefinition(
                                text = stringResource(id = R.string.import_button_onboarding),
                                textStyle = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.onSecondary,
                            ),
                            description = TextDefinition(
                                text = stringResource(id = R.string.import_button_onboarding_desc),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppTheme.colors.onSecondary,
                            ),
                            tapTargetStyle = TapTargetStyle(
                                backgroundColor = AppTheme.colors.secondary,
                                tapTargetHighlightColor = AppTheme.colors.onSecondary,
                                backgroundAlpha = 1f,
                            ),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(id = R.string.import_button_desc),
                            tint = AppTheme.colors.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.import_button_text),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = AppTheme.colors.onPrimary
                        )

                    }
                }
            }
        ) { paddingValues ->
            LibraryContents(
                viewModel = viewModel,
                lazyListState = lazyListState,
                snackBarHostState = snackBarHostState,
                navController = navController,
                paddingValues = paddingValues
            )

            if (showImportDialog.value) {
                BasicAlertDialog(onDismissRequest = {}) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth(),
                        //  .padding(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppTheme.colors.background
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                text = stringResource(id = R.string.epub_importing),
                                fontWeight = FontWeight.Medium,
                                fontSize = 17.sp,
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun LibraryContents(
    viewModel: LibraryViewModel,
    lazyListState: LazyListState,
    snackBarHostState: SnackbarHostState,
    navController: NavController,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val settingsVm = (context.getActivity() as MainActivity).settingsViewModel
    val libraryItems = viewModel.allItems.observeAsState(listOf()).value

    // Show tooltip for library screen.
    LaunchedEffect(key1 = true) {
        if (viewModel.shouldShowLibraryTooltip()) {
            val result = snackBarHostState.showSnackbar(
                message = context.getString(R.string.library_tooltip),
                actionLabel = context.getString(R.string.got_it),
                duration = SnackbarDuration.Indefinite
            )

            when (result) {
                SnackbarResult.ActionPerformed -> {
                    viewModel.libraryTooltipDismissed()
                }

                SnackbarResult.Dismissed -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(paddingValues)
    ) {
        if (libraryItems.isEmpty()) {
            NoBooksAvailable(text = stringResource(id = R.string.empty_library))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background),
                state = lazyListState
            ) {
                items(
                    count = libraryItems.size,
                    key = { i -> libraryItems[i].id }
                ) { i ->
                    val item = libraryItems[i]
                    if (item.fileExist()) {
                        LibraryLazyItem(
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                            item = item,
                            snackBarHostState = snackBarHostState,
                            navController = navController,
                            viewModel = viewModel,
                            settingsVm = settingsVm
                        )
                    } else {
                        viewModel.deleteItemFromDB(item)
                    }
                }
            }

        }
    }
}

@Composable
private fun LibraryLazyItem(
    modifier: Modifier,
    item: LibraryItem,
    snackBarHostState: SnackbarHostState,
    navController: NavController,
    viewModel: LibraryViewModel,
    settingsVm: SettingsViewModel
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val openDeleteDialog = remember { mutableStateOf(false) }

    // Swipe actions to show book details.
    val detailsAction = SwipeAction(
        icon = painterResource(
            id = if (settingsVm.getCurrentTheme() == ThemeMode.Dark) R.drawable.ic_info else R.drawable.ic_info_white
        ), background = AppTheme.colors.primary, onSwipe = {
            viewModel.viewModelScope.launch {
                delay(250L)
                if (item.isExternalBook) {
                    snackBarHostState.showSnackbar(
                        message = context.getString(R.string.external_book_info_unavailable),
                        actionLabel = context.getString(R.string.ok),
                        duration = SnackbarDuration.Short
                    )
                } else {
                    navController.navigate(
                        Screens.BookDetailScreen.withBookId(
                            item.bookId.toString()
                        )
                    )
                }
            }
        })

    // Swipe actions to share book.
    val shareAction = SwipeAction(
        icon = painterResource(
            id = if (settingsVm.getCurrentTheme() == ThemeMode.Dark) R.drawable.ic_share else R.drawable.ic_share_white
        ), background = AppTheme.colors.primary, onSwipe = {
            val uri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider",
                File(item.filePath)
            )
            val intent = Intent(Intent.ACTION_SEND)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.type = context.contentResolver.getType(uri)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.getString(R.string.share_app_chooser)
                )
            )
        })

    SwipeableActionsBox(
        modifier = modifier.padding(vertical = 4.dp),
        startActions = listOf(shareAction),
        endActions = listOf(detailsAction),
        swipeThreshold = 85.dp
    ) {
        LibraryCard(
            title = item.title,
            author = item.authors,
            item.getFileSize(),
            item.getDownloadDate(),
            isExternalBook = item.isExternalBook,
            onReadClick = {
                BookUtils.openBookFile(
                    context = context,
                    internalReader = viewModel.getInternalReaderSetting(),
                    libraryItem = item,
                    navController = navController
                )
            },
            onDeleteClick = { openDeleteDialog.value = true })
    }

    if (openDeleteDialog.value) {
        AlertDialog(onDismissRequest = {
            openDeleteDialog.value = false
        }, title = {
            Text(
                text = stringResource(id = R.string.library_delete_dialog_title),
                color = AppTheme.colors.onSurface,
            )
        }, confirmButton = {
            Button(
                variant = ButtonVariant.PrimaryOutlined,
                onClick = {
                    openDeleteDialog.value = false
                    val fileDeleted = item.deleteFile()
                    if (fileDeleted) {
                        viewModel.deleteItemFromDB(item)
                    } else {
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(
                                message = context.getString(R.string.error),
                                actionLabel = context.getString(R.string.ok),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },

                ) {
                Text(stringResource(id = R.string.confirm))
            }
        }, dismissButton = {
            TextButton(onClick = {
                openDeleteDialog.value = false
            }) {
                Text(stringResource(id = R.string.cancel))
            }
        })
    }
}

@Composable
fun LibraryCard(
    title: String,
    author: String,
    fileSize: String,
    date: String,
    isExternalBook: Boolean,
    onReadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.background
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.primary.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        id = if (isExternalBook) R.drawable.ic_library_external_item
                        else R.drawable.ic_library_item
                    ),
                    contentDescription = stringResource(id = R.string.back_button_desc),
                    tint = AppTheme.colors.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = AppTheme.colors.onSurface.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = fileSize,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppTheme.colors.onSurface.copy(alpha = 0.8f)
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    VerticalDivider(
                        modifier = Modifier
                            .height(14.dp)
                            .width(1.dp)
                            .clip(CircleShape),
                        color = AppTheme.colors.onSurface.copy(alpha = 0.5f),
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppTheme.colors.onSurface.copy(alpha = 0.8f)
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        text = stringResource(id = R.string.library_read_button),
                        variant = ButtonVariant.PrimaryElevated,
                        onClick = onReadClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
//                        Icon(
//                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_library_read),
//                            contentDescription = null,
//                            modifier = Modifier.size(18.dp)
//                        )
//                        Spacer(Modifier.width(6.dp))

                    }

                    Button(
                        text = stringResource(id = R.string.library_delete_button),
                        onClick = onDeleteClick,
                        variant = ButtonVariant.DestructiveElevated,

                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
//                        Icon(
//                            imageVector = Icons.Filled.Delete,
//                            contentDescription = null,
//                            modifier = Modifier.size(18.dp)
//                        )
//                        Spacer(Modifier.width(6.dp))

                    }
                }
            }
        }
    }
}


//@Composable
//private fun LibraryCard(
//    title: String,
//    author: String,
//    fileSize: String,
//    date: String,
//    isExternalBook: Boolean,
//    onReadClick: () -> Unit,
//    onDeleteClick: () -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
//            containerColor = AppTheme.colors.surface
//        ), shape = RoundedCornerShape(0.dp)
//    ) {
//        Row(
//            modifier = Modifier.padding(start = 12.dp, end = 12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Box(
//                modifier = Modifier
//                    .height(90.dp)
//                    .width(90.dp)
//                    .padding(10.dp)
//                    .clip(CircleShape)
//                    .background(AppTheme.colors.primary),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = ImageVector.vectorResource(
//                        id = if (isExternalBook) R.drawable.ic_library_external_item
//                        else R.drawable.ic_library_item
//                    ),
//                    contentDescription = stringResource(id = R.string.back_button_desc),
//                    tint =AppTheme.colors.onPrimary,
//                    modifier = Modifier.size(32.dp)
//                )
//            }
//
//            Column(modifier = Modifier.padding(8.dp)) {
//                Text(
//                    text = title,
//                    fontStyle = MaterialTheme.typography.headlineMedium.fontStyle,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis,
//                    color = AppTheme.colors.onSurface,
//                )
//
//                Text(
//                    text = author,
//                    color = AppTheme.colors.onSurface.copy(alpha = 0.85f),
//                    maxLines = 1,
//                    fontStyle = MaterialTheme.typography.bodySmall.fontStyle,
//                    fontWeight = FontWeight.Medium,
//                    fontSize = 16.sp,
//                    overflow = TextOverflow.Ellipsis,
//                    modifier = Modifier.offset(y = (-8).dp)
//                )
//
//                Row(modifier = Modifier.offset(y = (-8).dp)) {
//                    Text(
//                        text = fileSize,
//                        color = AppTheme.colors.onSurface,
//                        fontWeight = FontWeight.Light,
//                        fontSize = 14.sp,
//                        modifier = Modifier.padding(end = 6.dp)
//                    )
//                    VerticalDivider(
//                        modifier = Modifier
//                            .height(17.5.dp)
//                            .width(1.dp)
//                            .clip(CircleShape),
//                        color = AppTheme.colors.onSurface,
//                    )
//                    Text(
//                        text = date,
//                        color = AppTheme.colors.onSurface,
//                        fontWeight = FontWeight.Light,
//                        fontSize = 14.sp,
//                        modifier = Modifier.padding(start = 6.dp)
//                    )
//                }
//
//                Row(modifier = Modifier.offset(y = (-4).dp)) {
//                    LibraryCardButton(
//                        text = stringResource(id = R.string.library_read_button),
//                        icon = ImageVector.vectorResource(id = R.drawable.ic_library_read),
//                        onClick = { onReadClick() })
//
//                    Spacer(modifier = Modifier.width(10.dp))
//
//                    LibraryCardButton(
//                        text = stringResource(id = R.string.library_delete_button),
//                        icon = Icons.Outlined.Delete,
//                        onClick = { onDeleteClick() })
//                }
//                Spacer(modifier = Modifier.height(2.dp))
//            }
//        }
//    }
//}

@Composable
private fun LibraryCardButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = AppTheme.colors.onSurface,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Favorite Icon",
                tint = AppTheme.colors.onSurface,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = AppTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@ExperimentalMaterial3Api
@Composable
@Preview
fun LibraryScreenPreview() {
    LibraryCard(
        title = "The Idiot",
        author = "Fyodor Dostoevsky",
        fileSize = "5.9MB",
        date = "01- Jan -2020",
        isExternalBook = false,
        onReadClick = {},
        onDeleteClick = {})
}
