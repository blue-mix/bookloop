package com.starry.myne.ui.screens.reader.main.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starry.myne.R
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderFont
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderScreenState
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderViewModel
import com.starry.myne.ui.theme.poppinsFont
import custom.AppTheme
import custom.components.Button
import custom.components.ButtonVariant
import custom.components.Icon
import custom.components.IconButton
import custom.components.IconButtonVariant
import custom.components.Text
import kotlinx.coroutines.launch

private enum class TextScaleButtonType { INCREASE, DECREASE }

enum class ReaderSheetContent {
    NONE, FONTS, AUDIO, PROGRESS, HIGHLIGHTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderBottomArea(
    state: ReaderScreenState,
    viewModel: ReaderViewModel,
    snackBarHostState: SnackbarHostState,
    showFontDialog: MutableState<Boolean>,
    onJumpToTtsLocation: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentSheet by rememberSaveable { mutableStateOf(ReaderSheetContent.NONE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = ReaderDimens.radiusLg,
                    topEnd = ReaderDimens.radiusLg
                )
            )
            .background(AppTheme.colors.surface)
            .padding(bottom = ReaderDimens.spaceSm)
    ) {
        // The bottom bar row
        ReaderBottomBar(
            onFontClick = {
                currentSheet = ReaderSheetContent.FONTS
                scope.launch { sheetState.show() }
            },
            onAudioClick = {
                currentSheet = ReaderSheetContent.AUDIO
                scope.launch { sheetState.show() }
            },
            onProgressClick = {
                currentSheet = ReaderSheetContent.PROGRESS
                scope.launch { sheetState.show() }
            },
            onHighlightsClick = {
                currentSheet = ReaderSheetContent.HIGHLIGHTS
                scope.launch { sheetState.show() }
            }
        )

        // The actual modal sheet
        if (currentSheet != ReaderSheetContent.NONE) {
            ModalBottomSheet(
                containerColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.onBackground,
                sheetState = sheetState,
                onDismissRequest = { currentSheet = ReaderSheetContent.NONE }
            ) {
                when (currentSheet) {
                    ReaderSheetContent.FONTS -> {
                        TypographyPanel(
                            state = state,
                            showFontDialog = showFontDialog,
                            snackBarHostState = snackBarHostState,
                            onFontSizeChanged = { viewModel.setFontSize(it) },
                        )
                    }

                    ReaderSheetContent.AUDIO -> {
                        Column(
                            Modifier.padding(
                                horizontal = ReaderDimens.spaceLg,
                                vertical = ReaderDimens.spaceMd
                            )
                        ) {
                            val ttsState = viewModel.ttsState.collectAsStateWithLifecycle()
                            if (ttsState.value.isReady) {
                                TtsControls(
                                    vm = viewModel,
                                    onJumpToTtsLocation = onJumpToTtsLocation
                                )
                            } else {
                                Text(
                                    "Initializing speech…",
                                    style = AppTheme.typography.body3,
                                    color = AppTheme.colors.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    ReaderSheetContent.PROGRESS -> {
                        Text("Progress UI here", modifier = Modifier.padding(16.dp))
                    }

                    ReaderSheetContent.HIGHLIGHTS -> {
                        HighlightsSheet(
                            state = state,
                            onNavigateTo = { chapterIndex, paragraphIndex ->
                                viewModel.requestFocus(chapterIndex, paragraphIndex)
                            },
                            onDelete = { chapterId, h -> viewModel.removeHighlight(chapterId, h) },
//                        onClearChapter = { chapterId ->
//                            // remove all for one chapter
//                            state.highlightsByChapter[chapterId].orEmpty()
//                                .forEach { viewModel.removeHighlight(chapterId, it) }
//                        },
//                        onClearAll = {
//                            state.highlightsByChapter.forEach { (chapterId, list) ->
//                                list.forEach { viewModel.removeHighlight(chapterId, it) }
//                            }
//                        }
                            onChangeColor = { chapterId, h, newColor ->
                                viewModel.changeHighlightColor(chapterId, h, newColor)
                            },
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

//@OptIn(ExperimentalAnimationApi::class)
//@Composable
//fun ReaderBottomArea(
//    state: ReaderScreenState,
//    viewModel: ReaderViewModel,
//    snackBarHostState: SnackbarHostState,
//    showFontDialog: MutableState<Boolean>,
//    onJumpToTtsLocation: (Int) -> Unit = {}
//) {
//    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Text, 1 = Audio
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(topStart = ReaderDimens.radiusLg, topEnd = ReaderDimens.radiusLg))
//            .background(AppTheme.colors.surface)
//            .padding(bottom = ReaderDimens.spaceSm)
//    ) {
//        // Handle
//        Box(
//            modifier = Modifier
//                .padding(top = ReaderDimens.spaceSm)
//                .width(56.dp)
//                .height(5.dp)
//                .clip(RoundedCornerShape(50))
//                .background(AppTheme.colors.onSurface.copy(alpha = 0.28f))
//                .align(Alignment.CenterHorizontally)
//        )
//
//        // Tabs
//        TabRow(
//            selectedTabIndex = selectedTab,
//            containerColor = AppTheme.colors.surface,
//            contentColor = AppTheme.colors.onSurface,
//        ) {
//            Tab(
//                selected = selectedTab == 0,
//                onClick = { selectedTab = 0 },
//                text = {
//                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                        Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_reader_font), contentDescription = null, tint = AppTheme.colors.onSurface)
//                        Text("Text", style = AppTheme.typography.body2, color = AppTheme.colors.onSurface)
//                    }
//                }
//            )
//            Tab(
//                selected = selectedTab == 1,
//                onClick = { selectedTab = 1 },
//                text = {
//                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, tint = AppTheme.colors.onSurface)
//                        Text("Audio", style = AppTheme.typography.body2, color = AppTheme.colors.onSurface)
//                    }
//                }
//            )
//        }
//
//        androidx.compose.animation.Crossfade(
//            targetState = selectedTab,
//            label = "bottom-panel-crossfade"
//        ) { tab ->
//            when (tab) {
//                0 -> {
//                    // RECOMMENDED RENAME: TypographyPanel -> TypographyPanel
//                    TypographyPanel(
//                        state = state,
//                        showFontDialog = showFontDialog,
//                        snackBarHostState = snackBarHostState,
//                        onFontSizeChanged = { viewModel.setFontSize(it) }
//                    )
//                }
//                1 -> {
//                    Column(Modifier.padding(horizontal = ReaderDimens.spaceLg, vertical = ReaderDimens.spaceMd)) {
//                        val ttsState = viewModel.ttsState.collectAsStateWithLifecycle()
//                        if (ttsState.value.isReady) {
//                            // Pass the optional jump callback
//                            TtsControls(vm = viewModel, onJumpToTtsLocation = onJumpToTtsLocation)
//                        } else {
//                            Text(
//                                "Initializing speech…",
//                                style = AppTheme.typography.body3,
//                                color = AppTheme.colors.onSurface.copy(alpha = 0.7f),
//                                modifier = Modifier.padding(top = ReaderDimens.spaceSm)
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

@Composable
fun ReaderFontChooserDialog(
    showFontDialog: MutableState<Boolean>,
    fontFamily: ReaderFont,
    onFontFamilyChanged: (ReaderFont) -> Unit
) {
    val radioOptions = ReaderFont.getAllFonts()
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(fontFamily) }

    if (showFontDialog.value) {
        AlertDialog(
            containerColor = AppTheme.colors.background.copy(0.8f),
            onDismissRequest = { showFontDialog.value = false },
            title = {
                Text(
                    text = stringResource(id = R.string.reader_font_style_chooser),
                    style = AppTheme.typography.h2
                )
            },
            text = {
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    radioOptions.forEach { font ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .selectable(
                                    selected = (font.name == selectedOption.name),
                                    onClick = { onOptionSelected(font) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (font.name == selectedOption.name),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AppTheme.colors.primary
                                )
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = font.name,
                                    style = AppTheme.typography.body1,
                                    fontFamily = font.fontFamily // Preview with actual font
                                )
                                Text(
                                    text = "Preview text",
                                    style = AppTheme.typography.body3,
                                    color = AppTheme.colors.onSurface,
                                    fontFamily = font.fontFamily
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        showFontDialog.value = false
                        onFontFamilyChanged(selectedOption)
                    }
                ) {
                    Text(stringResource(id = R.string.theme_dialog_apply_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFontDialog.value = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ReaderBottomBar(
    onHighlightsClick: () -> Unit,
    onProgressClick: () -> Unit,
    onFontClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)// a touch of breathing room
            .background(AppTheme.colors.surface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            variant = IconButtonVariant.Ghost,
            onClick = onHighlightsClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Star, // replace with highlight icon
                contentDescription = "Highlights",
                tint = AppTheme.colors.onSurface
            )
        }


        IconButton(
            variant = IconButtonVariant.Ghost,
            onClick = onFontClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_reader_font),
                contentDescription = "Fonts",
                tint = AppTheme.colors.onSurface
            )
        }

        IconButton(
            variant = IconButtonVariant.Ghost,
            onClick = onProgressClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MenuBook,
                contentDescription = "Progress",
                tint = AppTheme.colors.onSurface
            )
        }

        IconButton(
            variant = IconButtonVariant.Ghost,
            onClick = onAudioClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Headphones,
                contentDescription = "Audio",
                tint = AppTheme.colors.onSurface
            )
        }
    }
}

private object ReaderDimens {
    val spaceXs = 6.dp
    val spaceSm = 10.dp
    val spaceMd = 14.dp
    val spaceLg = 20.dp
    val spaceXl = 24.dp
    val radiusMd = 14.dp
    val radiusLg = 20.dp
    val radiusPill = 999.dp
    val controlHeight = 44.dp
}

@Composable
fun TypographyPanel(
    state: ReaderScreenState,
    showFontDialog: MutableState<Boolean>,
    snackBarHostState: SnackbarHostState,
    onFontSizeChanged: (newValue: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface)
            .clip(
                RoundedCornerShape(
                    topStart = ReaderDimens.radiusLg,
                    topEnd = ReaderDimens.radiusLg
                )
            )
            .padding(vertical = ReaderDimens.spaceLg, horizontal = ReaderDimens.spaceLg)
    ) {
        // RECOMMENDED RENAME: TextScaleControls -> FontSizeRow
        TextScaleControls(
            fontSize = state.fontSize,
            snackBarHostState = snackBarHostState,
            onFontSizeChanged = onFontSizeChanged
        )

        Spacer(modifier = Modifier.height(ReaderDimens.spaceLg))

        FontSelectionButton(
            readerFontFamily = state.fontFamily,
            showFontDialog = showFontDialog
        )
    }
}


@Composable
private fun TextScaleControls(
    fontSize: Int,
    snackBarHostState: SnackbarHostState,
    onFontSizeChanged: (newValue: Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReaderTextScaleButton(
            buttonType = TextScaleButtonType.DECREASE,
            fontSize = fontSize,
            snackBarHostState = snackBarHostState,
            onFontSizeChanged = onFontSizeChanged
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.animateContentSize()
        ) {
            Text(
                text = fontSize.toString(),
                style = AppTheme.typography.label1,
                fontFamily = poppinsFont
            )
            Text(
                text = "Font size",
                style = AppTheme.typography.label3,
                color = AppTheme.colors.onSurface.copy(alpha = 0.48f)
            )
        }

        ReaderTextScaleButton(
            buttonType = TextScaleButtonType.INCREASE,
            fontSize = fontSize,
            snackBarHostState = snackBarHostState,
            onFontSizeChanged = onFontSizeChanged
        )
    }
}


@Composable
private fun ReaderTextScaleButton(
    buttonType: TextScaleButtonType,
    fontSize: Int,
    snackBarHostState: SnackbarHostState,
    onFontSizeChanged: (newValue: Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val (iconRes, adjustment) = remember(buttonType) {
        when (buttonType) {
            TextScaleButtonType.DECREASE -> Pair(R.drawable.ic_reader_text_minus, -5)
            TextScaleButtonType.INCREASE -> Pair(R.drawable.ic_reader_text_plus, 5)
        }
    }

    val onClick = {
        val newSize = fontSize + adjustment
        when {
            newSize < 50 -> coroutineScope.launch {
                snackBarHostState.showSnackbar(
                    context.getString(R.string.reader_min_font_size_reached),
                    null
                )
            }

            newSize > 200 -> coroutineScope.launch {
                snackBarHostState.showSnackbar(
                    context.getString(R.string.reader_max_font_size_reached),
                    null
                )
            }

            else -> onFontSizeChanged(newSize)
        }
    }

    FilledTonalButton(
        colors = ButtonDefaults.textButtonColors(containerColor = AppTheme.colors.primary),
        onClick = onClick as () -> Unit,
        modifier = Modifier.size(width = 100.dp, height = ReaderDimens.controlHeight),
        shape = RoundedCornerShape(ReaderDimens.radiusMd),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
            tint = AppTheme.colors.onPrimary
        )
    }
}

@Composable
private fun ValuePill(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(ReaderDimens.radiusPill))
            .background(AppTheme.colors.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = AppTheme.typography.label3,
            color = AppTheme.colors.onSurface.copy(alpha = 0.8f)
        )
    }
}

//@Composable
//fun TtsControls(
//    vm: ReaderViewModel,
//    onJumpToTtsLocation: ((Int) -> Unit)? = null
//) {
//    val ui by vm.ttsState.collectAsStateWithLifecycle()
//    val reader by vm.state.collectAsStateWithLifecycle()
//
//    val ready = ui.isReady
//    val hasLocator = ui.currentLocator != null
//
//        // Where am I? + Jump
//       AnimatedVisibility(visible = hasLocator) {
//            Column(Modifier.padding(bottom = ReaderDimens.spaceSm)) {
//                val loc = ui.currentLocator!!
//                val chNumber = loc.chapterIndex + 1
//                val chCount = reader.chapters.size
//                val paraNumber = loc.paragraphIndex + 1
//                val sentNumber = loc.sentenceIndexInParagraph + 1
//                val chTitle = reader.chapters.getOrNull(loc.chapterIndex)?.title.orEmpty()
//
//                Row(horizontalArrangement = Arrangement.spacedBy(ReaderDimens.spaceSm)) {
//                    ValuePill("Ch $chNumber/$chCount • ¶$paraNumber • S $sentNumber")
//                    if (onJumpToTtsLocation != null) {
//                        TextButton(onClick = { onJumpToTtsLocation(loc.chapterIndex) }) {
//                            Text("View location")
//                        }
//                    }
//                }
//                if (chTitle.isNotBlank()) {
//                    Text(
//                        chTitle,
//                        style = AppTheme.typography.label3,
//                        color = AppTheme.colors.onSurface.copy(alpha = 0.6f),
//                        modifier = Modifier.padding(top = ReaderDimens.spaceXs),
//                        maxLines = 1
//                    )
//                }
//            }
//        }
//
//        // Status + Auto-scroll
//        Row(
//            Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            val status = if (ui.isPlaying) "Reading aloud" else "Ready"
//            Text(status, style = AppTheme.typography.label2, color = AppTheme.colors.onSurface.copy(alpha = 0.8f))
//
//            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                Text("Auto-scroll", style = AppTheme.typography.label3, color = AppTheme.colors.onSurface.copy(alpha = 0.7f))
//                Switch(checked = ui.autoScroll, onCheckedChange = { vm.ttsSetAutoScroll(it) })
//            }
//        }
//
//        Spacer(Modifier.height(ReaderDimens.spaceSm))
//
//        // Transport
//        Row(
//            Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceEvenly,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(
//                enabled = ready && hasLocator,
//                modifier = Modifier.clip(RoundedCornerShape(ReaderDimens.radiusMd)),
//                variant = IconButtonVariant.PrimaryElevated,
//                onClick = vm::ttsPrev
//            ) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous") }
//
//            IconButton(
//                enabled = ready,
//                modifier = Modifier.clip(RoundedCornerShape(ReaderDimens.radiusMd)),
//                variant = IconButtonVariant.PrimaryElevated,
//                onClick = vm::ttsPlayPauseToggle,
//            ) {
//                if (ui.isPlaying) Icon(Icons.Rounded.Pause, contentDescription = "Pause")
//                else Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
//            }
//
//            IconButton(
//                enabled = ready && hasLocator,
//                modifier = Modifier.clip(RoundedCornerShape(ReaderDimens.radiusMd)),
//                variant = IconButtonVariant.PrimaryElevated,
//                onClick = vm::ttsNext
//            ) { Icon(Icons.Rounded.SkipNext, contentDescription = "Next") }
//
//            IconButton(
//                enabled = ready && hasLocator,
//                modifier = Modifier.clip(RoundedCornerShape(ReaderDimens.radiusMd)),
//                variant = IconButtonVariant.PrimaryElevated,
//                onClick = vm::ttsStop
//            ) { Icon(Icons.Rounded.Stop, contentDescription = "Stop") }
//        }
//
//        Spacer(Modifier.height(ReaderDimens.spaceMd))
//
//        // Speed
//        LabeledSliderRow(
//            label = "Speed",
//            valueText = String.format("%.1fx", ui.speed),
//            value = ui.speed,
//            onChange = { vm.ttsSetSpeed((it * 10f).toInt() / 10f) },
//            range = 0.5f..2.0f,
//            steps = 15, // ~0.1 increments
//            onDecrement = { vm.ttsSetSpeed((ui.speed - 0.1f).coerceIn(0.5f, 2.0f)) },
//            onIncrement = { vm.ttsSetSpeed((ui.speed + 0.1f).coerceIn(0.5f, 2.0f)) }
//        )
//
//        Spacer(Modifier.height(ReaderDimens.spaceSm))
//
//    }

@Composable
private fun LabeledSliderRow(
    label: String,
    valueText: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = AppTheme.typography.label2)
            Text(
                valueText,
                style = AppTheme.typography.label2,
                color = AppTheme.colors.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.animateContentSize()
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier
                    .clip(RoundedCornerShape(ReaderDimens.radiusMd))
                    .height(ReaderDimens.controlHeight),
                variant = ButtonVariant.PrimaryElevated,
                onClick = onDecrement
            ) { Text("−0.1") }

            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                steps = steps
            )

            Button(
                modifier = Modifier
                    .clip(RoundedCornerShape(ReaderDimens.radiusMd))
                    .height(ReaderDimens.controlHeight),
                variant = ButtonVariant.PrimaryElevated,
                onClick = onIncrement
            ) { Text("+0.1") }
        }
    }
}

@Composable
private fun FontSelectionButton(
    readerFontFamily: ReaderFont,
    showFontDialog: MutableState<Boolean>
) {
    FilledTonalButton(
        colors = ButtonDefaults.textButtonColors(containerColor = AppTheme.colors.primary),
        onClick = { showFontDialog.value = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(ReaderDimens.controlHeight),
        shape = RoundedCornerShape(ReaderDimens.radiusMd)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_reader_font),
                contentDescription = "Choose font",
                tint = AppTheme.colors.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Font: ${readerFontFamily.name}",
                style = AppTheme.typography.body2,
                color = AppTheme.colors.onPrimary
            )
        }
    }
}


