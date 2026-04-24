package com.starry.myne.ui.screens.categories.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.starry.myne.R
import com.starry.myne.helpers.weakHapticFeedback
import com.starry.myne.ui.common.CustomTopAppBar
import com.starry.myne.ui.navigation.Screens
import com.starry.myne.ui.screens.main.bottomNavPadding
import custom.AppTheme
import custom.components.Text
import custom.components.card.CardDefaults
import custom.components.card.OutlinedCard

@Composable
fun CategoriesScreen(navController: NavController) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        topBar = {
            CustomTopAppBar(
                headerText = stringResource(id = R.string.categories_header),
                iconRes = R.drawable.ic_nav_categories
            )
        }, content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background)
                    .padding(paddingValues)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(168.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 8.dp, bottom = bottomNavPadding),
                    content = {
                        items(BookCategories.ALL.size) { idx ->
                            val category = BookCategories.ALL[idx]
                            CategoriesItem(
                                category = stringResource(id = category.nameRes),
                                onClick = {
                                    navController.navigate(
                                        Screens.CategoryDetailScreen.withCategory(category.category)
                                    )
                                }
                            )
                        }
                    },
                )
            }
        })

}


@Composable
private fun CategoriesItem(category: String, onClick: () -> Unit) {
    val view = LocalView.current
    OutlinedCard(
        modifier = Modifier
            .height(90.dp)
            .width(160.dp)
            .padding(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.background
        ),
        shape = RoundedCornerShape(6.dp),
        onClick = {
            view.weakHapticFeedback()
            onClick()
        }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                modifier = Modifier.padding(2.dp),
                text = category,
                fontSize = 17.sp,
                fontStyle = AppTheme.typography.h2.fontStyle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AppTheme.colors.onBackground,
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
@Preview(showBackground = true)
fun CategoriesScreenPreview() {
    CategoriesScreen(rememberNavController())
}

