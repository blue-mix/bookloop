package com.starry.myne.ui.screens.settings.composables

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.starry.myne.MainActivity
import com.starry.myne.R
import com.starry.myne.helpers.Utils
import com.starry.myne.helpers.getActivity
import com.starry.myne.ui.common.CustomTopAppBar
import com.starry.myne.ui.navigation.Screens
import com.starry.myne.ui.screens.main.bottomNavPadding
import com.starry.myne.ui.screens.settings.viewmodels.SettingsViewModel
import com.starry.myne.ui.screens.settings.viewmodels.ThemeMode
import custom.AppTheme
import custom.components.Text
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel = (context.getActivity() as MainActivity).settingsViewModel

    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.padding(bottom = bottomNavPadding),
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            CustomTopAppBar(
                headerText = stringResource(id = R.string.settings_header),
                iconRes = R.drawable.ic_nav_settings
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                //SettingsCard()
                GeneralOptionsUI(viewModel = viewModel, snackBarHostState = snackBarHostState)
                DisplayOptionsUI(viewModel = viewModel, snackBarHostState = snackBarHostState)
                InformationUI(navController = navController)
            }
        }
    }
}

@Composable
private fun GeneralOptionsUI(
    viewModel: SettingsViewModel,
    snackBarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val googleBooksApiSwitchState = remember { mutableStateOf(viewModel.getUseGoogleApiValue()) }
    val internalReaderValue = when (viewModel.getInternalReaderValue()) {
        true -> stringResource(id = R.string.reader_option_inbuilt)
        false -> stringResource(id = R.string.reader_option_external)
    }
    val showReaderDialog = remember { mutableStateOf(false) }
    val radioOptions = listOf(
        stringResource(id = R.string.reader_option_inbuilt),
        stringResource(id = R.string.reader_option_external)
    )
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(internalReaderValue) }

    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .padding(top = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.general_settings_header),
            color = AppTheme.colors.onBackground.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        SettingItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_settings_reader),
            mainText = stringResource(id = R.string.default_reader_setting),
            subText = internalReaderValue,
            onClick = { showReaderDialog.value = true }
        )
        SettingItem(
            icon = Icons.Filled.Language,
            mainText = stringResource(id = R.string.default_locale_setting),
            subText = stringResource(id = R.string.default_locale_setting_desc),
            onClick = {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Utils.isMiui()) {
                    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                } else {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            context.getString(R.string.locale_setting_not_found)
                        )
                    }
                }
            }
        )
        SettingItemWIthSwitch(
            icon = ImageVector.vectorResource(id = R.drawable.ic_settings_google_api),
            mainText = stringResource(id = R.string.google_books_api_setting),
            subText = stringResource(id = R.string.google_books_api_setting_desc),
            switchState = googleBooksApiSwitchState,
            onCheckChange = {
                viewModel.setUseGoogleApiValue(it)
                googleBooksApiSwitchState.value = it
            }
        )
    }

    if (showReaderDialog.value) {
        AlertDialog(containerColor = AppTheme.colors.background, onDismissRequest = {
            showReaderDialog.value = false
        }, title = {
            Text(
                text = stringResource(id = R.string.default_reader_dialog_title),
                color = AppTheme.colors.onSurface,
            )
        }, text = {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.Center,
            ) {
                radioOptions.forEach { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .selectable(
                                selected = (text == selectedOption),
                                onClick = { onOptionSelected(text) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (text == selectedOption),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppTheme.colors.primary,
                                unselectedColor = AppTheme.colors.disabled,
                                disabledSelectedColor = Color.Black,
                                disabledUnselectedColor = Color.Black
                            ),
                        )
                        Text(
                            text = text,
                            modifier = Modifier.padding(start = 16.dp),
                            color = AppTheme.colors.onSurface,
                        )
                    }
                }
            }
        }, confirmButton = {
            FilledTonalButton(
                onClick = {
                    showReaderDialog.value = false

                    when (selectedOption) {
                        context.getString(R.string.reader_option_inbuilt) -> {
                            viewModel.setInternalReaderValue(true)
                        }

                        context.getString(R.string.reader_option_external) -> {
                            viewModel.setInternalReaderValue(false)
                        }
                    }
                }, colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AppTheme.colors.primary,
                    contentColor = AppTheme.colors.onPrimary
                )
            ) {
                Text(stringResource(id = R.string.confirm))
            }
        }, dismissButton = {
            TextButton(onClick = {
                showReaderDialog.value = false
            }) {
                Text(stringResource(id = R.string.cancel))
            }
        })
    }
}


