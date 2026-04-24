package com.starry.myne.ui.screens.home.composables

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.starry.myne.R
import com.starry.myne.helpers.NetworkObserver
import com.starry.myne.helpers.book.BookLanguage
import com.starry.myne.helpers.book.BookUtils
import com.starry.myne.ui.common.BookItemCard
import com.starry.myne.ui.common.BookItemShimmerLoader
import com.starry.myne.ui.common.BookLanguageSheet
import com.starry.myne.ui.common.NetworkError
import com.starry.myne.ui.common.ProgressDots
import com.starry.myne.ui.navigation.BottomBarScreen
import com.starry.myne.ui.navigation.Screens
import com.starry.myne.ui.screens.home.viewmodels.AllBooksState
import com.starry.myne.ui.screens.home.viewmodels.HomeViewModel
import com.starry.myne.ui.screens.home.viewmodels.SearchBarState
import com.starry.myne.ui.screens.home.viewmodels.UserAction
import com.starry.myne.ui.screens.main.bottomNavPadding
import custom.AppTheme
import custom.components.HorizontalDivider
import custom.components.Icon
import custom.components.IconButton
import custom.components.IconButtonVariant
import custom.components.Text
import custom.components.textfield.OutlinedTextField
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(navController: NavController, networkStatus: NetworkObserver.Status) {

    val viewModel: HomeViewModel = hiltViewModel()

    // Load the first set of items.
    LaunchedEffect(key1 = true) {
        delay(200)
        viewModel.loadNextItems()
    }

    /*
     Block back button press if search bar is visible to avoid
     app from closing immediately, instead disable search bar
     on first back press, and close app on second.
     */
    val sysBackButtonState = remember { mutableStateOf(false) }
    BackHandler(enabled = sysBackButtonState.value) {
        if (viewModel.searchBarState.isSearchBarVisible) {
            if (viewModel.searchBarState.searchText.isNotEmpty()) {
                viewModel.onAction(UserAction.TextFieldInput("", networkStatus))
            } else {
                viewModel.onAction(UserAction.CloseIconClicked)
            }
        }
    }

    // Close search bar when navigating to other screens.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    LaunchedEffect(currentDestination) {
        if (currentDestination?.route != BottomBarScreen.Home.route) {
            viewModel.onAction(UserAction.TextFieldInput("", networkStatus))
            viewModel.onAction(UserAction.CloseIconClicked)
        }
    }

    val showLanguageSheet = remember { mutableStateOf(false) }
    BookLanguageSheet(
        showBookLanguage = showLanguageSheet,
        selectedLanguage = viewModel.language.value,
        onLanguageChange = { viewModel.onAction(UserAction.LanguageItemClicked(it)) }
    )

    HomeScreenScaffold(
        viewModel = viewModel,
        networkStatus = networkStatus,
        navController = navController,
        sysBackButtonState = sysBackButtonState,
        showLanguageSheet = showLanguageSheet
    )
}

@Composable
private fun HomeScreenScaffold(
    viewModel: HomeViewModel,
    networkStatus: NetworkObserver.Status,
    navController: NavController,
    sysBackButtonState: MutableState<Boolean>,
    showLanguageSheet: MutableState<Boolean>
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val topBarState = viewModel.searchBarState

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(bottom = bottomNavPadding),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.background)
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp)
            ) {
                Crossfade(
                    modifier = Modifier.animateContentSize(),
                    targetState = topBarState.isSearchBarVisible,
                    animationSpec = tween(durationMillis = 200), label = "search cross fade"
                ) {
                    if (it) {
                        SearchAppBar(onCloseIconClicked = {
                            viewModel.onAction(UserAction.CloseIconClicked)
                        }, onInputValueChange = { newText ->
                            viewModel.onAction(
                                UserAction.TextFieldInput(newText, networkStatus)
                            )
                        }, text = topBarState.searchText, onSearchClicked = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        })
                        sysBackButtonState.value = true
                    } else {
                        HomeTopAppBar(
                            bookLanguage = viewModel.language.value,
                            onSearchIconClicked = {
                                viewModel.onAction(UserAction.SearchIconClicked)
                            }, onLanguageIconClicked = {
                                showLanguageSheet.value = true
                            })
                        sysBackButtonState.value = false
                    }
                }
                HorizontalDivider(
                    thickness = 2.dp,
                    color = AppTheme.colors.surface
                )
            }
        },
    ) { paddingValues ->
        HomeScreenContents(
            viewModel = viewModel,
            networkStatus = networkStatus,
            navController = navController,
            paddingValues = paddingValues
        )
    }
}

