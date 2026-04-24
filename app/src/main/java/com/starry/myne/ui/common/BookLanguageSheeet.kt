package com.starry.myne.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starry.myne.R
import com.starry.myne.helpers.book.BookLanguage
import com.starry.myne.helpers.weakHapticFeedback
import custom.AppTheme
import custom.components.Text
import custom.components.card.CardDefaults
import custom.components.card.OutlinedCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookLanguageSheet(
    showBookLanguage: MutableState<Boolean>,
    selectedLanguage: BookLanguage,
    onLanguageChange: (BookLanguage) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    if (showBookLanguage.value) {
        ModalBottomSheet(
            containerColor = AppTheme.colors.background,
            sheetState = sheetState,
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                    delay(300)
                    showBookLanguage.value = false
                }
            }
        ) {
            val languages = BookLanguage.getAllLanguages()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(AppTheme.colors.background),
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    text = stringResource(id = R.string.language_menu_title),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp
                )
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp),
                    columns = GridCells.Adaptive(168.dp)
                ) {
                    items(languages.size) { idx ->
                        val language = languages[idx]
                        BookLanguageButton(
                            language = language,
                            isSelected = language == selectedLanguage,
                            onClick = {
                                view.weakHapticFeedback()
                                coroutineScope.launch {
                                    sheetState.hide()
                                    delay(300)
                                    showBookLanguage.value = false
                                    onLanguageChange(language)
                                }
                            })
                    }
                }
            }
        }
    }
}


@Composable
private fun BookLanguageButton(language: BookLanguage, isSelected: Boolean, onClick: () -> Unit) {
    val buttonColor: Color
    val textColor: Color
    if (isSelected) {
        buttonColor = AppTheme.colors.onBackground
        textColor = AppTheme.colors.background
    } else {
        buttonColor = AppTheme.colors.surface
        textColor = AppTheme.colors.onSurface
    }

    OutlinedCard(
        modifier = Modifier
            .height(60.dp)
            .width(70.dp)
            .padding(6.dp),
        colors = CardDefaults.cardColors(containerColor = buttonColor),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                modifier = Modifier.padding(2.dp),
                text = language.name,
                fontStyle = AppTheme.typography.h2.fontStyle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
            )
        }
    }
}