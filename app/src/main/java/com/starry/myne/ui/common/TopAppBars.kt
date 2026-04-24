package com.starry.myne.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starry.myne.R
import com.starry.myne.helpers.weakHapticFeedback
import custom.AppTheme
import custom.components.HorizontalDivider
import custom.components.Icon
import custom.components.Text


@Composable
fun CustomTopAppBar(headerText: String, iconRes: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = headerText,
                fontSize = 28.sp,
                color = AppTheme.colors.onBackground,
            )
            Icon(
                imageVector = ImageVector.vectorResource(id = iconRes),
                contentDescription = null,
                tint = AppTheme.colors.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }
        HorizontalDivider(
            thickness = 2.dp, color = AppTheme.colors.surface
        )
    }
}

@Composable
fun CustomTopAppBar(headerText: String, onBackButtonClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = headerText,
                modifier = Modifier.padding(bottom = 16.dp),
                color = AppTheme.colors.onBackground,
                fontStyle = AppTheme.typography.h2.fontStyle,
                fontSize = 24.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarActionItem(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack, onclick = onBackButtonClicked
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        HorizontalDivider(
            thickness = 2.dp, color = AppTheme.colors.surface
        )
    }
}

@Composable
fun CustomTopAppBar(
    headerText: String,
    actionIcon: ImageVector,
    onBackButtonClicked: () -> Unit,
    onActionClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = headerText,
                modifier = Modifier.padding(bottom = 16.dp),
                color = AppTheme.colors.onBackground,
                fontStyle = AppTheme.typography.h2.fontStyle,
                fontSize = 24.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarActionItem(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack, onclick = onBackButtonClicked
                )
                Spacer(modifier = Modifier.weight(1f))
                TopBarActionItem(
                    icon = actionIcon, onclick = onActionClicked
                )
            }
        }
        HorizontalDivider(
            thickness = 2.dp, color = AppTheme.colors.surface
        )
    }

}

@Composable
private fun TopBarActionItem(icon: ImageVector, onclick: () -> Unit) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .padding(start = 16.dp, top = 2.dp, bottom = 18.dp, end = 16.dp)
            .clip(CircleShape)
            .background(AppTheme.colors.surface)
            .clickable {
                view.weakHapticFeedback()
                onclick()
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(id = R.string.back_button_desc),
            tint = AppTheme.colors.onSurface,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopAppBarsPV() {
    Column(Modifier.fillMaxSize()) {
        CustomTopAppBar(headerText = "Something", iconRes = R.drawable.ic_nav_categories)
        CustomTopAppBar(headerText = "Something", onBackButtonClicked = {})
        CustomTopAppBar(
            headerText = "Something",
            actionIcon = Icons.Default.Translate,
            onBackButtonClicked = { /*TODO*/ },
            onActionClicked = {})
    }
}


@Composable
fun NewCustomTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    actionIcons: List<Pair<ImageVector, () -> Unit>> = emptyList()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(AppTheme.colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Title (always centered)
            Text(
                text = title,
                fontSize = 22.sp,
                color = AppTheme.colors.onBackground,
                fontStyle = AppTheme.typography.h2.fontStyle,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Left & Right Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBack && onBackClick != null) {
                    NewTopBarActionItem(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        onClick = onBackClick
                    )
                } else {
                    Spacer(Modifier.size(44.dp)) // keep spacing symmetric if no back
                }

                Spacer(Modifier.weight(1f))

                actionIcons.forEach { (icon, onClick) ->
                    NewTopBarActionItem(icon = icon, onClick = onClick)
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.surface)
    }
}

@Composable
private fun NewTopBarActionItem(icon: ImageVector, onClick: () -> Unit) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable {
                view.weakHapticFeedback()
                onClick()
            }
            .background(AppTheme.colors.surface),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(id = R.string.back_button_desc),
            tint = AppTheme.colors.onSurface,
            modifier = Modifier.size(22.dp)
        )
    }
}
