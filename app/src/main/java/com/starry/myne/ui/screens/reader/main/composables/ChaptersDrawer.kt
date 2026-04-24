package com.starry.myne.ui.screens.reader.main.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starry.myne.R
import com.starry.myne.epub.models.EpubChapter
import custom.AppTheme
import custom.components.HorizontalDivider
import custom.components.Text
import kotlinx.coroutines.launch

@Composable
fun ChaptersDrawer(
    drawerState: DrawerState,
    chapters: List<EpubChapter>,
    currentChapterIndex: Int,
    onScrollToChapter: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        modifier = Modifier
            .systemBarsPadding()
            .background(AppTheme.colors.background),
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AppTheme.colors.background.copy(0.9f),
                drawerContentColor = AppTheme.colors.onBackground
            ) {
                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(id = R.string.reader_chapter_list_title),
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.onBackground
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp),
                    thickness = 0.5.dp,
                )
                LazyColumn {
                    items(
                        count = chapters.size,
                        key = { idx -> chapters[idx].chapterId }) { idx ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = chapters[idx].title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = idx == currentChapterIndex,
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    onScrollToChapter(idx)
                                }
                            }
                        )
                    }
                }
            }
        }, content = content
    )
}