@Composable
private fun DisplayOptionsUI(
    viewModel: SettingsViewModel,
    snackBarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Display settings for the theme
    val displayValue =
        when (viewModel.getThemeValue()) {
            ThemeMode.Light.ordinal -> stringResource(id = R.string.theme_option_light)
            ThemeMode.Dark.ordinal -> stringResource(id = R.string.theme_option_dark)
            else -> stringResource(id = R.string.theme_option_system)
        }
    val displayDialog = remember { mutableStateOf(false) }
    val radioOptions = listOf(
        stringResource(id = R.string.theme_option_light),
        stringResource(id = R.string.theme_option_dark),
        stringResource(id = R.string.theme_option_system)
    )
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(displayValue) }

    // Display settings for the amoled theme
    val amoledSwitch = remember { mutableStateOf(viewModel.getAmoledThemeValue()) }
    val amoledDesc = if (amoledSwitch.value) {
        stringResource(id = R.string.amoled_theme_setting_enabled_desc)
    } else {
        stringResource(id = R.string.amoled_theme_setting_disabled_desc)
    }

    // Display settings for the Material You theme
    val materialYouSwitch = remember { mutableStateOf(viewModel.getMaterialYouValue()) }
    val materialYouDesc = if (materialYouSwitch.value) {
        stringResource(id = R.string.material_you_setting_enabled_desc)
    } else {
        stringResource(id = R.string.material_you_setting_disabled_desc)
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .padding(top = 2.dp)
    ) {
        Text(
            text = stringResource(id = R.string.display_setting_header),
            color = AppTheme.colors.onBackground.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        SettingItem(
            icon = Icons.Filled.BrightnessMedium,
            mainText = stringResource(id = R.string.theme_setting),
            subText = displayValue,
            onClick = { displayDialog.value = true }
        )
        SettingItemWIthSwitch(
            icon = Icons.Filled.Contrast,
            mainText = stringResource(id = R.string.amoled_theme_setting),
            subText = amoledDesc,
            switchState = amoledSwitch,
            onCheckChange = {
                viewModel.setAmoledTheme(it)
                amoledSwitch.value = it
            })
        SettingItemWIthSwitch(
            icon = Icons.Filled.Palette,
            mainText = stringResource(id = R.string.material_you_setting),
            subText = materialYouDesc,
            switchState = materialYouSwitch,
            onCheckChange = { materialYouValue ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    viewModel.setMaterialYou(materialYouValue)
                    materialYouSwitch.value = materialYouValue
                } else {
                    viewModel.setMaterialYou(false)
                    materialYouSwitch.value = false
                    coroutineScope.launch { snackBarHostState.showSnackbar(context.getString(R.string.material_you_error)) }
                }
            }
        )
    }

    if (displayDialog.value) {
        AlertDialog(containerColor = AppTheme.colors.background, onDismissRequest = {
            displayDialog.value = false
        }, title = {
            Text(
                text = stringResource(id = R.string.theme_dialog_title),
                color = AppTheme.colors.onSurface,
            )
        }, text = {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.Center,
            ) {
                radioOptions.forEach { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .selectable(
                                selected = (text == selectedOption),
                                onClick = { onOptionSelected(text) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (text == selectedOption),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppTheme.colors.primary,
                                unselectedColor = AppTheme.colors.disabled,
                                disabledSelectedColor = Color.Black,
                                disabledUnselectedColor = Color.Black
                            ),
                        )
                        Text(
                            text = text,
                            modifier = Modifier.padding(start = 16.dp),
                            color = AppTheme.colors.onSurface,
                        )
                    }
                }
            }
        }, confirmButton = {
            FilledTonalButton(
                onClick = {
                    displayDialog.value = false

                    when (selectedOption) {
                        context.getString(R.string.theme_option_light) -> {
                            viewModel.setTheme(ThemeMode.Light)
                        }

                        context.getString(R.string.theme_option_dark) -> {
                            viewModel.setTheme(ThemeMode.Dark)
                        }

                        context.getString(R.string.theme_option_system) -> {
                            viewModel.setTheme(ThemeMode.Auto)
                        }
                    }
                }, colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AppTheme.colors.background,
                    contentColor = AppTheme.colors.surface
                )
            ) {
                Text(stringResource(id = R.string.theme_dialog_apply_button))
            }
        }, dismissButton = {
            TextButton(onClick = {
                displayDialog.value = false
            }) {
                Text(stringResource(id = R.string.cancel))
            }
        })
    }
}

@Composable
private fun InformationUI(navController: NavController) {
    Box {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .padding(top = 2.dp)
        ) {
            Text(
                text = stringResource(id = R.string.miscellaneous_setting_header),
                color = AppTheme.colors.onBackground.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SettingItem(
                icon = Icons.Filled.LocalPolice,
                mainText = stringResource(id = R.string.license_setting),
                subText = stringResource(id = R.string.license_setting_desc),
                onClick = { navController.navigate(Screens.OSLScreen.route) }
            )

        }
    }

}


@Composable
@Preview
fun SettingsScreenPreview() {
    SettingsScreen(rememberNavController())
}