//fun ReaderBottomArea(
//    state: ReaderScreenState,
//    viewModel: ReaderViewModel,
//    snackBarHostState: SnackbarHostState,
//    showFontDialog: MutableState<Boolean>,
//    onJumpToTtsLocation: (Int) -> Unit
//) {
//    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Text, 1 = Audio
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
//            .background(AppTheme.colors.surface)
//            .padding(bottom = 12.dp)
//    ) {
//        // Handle (drag affordance)
//        Box(
//            modifier = Modifier
//                .padding(top = 10.dp)
//                .width(40.dp)
//                .height(4.dp)
//                .clip(RoundedCornerShape(50))
//                .background(AppTheme.colors.onSurface.copy(alpha = 0.35f))
//                .align(Alignment.CenterHorizontally)
//        )
//
//        // Tabs with icons
//        TabRow(
//            selectedTabIndex = selectedTab,
//            containerColor = AppTheme.colors.surface,
//            contentColor = AppTheme.colors.onSurface,
//        ) {
//            Tab(
//                selected = selectedTab == 0,
//                onClick = { selectedTab = 0 },
//                text = {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        Icon(
//                            imageVector = ImageVector.vectorResource(R.drawable.ic_reader_font),
//                            contentDescription = null,
//                            tint = AppTheme.colors.onSurface
//                        )
//                        Text(
//                            "Text",
//                            style = AppTheme.typography.body2,
//                            color = AppTheme.colors.onSurface
//                        )
//                    }
//                }
//            )
//            Tab(
//                selected = selectedTab == 1,
//                onClick = { selectedTab = 1 },
//                text = {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        // Use a play icon you already have, or switch to Material icon if available
//                        Icon(
//                            imageVector = Icons.Rounded.PlayArrow,
//                            contentDescription = null,
//                            tint = AppTheme.colors.onSurface
//                        )
//                        Text(
//                            "Audio",
//                            style = AppTheme.typography.body2,
//                            color = AppTheme.colors.onSurface
//                        )
//                    }
//                }
//            )
//        }
//
//        when (selectedTab) {
//            0 -> {
//                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
//                    TypographyPanel(
//                        state = state,
//                        showFontDialog = showFontDialog,
//                        snackBarHostState = snackBarHostState,
//                        onFontSizeChanged = { viewModel.setFontSize(it) },
//                    )
//                }
//            }
//
//            1 -> {
//                val ttsState = viewModel.ttsState.collectAsStateWithLifecycle()
//                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
//                    if (ttsState.value.isReady) {
//                        TtsControls(vm = viewModel, onJumpToTtsLocation = onJumpToTtsLocation)
//                    } else {
//                        Text(
//                            "Initializing speech…",
//                            style = AppTheme.typography.body3,
//                            color = AppTheme.colors.onSurface.copy(alpha = 0.7f)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}

