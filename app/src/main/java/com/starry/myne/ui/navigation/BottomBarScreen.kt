package com.starry.myne.ui.navigation

import com.starry.myne.R

sealed class BottomBarScreen(
    val route: String,
    val title: Int,
    val icon: Int
) {
    data object Home : BottomBarScreen(
        route = "home",
        title = R.string.navigation_home,
        icon = R.drawable.ic_nav_home
    )

    data object Categories : BottomBarScreen(
        route = "categories",
        title = R.string.navigation_categories,
        icon = R.drawable.ic_nav_categories
    )

    data object Library : BottomBarScreen(
        route = "library",
        title = R.string.navigation_library,
        icon = R.drawable.ic_nav_library
    )

    data object Settings : BottomBarScreen(
        route = "settings",
        title = R.string.navigation_settings,
        icon = R.drawable.ic_nav_settings
    )
}