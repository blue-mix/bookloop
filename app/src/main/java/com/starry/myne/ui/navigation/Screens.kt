package com.starry.myne.ui.navigation

const val BOOK_ID_ARG_KEY = "bookId"
const val LIBRARY_ITEM_ID_ARG_KEY = "libraryItemId"
const val CATEGORY_DETAIL_ARG_KEY = "category"

sealed class Screens(val route: String) {

    data object BookDetailScreen : Screens("book_detail_screen/{$BOOK_ID_ARG_KEY}") {
        fun withBookId(id: String): String {
            return this.route.replace("{$BOOK_ID_ARG_KEY}", id)
        }
    }

    data object CategoryDetailScreen :
        Screens("category_detail_screen/{$CATEGORY_DETAIL_ARG_KEY}") {
        fun withCategory(category: String): String {
            return this.route.replace("{$CATEGORY_DETAIL_ARG_KEY}", category)
        }
    }

    data object ReaderDetailScreen : Screens("reader_detail_screen/{$LIBRARY_ITEM_ID_ARG_KEY}") {
        fun withLibraryItemId(id: String): String {
            return this.route.replace("{$LIBRARY_ITEM_ID_ARG_KEY}", id)
        }
    }

    data object WelcomeScreen : Screens("welcome_screen")

    data object OSLScreen : Screens("osl_screen")

    data object AboutScreen : Screens("about_screen")
}