@Composable
fun HomeScreenContents(
    viewModel: HomeViewModel,
    networkStatus: NetworkObserver.Status,
    navController: NavController,
    paddingValues: PaddingValues
) {
    val topBarState = viewModel.searchBarState
    val allBooksState = viewModel.allBooksState


    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(paddingValues)
    ) {

        // If search text is empty show list of all books.
        if (topBarState.searchText.isBlank()) {
            AllBooksList(
                allBooksState = allBooksState,
                networkStatus = networkStatus,
                navController = navController,
                onRetryClicked = { viewModel.reloadItems() },
                onLoadNextItems = { viewModel.loadNextItems() }
            )
        } else {
            SearchBookList(searchBarState = topBarState, navController = navController)
        }
    }
}

@Composable
private fun AllBooksList(
    allBooksState: AllBooksState,
    networkStatus: NetworkObserver.Status,
    navController: NavController,
    onRetryClicked: () -> Unit,
    onLoadNextItems: () -> Unit
) {
    AnimatedVisibility(
        visible = allBooksState.page == 1L && allBooksState.isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BookItemShimmerLoader()
    }

    AnimatedVisibility(
        visible = !allBooksState.isLoading && allBooksState.error != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        NetworkError(onRetryClicked = { onRetryClicked() })
    }

    AnimatedVisibility(
        visible = !allBooksState.isLoading || allBooksState.error == null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .padding(start = 8.dp, end = 8.dp),
            columns = GridCells.Adaptive(295.dp)
        ) {
            items(allBooksState.items.size) { i ->
                val item = allBooksState.items[i]
                if (networkStatus == NetworkObserver.Status.Available
                    && i >= allBooksState.items.size - 1
                    && !allBooksState.endReached
                    && !allBooksState.isLoading
                ) {
                    onLoadNextItems()
                }
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BookItemCard(
                        title = item.title,
                        author = BookUtils.getAuthorsAsString(item.authors),
                        language = BookUtils.getLanguagesAsString(item.languages),
                        subjects = BookUtils.getSubjectsAsString(
                            item.subjects, 3
                        ),
                        coverImageUrl = item.formats.imagejpeg
                    ) {
                        navController.navigate(
                            Screens.BookDetailScreen.withBookId(
                                item.id.toString()
                            )
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = allBooksState.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ProgressDots()
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBookList(searchBarState: SearchBarState, navController: NavController) {
    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(start = 8.dp, end = 8.dp),
        columns = GridCells.Adaptive(295.dp)
    ) {
        if (searchBarState.isSearching) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ProgressDots()
                }
            }
        }

        items(searchBarState.searchResults.size) { i ->
            val item = searchBarState.searchResults[i]
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BookItemCard(
                    title = item.title,
                    author = BookUtils.getAuthorsAsString(item.authors),
                    language = BookUtils.getLanguagesAsString(item.languages),
                    subjects = BookUtils.getSubjectsAsString(
                        item.subjects, 3
                    ),
                    coverImageUrl = item.formats.imagejpeg
                ) {
                    navController.navigate(
                        Screens.BookDetailScreen.withBookId(
                            item.id.toString()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopAppBar(
    bookLanguage: BookLanguage,
    onSearchIconClicked: () -> Unit,
    onLanguageIconClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (bookLanguage == BookLanguage.AllBooks)
                stringResource(id = R.string.home_header) else bookLanguage.name,
            fontSize = 28.sp,
            color = AppTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onLanguageIconClicked, variant = IconButtonVariant.Ghost) {
            Icon(
                imageVector = Icons.Filled.Translate,
                contentDescription = stringResource(id = R.string.home_language_icon_desc),
                tint = AppTheme.colors.onBackground,
                modifier = Modifier.size(30.dp)
            )
        }
        IconButton(onClick = onSearchIconClicked, variant = IconButtonVariant.Ghost) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_search),
                contentDescription = stringResource(id = R.string.home_search_icon_desc),
                tint = AppTheme.colors.onBackground,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}


@Composable
private fun SearchAppBar(
    onCloseIconClicked: () -> Unit,
    onInputValueChange: (String) -> Unit,
    text: String,
    onSearchClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = text,
            onValueChange = { onInputValueChange(it) },
            placeholder = {
                Text(
                    text = "Search...",
                    color = AppTheme.colors.onBackground.copy(
                        alpha = 0.5f
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search Icon",
                    tint = AppTheme.colors.onBackground.copy(
                        alpha = 0.5f
                    )
                )
            },
            trailingIcon = {
                IconButton(variant = IconButtonVariant.Ghost, onClick = {
                    if (text.isNotEmpty()) {
                        onInputValueChange("")
                    } else {
                        onCloseIconClicked()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Icon",
                        tint = AppTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchClicked() }),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.height(5.6.dp))

        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

}


@Composable
@Preview(showBackground = true)
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController(), NetworkObserver.Status.Unavailable)
}