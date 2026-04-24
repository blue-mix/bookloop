package com.starry.myne.ui.screens.settings.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.starry.myne.R
import com.starry.myne.ui.common.CustomTopAppBar
import custom.AppTheme


@Composable
fun OSLScreen(navController: NavController) {

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        topBar = {
            CustomTopAppBar(headerText = stringResource(id = R.string.open_source_licenses_header)) {
                navController.navigateUp()
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .padding(paddingValues)
        ) {
            LibrariesContainer(
                modifier = Modifier.fillMaxSize(),
                showAuthor = true,
                showVersion = true,
                showLicenseBadges = true,
                colors = LibraryDefaults.libraryColors(
                    backgroundColor = AppTheme.colors.background,
                    contentColor = AppTheme.colors.onBackground,
                    badgeBackgroundColor = AppTheme.colors.primary,
                    badgeContentColor = AppTheme.colors.onPrimary
                )
            )
        }
    }
}