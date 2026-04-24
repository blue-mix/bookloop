package com.starry.myne.ui.screens.reader.main.composables


import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.starry.myne.ui.screens.reader.main.dictionary.DictionaryUiState
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderScreenState
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderViewModel
import custom.AppTheme
import custom.components.Icon
import custom.components.IconButton
import custom.components.IconButtonVariant
import custom.components.Surface
import custom.components.Text
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onScrollToChapter: suspend (Int) -> Unit,
    chaptersContent: @Composable (paddingValues: PaddingValues) -> Unit
) {
    val state = viewModel.state.collectAsState().value
    val dictionaryState = viewModel.dictionary.collectAsState().value  // ← from VM
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Hide reader menu on back press.
    BackHandler(state.showReaderMenu) { viewModel.hideReaderInfo() }

    val showFontDialog = remember { mutableStateOf(false) }
    ReaderFontChooserDialog(
        showFontDialog = showFontDialog,
        fontFamily = state.fontFamily,
        onFontFamilyChanged = { viewModel.setFontFamily(it) }
    )

    val snackBarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BackHandler(drawerState.isOpen) {
        if (drawerState.isOpen) coroutineScope.launch { drawerState.close() }
    }

    ChaptersDrawer(
        drawerState = drawerState,
        chapters = state.chapters,
        currentChapterIndex = state.currentChapterIndex,
        onScrollToChapter = { idx ->
            coroutineScope.launch {
                // stop so the highlight & audio don't keep running in old chapter
                viewModel.ttsStop()
                drawerState.close()
                onScrollToChapter(idx) // your suspend scroller
            }
        },
    ) {
        Scaffold(
            containerColor = AppTheme.colors.background,
            contentColor = AppTheme.colors.onBackground,
            snackbarHost = { SnackbarHost(snackBarHostState) },
            topBar = {
                ReaderTopAppBar(
                    state = state,
                    onChapterListClicked = { coroutineScope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = state.showReaderMenu,
                    enter = expandVertically(initialHeight = { 0 }) + fadeIn(),
                    exit = shrinkVertically(targetHeight = { 0 }) + fadeOut(),
                ) {

                    ReaderBottomArea(
                        state = state,
                        viewModel = viewModel,
                        snackBarHostState = snackBarHostState,
                        showFontDialog = showFontDialog,
                        onJumpToTtsLocation = { idx -> coroutineScope.launch { onScrollToChapter(idx) } }
                    )
                }
            },
            content = { paddingValues ->
                Crossfade(
                    targetState = state.isLoading,
                    label = "reader content loading cross fade"
                ) { loading ->
                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 65.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.shouldShowLoader) {
                                CircularProgressIndicator(color = AppTheme.colors.primary)
                            }
                        }
                    } else {
                        chaptersContent(paddingValues)
                    }
                }
            }
        )
    }

    when (val s = dictionaryState) {
        is DictionaryUiState.Idle -> Unit
        is DictionaryUiState.Loading,
        is DictionaryUiState.Success,
        is DictionaryUiState.Error -> {
            ModalBottomSheet(
                containerColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.onBackground,
                onDismissRequest = { viewModel.clearDictionary() },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                sheetState = sheetState
            ) {
                DictionarySheet(
                    state = s,
                    onSpeak = viewModel::ttsSpeakText,
                    onClose = viewModel::clearDictionary
                )
            }
        }
    }
}

// ---- Top App Bar (unchanged visuals) ----------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopAppBar(
    state: ReaderScreenState,
    onChapterListClicked: () -> Unit,
) {
    AnimatedVisibility(
        visible = state.showReaderMenu,
        enter = expandVertically(initialHeight = { 0 }, expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(targetHeight = { 0 }, shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        Surface(color = AppTheme.colors.surface) {
            Column(modifier = Modifier.displayCutoutPadding()) {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = AppTheme.colors.surface,
                    ),
                    title = {
                        Text(
                            text = state.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.animateContentSize(),
                            color = AppTheme.colors.onBackground,
                        )
                    },
                    actions = {
                        IconButton(
                            variant = IconButtonVariant.Ghost,
                            onClick = onChapterListClicked
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = null,
                                tint = AppTheme.colors.onBackground
                            )
                        }
                    }
                )
                Column(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = state.currentChapter.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = AppTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                val chapterProgressbarState = animateFloatAsState(
                    targetValue = state.chapterScrollPercent,
                    label = "chapter progress bar state animation"
                )
                LinearProgressIndicator(
                    progress = { chapterProgressbarState.value },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTheme.colors.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionarySheet(
    state: DictionaryUiState,
    onSpeak: (String) -> Unit,
    onClose: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        when (state) {
            is DictionaryUiState.Loading -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text(
                    "Looking up…",
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.primary
                )
            }

            is DictionaryUiState.Error -> {
                Text(
                    "No result",
                    style = AppTheme.typography.h2,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    state.message ?: "Lookup failed",
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.error
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }

            is DictionaryUiState.Success -> {
                val e = state.entry

                // Word + Speak button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            e.word,
                            style = AppTheme.typography.h4,
                            fontWeight = FontWeight.Bold
                        )
                        e.phonetics.firstOrNull { !it.text.isNullOrBlank() }?.text?.let {
                            Text(
                                it,
                                style = AppTheme.typography.body2,
                                color = AppTheme.colors.primary
                            )
                        }
                    }
                    IconButton(onClick = { onSpeak(e.word) }) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speak"
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Meanings
                e.meanings.forEach { meaning ->
                    Text(
                        meaning.partOfSpeech.toString(),
                        style = AppTheme.typography.label1,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    meaning.definitions.take(2).forEach { def ->
                        Text("• ${def.definition}", style = AppTheme.typography.body2)
                        def.example?.let { ex ->
                            Text(
                                "Example: $ex",
                                style = AppTheme.typography.body3,
                                color = AppTheme.colors.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }

            DictionaryUiState.Idle -> Unit
        }
    }

}


