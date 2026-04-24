package com.starry.myne.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.starry.myne.helpers.NetworkObserver
import com.starry.myne.ui.screens.categories.composables.CategoriesScreen
import com.starry.myne.ui.screens.categories.composables.CategoryDetailScreen
import com.starry.myne.ui.screens.detail.composables.BookDetailScreen
import com.starry.myne.ui.screens.home.composables.HomeScreen
import com.starry.myne.ui.screens.library.composables.LibraryScreen
import com.starry.myne.ui.screens.reader.detail.ReaderDetailScreen
import com.starry.myne.ui.screens.settings.composables.OSLScreen
import com.starry.myne.ui.screens.settings.composables.SettingsScreen
import com.starry.myne.ui.screens.welcome.composables.WelcomeScreen
import custom.AppTheme

@Composable
fun NavGraph(
    startDestination: String,
    navController: NavHostController,
    networkStatus: NetworkObserver.Status,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.background(AppTheme.colors.background)
    ) {

        /** Welcome Screen */
        composable(
            route = Screens.WelcomeScreen.route,
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
        ) {
            WelcomeScreen(navController = navController)
        }

        /** Home Screen */
        composable(
            route = BottomBarScreen.Home.route,
            enterTransition = { bottomNavEnter() },
            exitTransition = {
                if (initialState.destination.route == Screens.BookDetailScreen.route) {
                    exitTransition()
                } else bottomNavExit()
            },
            popEnterTransition = {
                if (initialState.destination.route == Screens.BookDetailScreen.route) {
                    popEnterTransition()
                } else bottomNavPopEnter()
            },
            popExitTransition = { bottomNavPopExit() }) {
            HomeScreen(navController, networkStatus)
        }

        /** Book Detail Screen */
        composable(
            route = Screens.BookDetailScreen.route,
            arguments = listOf(
                navArgument(BOOK_ID_ARG_KEY) {
                    type = NavType.StringType
                },
            ),
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() },
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments!!.getString(BOOK_ID_ARG_KEY)!!
            BookDetailScreen(bookId, navController)
        }

        /** Categories Screen */
        composable(
            route = BottomBarScreen.Categories.route,
            enterTransition = { bottomNavEnter() },
            exitTransition = {
                if (initialState.destination.route == Screens.CategoryDetailScreen.route) {
                    exitTransition()
                } else bottomNavExit()
            },
            popEnterTransition = {
                if (initialState.destination.route == Screens.CategoryDetailScreen.route) {
                    popEnterTransition()
                } else bottomNavPopEnter()
            },
            popExitTransition = { bottomNavPopExit() }) {
            CategoriesScreen(navController)
        }

        /** Category Detail Screen */
        composable(
            route = Screens.CategoryDetailScreen.route,
            arguments = listOf(navArgument(CATEGORY_DETAIL_ARG_KEY) {
                type = NavType.StringType
            }),
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() },
        ) { backStackEntry ->
            val category = backStackEntry.arguments!!.getString(CATEGORY_DETAIL_ARG_KEY)!!
            CategoryDetailScreen(category, navController, networkStatus)
        }

        /** Library Screen */
        composable(
            route = BottomBarScreen.Library.route,
            enterTransition = { bottomNavEnter() },
            exitTransition = {
                if (initialState.destination.route == Screens.BookDetailScreen.route
                    || initialState.destination.route == Screens.ReaderDetailScreen.route
                ) {
                    exitTransition()
                } else bottomNavExit()
            },
            popEnterTransition = {
                if (initialState.destination.route == Screens.BookDetailScreen.route
                    || initialState.destination.route == Screens.ReaderDetailScreen.route
                ) {
                    popEnterTransition()
                } else bottomNavPopEnter()
            },
            popExitTransition = { bottomNavPopExit() }) {
            LibraryScreen(navController)
        }

        /** Reader Detail Screen */
        composable(
            route = Screens.ReaderDetailScreen.route,
            arguments = listOf(
                navArgument(
                    LIBRARY_ITEM_ID_ARG_KEY
                ) {
                    type = NavType.StringType
                }),
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() },
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments!!.getString(LIBRARY_ITEM_ID_ARG_KEY)!!
            ReaderDetailScreen(
                libraryItemId = bookId, navController = navController, networkStatus = networkStatus
            )
        }

        /** Settings Screen */
        composable(
            route = BottomBarScreen.Settings.route,
            enterTransition = { bottomNavEnter() },
            exitTransition = {
                if (initialState.destination.route == Screens.OSLScreen.route || initialState.destination.route == Screens.AboutScreen.route) {
                    exitTransition()
                } else bottomNavExit()
            },
            popEnterTransition = {
                if (initialState.destination.route == Screens.OSLScreen.route || initialState.destination.route == Screens.AboutScreen.route) {
                    popEnterTransition()
                } else bottomNavPopEnter()
            },
            popExitTransition = { bottomNavPopExit() }) {
            SettingsScreen(navController)
        }

        /** Open Source Licenses Screen */
        composable(
            route = Screens.OSLScreen.route,
            enterTransition = { enterTransition() },
            popExitTransition = { popExitTransition() },
        ) {
            OSLScreen(navController = navController)
        }

    }
}