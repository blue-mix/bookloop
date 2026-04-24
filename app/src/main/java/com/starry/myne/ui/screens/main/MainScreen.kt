package com.starry.myne.ui.screens.main

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.starry.myne.MainViewModel
import com.starry.myne.helpers.NetworkObserver
import com.starry.myne.ui.navigation.BottomBarScreen
import com.starry.myne.ui.navigation.NavGraph
import com.starry.myne.ui.navigation.Screens
import custom.AppTheme
import custom.components.Icon
import custom.components.Scaffold
import custom.components.Text

/**
 * Padding for the bottom navigation bar.
 */
val bottomNavPadding = 70.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    intent: Intent,
    startDestination: String,
    networkStatus: NetworkObserver.Status,
) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            BottomBar(navController = navController)
        },
        containerColor = AppTheme.colors.background,
    ) {
        NavGraph(
            startDestination = startDestination,
            navController = navController,
            networkStatus = networkStatus
        )

        val shouldHandleShortCut = remember { mutableStateOf(false) }
        LaunchedEffect(key1 = true) {
            shouldHandleShortCut.value = true
        }
        if (shouldHandleShortCut.value) {
            HandleShortcutIntent(intent, navController)
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val screens = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Categories,
        BottomBarScreen.Library,
        BottomBarScreen.Settings,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomBarDestination = screens.any { it.route == currentDestination?.route }

    AnimatedVisibility(
        visible = bottomBarDestination,
        modifier = Modifier.fillMaxWidth(),
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        content = {
            Row(
                modifier = Modifier
                    .background(AppTheme.colors.background)
                    .padding(12.dp)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                screens.forEach { screen ->
                    CustomBottomNavigationItem(
                        screen = screen, isSelected = screen.route == currentDestination?.route
                    ) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                }
            }
        })
}

@Composable
private fun CustomBottomNavigationItem(
    screen: BottomBarScreen,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background =
        if (isSelected) AppTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent
    val contentColor =
        if (isSelected) AppTheme.colors.primary else AppTheme.colors.onBackground

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Icon(
                imageVector = ImageVector.vectorResource(id = screen.icon),
                contentDescription = stringResource(id = screen.title),
                tint = contentColor
            )

            AnimatedVisibility(visible = isSelected) {
                Text(
                    text = stringResource(id = screen.title),
                    color = contentColor,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HandleShortcutIntent(intent: Intent, navController: NavController) {
    val data = intent.data
    if (data != null && data.scheme == MainViewModel.LAUNCHER_SHORTCUT_SCHEME) {
        val libraryItemId = intent.getIntExtra(MainViewModel.LC_SC_LIBRARY_ITEM_ID, -100)
        if (libraryItemId != -100) {
            navController.navigate(Screens.ReaderDetailScreen.withLibraryItemId(libraryItemId.toString()))
            return
        }
        if (intent.getBooleanExtra(MainViewModel.LC_SC_BOOK_LIBRARY, false)) {
            navController.navigate(BottomBarScreen.Library.route) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }
}