//@Composable
//fun TypographyPanel(
//    state: ReaderScreenState,
//    showFontDialog: MutableState<Boolean>,
//    snackBarHostState: SnackbarHostState,
//    onFontSizeChanged: (newValue: Int) -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
//            .background(AppTheme.colors.surface)
//            .padding(vertical = 16.dp, horizontal = 20.dp)
//    ) {
//
//        TextScaleControls(
//            fontSize = state.fontSize,
//            snackBarHostState = snackBarHostState,
//            onFontSizeChanged = onFontSizeChanged
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        FontSelectionButton(
//            readerFontFamily = state.fontFamily,
//            showFontDialog = showFontDialog
//        )
//
//    }
//}
//
//@Composable
//private fun TextScaleControls(
//    fontSize: Int,
//    snackBarHostState: SnackbarHostState,
//    onFontSizeChanged: (newValue: Int) -> Unit
//) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceEvenly,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        ReaderTextScaleButton(
//            buttonType = TextScaleButtonType.DECREASE,
//            fontSize = fontSize,
//            snackBarHostState = snackBarHostState,
//            onFontSizeChanged = onFontSizeChanged
//        )
//
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Text(
//                text = fontSize.toString(),
//                style = AppTheme.typography.label1,
//                fontFamily = poppinsFont
//            )
//            Text(
//                text = "Font size",
//                style = AppTheme.typography.label3,
//                color = AppTheme.colors.onSurface.copy(alpha = 0.4f)
//            )
//        }
//
//        ReaderTextScaleButton(
//            buttonType = TextScaleButtonType.INCREASE,
//            fontSize = fontSize,
//            snackBarHostState = snackBarHostState,
//            onFontSizeChanged = onFontSizeChanged
//        )
//    }
//}
//
//@Composable
//private fun FontSelectionButton(
//    readerFontFamily: ReaderFont,
//    showFontDialog: MutableState<Boolean>
//) {
//    FilledTonalButton(
//        colors = ButtonDefaults.textButtonColors(
//            containerColor = AppTheme.colors.primary,
//        ),
//        onClick = { showFontDialog.value = true },
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(14.dp),
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.Center,
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Icon(
//                imageVector = ImageVector.vectorResource(id = R.drawable.ic_reader_font),
//                contentDescription = null,
//                tint = AppTheme.colors.onPrimary
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = "Font: ${readerFontFamily.name}",
//                style = AppTheme.typography.body2,
//                color = AppTheme.colors.onPrimary
//            )
//        }
//    }
//}
//
//
@Composable
fun TtsControls(
    vm: ReaderViewModel,
    modifier: Modifier = Modifier,
    onJumpToTtsLocation: ((Int) -> Unit)? = null
) {
    val ttsState = vm.ttsState.collectAsStateWithLifecycle()
    val reader by vm.state.collectAsStateWithLifecycle()
    val tts = ttsState.value
    val ready = tts.isReady
    val hasLocator = tts.currentLocator != null
    Column(modifier) {
        // Speed
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Speed", style = AppTheme.typography.label2)
                Text(
                    String.format("%.1fx", tts.speed),
                    style = AppTheme.typography.label2,
                    color = AppTheme.colors.onSurface.copy(alpha = 0.8f)
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    variant = ButtonVariant.PrimaryElevated,
                    onClick = { vm.ttsSetSpeed((tts.speed - 0.1f).coerceIn(0.5f, 2.0f)) },

                    ) { Text("−0.1") }

                Slider(
                    value = tts.speed,
                    onValueChange = { vm.ttsSetSpeed((it * 10f).toInt() / 10f) },
                    valueRange = 0.5f..2.0f,
                )


                Button(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    variant = ButtonVariant.PrimaryElevated,
                    onClick = { vm.ttsSetSpeed((tts.speed + 0.1f).coerceIn(0.5f, 2.0f)) },

                    ) { Text("+0.1") }
            }
        }
        // Status + Auto-scroll
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val status = if (tts.isPlaying) "Reading aloud" else "Ready"
            Text(
                status,
                style = AppTheme.typography.label2,
                color = AppTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Auto-scroll",
                    style = AppTheme.typography.label3,
                    color = AppTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Switch(
                    checked = tts.autoScroll,
                    onCheckedChange = { vm.ttsSetAutoScroll(it) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Transport
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                enabled = ready && hasLocator,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                variant = IconButtonVariant.PrimaryElevated,
                onClick = vm::ttsPrev
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous")
            }

            // Big Play/Pause
            IconButton(
                enabled = ready,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                variant = IconButtonVariant.PrimaryElevated,
                onClick = vm::ttsPlayPauseToggle,
            ) {
                if (tts.isPlaying) {
                    Icon(Icons.Rounded.Pause, contentDescription = "Pause")
                } else {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
                }
            }

            IconButton(
                enabled = ready && hasLocator,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                variant = IconButtonVariant.PrimaryElevated,
                onClick = vm::ttsNext
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next")
            }

            IconButton(
                enabled = ready && hasLocator,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                variant = IconButtonVariant.PrimaryElevated,
                onClick = vm::ttsStop
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = "Stop")
            }
        }
        if (ready && !hasLocator && !tts.isPlaying) {
            Text(
                "Tip: long-press text → Read from here to start TTS.",
                style = AppTheme.typography.label3,
                color = AppTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(12.dp))

        val loc = tts.currentLocator
        if (loc != null) {
            val chNumber = loc.chapterIndex + 1
            val chCount = reader.chapters.size
            val paraNumber = loc.paragraphIndex + 1
            val sentNumber = loc.sentenceIndexInParagraph + 1
            val chTitle = reader.chapters.getOrNull(loc.chapterIndex)?.title.orEmpty()

            Row(Modifier.padding(top = 6.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(AppTheme.colors.onSurface.copy(alpha = 0.06f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Ch $chNumber/$chCount • ¶$paraNumber • S $sentNumber",
                        style = AppTheme.typography.label3,
                        color = AppTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            if (chTitle.isNotBlank()) {
                Text(
                    chTitle,
                    style = AppTheme.typography.label3,
                    color = AppTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1
                )
            }
        } else {
            Text(
                "No active position",
                style = AppTheme.typography.label3,
                color = AppTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        tts.currentLocator?.let { loc ->
            TextButton(
                onClick = { onJumpToTtsLocation?.invoke(loc.chapterIndex) },
                enabled = onJumpToTtsLocation != null,
                modifier = Modifier.padding(top = 4.dp)
            ) { Text("View location") }
        }

    }
}


//@Composable
//private fun ReaderTextScaleButton(
//    buttonType: TextScaleButtonType,
//    fontSize: Int,
//    snackBarHostState: SnackbarHostState,
//    onFontSizeChanged: (newValue: Int) -> Unit
//) {
//    val coroutineScope = rememberCoroutineScope()
//
//    val context = LocalContext.current
//    val (iconRes, adjustment) = remember(buttonType) {
//        when (buttonType) {
//            TextScaleButtonType.DECREASE -> Pair(R.drawable.ic_reader_text_minus, -5)
//            TextScaleButtonType.INCREASE -> Pair(R.drawable.ic_reader_text_plus, 5)
//        }
//    }
//
//    val callback: () -> Unit = {
//        val newSize = fontSize + adjustment
//        when {
//            newSize < 50 -> {
//                coroutineScope.launch {
//                    snackBarHostState.showSnackbar(
//                        context.getString(R.string.reader_min_font_size_reached),
//                        null
//                    )
//                }
//            }
//
//            newSize > 200 -> {
//                coroutineScope.launch {
//                    snackBarHostState.showSnackbar(
//                        context.getString(R.string.reader_max_font_size_reached),
//                        null
//                    )
//                }
//            }
//
//            else -> {
//                coroutineScope.launch {
//                    val adjustedSize = fontSize + adjustment
//                    onFontSizeChanged(adjustedSize)
//                }
//            }
//        }
//    }
//
//    FilledTonalButton(
//        colors = ButtonDefaults.textButtonColors(
//            containerColor = AppTheme.colors.primary,
//        ),
//        onClick = { callback() },
//        modifier = Modifier.size(100.dp, 45.dp),
//        shape = RoundedCornerShape(18.dp),
//    ) {
//        Icon(
//            imageVector = ImageVector.vectorResource(id = iconRes),
//            contentDescription = null,
//            modifier = Modifier.size(ButtonDefaults.IconSize),
//            tint = AppTheme.colors.onPrimary
//
//        )
//    }
//}
