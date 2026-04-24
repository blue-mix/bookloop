package com.starry.myne.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import com.starry.myne.R
import com.starry.myne.helpers.highlights.HighlightColor

private const val MENU_ITEM_COPY = 0
private const val MENU_ITEM_SHARE = 1
private const val MENU_ITEM_WEB = 2
private const val MENU_ITEM_TRANSLATE = 3
private const val MENU_ITEM_DICTIONARY = 4
private const val MENU_ITEM_HI_YELLOW = 10
private const val MENU_ITEM_HI_GREEN = 11
private const val MENU_ITEM_HI_BLUE = 12
private const val MENU_ITEM_HI_PINK = 13

private class MyneTextActionModeCallback(
    private val context: Context,
    var rect: Rect = Rect.Zero,
    var onCopyRequested: (() -> Unit)? = null,
    var onShareRequested: (() -> Unit)? = null,
    var onWebSearchRequested: (() -> Unit)? = null,
    var onTranslateRequested: (() -> Unit)? = null,
    var onDictionaryRequested: (() -> Unit)? = null,
    var onHighlightColorRequested: ((HighlightColor) -> Unit)? = null
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        requireNotNull(menu)
        requireNotNull(mode)

        onCopyRequested?.let {
            menu.add(0, MENU_ITEM_COPY, 0, context.getString(R.string.copy))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        onShareRequested?.let {
            menu.add(0, MENU_ITEM_SHARE, 1, context.getString(R.string.share))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        onWebSearchRequested?.let {
            menu.add(0, MENU_ITEM_WEB, 2, context.getString(R.string.web_search))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        onTranslateRequested?.let {
            menu.add(0, MENU_ITEM_TRANSLATE, 3, context.getString(R.string.translate))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        onDictionaryRequested?.let {
            menu.add(0, MENU_ITEM_DICTIONARY, 4, context.getString(R.string.dictionary))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        onHighlightColorRequested?.let {
            // 🖍️ Highlight color variants as separate items
            menu.add(0, MENU_ITEM_HI_YELLOW, 6, context.getString(R.string.highlight_yellow))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.add(0, MENU_ITEM_HI_GREEN, 7, context.getString(R.string.highlight_green))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.add(0, MENU_ITEM_HI_BLUE, 8, context.getString(R.string.highlight_blue))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.add(0, MENU_ITEM_HI_PINK, 9, context.getString(R.string.highlight_pink))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item!!.itemId) {
            MENU_ITEM_COPY -> onCopyRequested?.invoke()
            MENU_ITEM_SHARE -> onShareRequested?.invoke()
            MENU_ITEM_WEB -> onWebSearchRequested?.invoke()
            MENU_ITEM_TRANSLATE -> onTranslateRequested?.invoke()
            MENU_ITEM_DICTIONARY -> onDictionaryRequested?.invoke()
            MENU_ITEM_HI_YELLOW -> onHighlightColorRequested?.invoke(HighlightColor.YELLOW)
            MENU_ITEM_HI_GREEN -> onHighlightColorRequested?.invoke(HighlightColor.GREEN)
            MENU_ITEM_HI_BLUE -> onHighlightColorRequested?.invoke(HighlightColor.BLUE)
            MENU_ITEM_HI_PINK -> onHighlightColorRequested?.invoke(HighlightColor.PINK)
            else -> return false
        }
        mode?.finish()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {}
}

private class FloatingTextActionModeCallback(
    val callback: MyneTextActionModeCallback
) : ActionMode.Callback2() {
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return callback.onActionItemClicked(mode, item)
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return callback.onCreateActionMode(mode, menu)
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return callback.onPrepareActionMode(mode, menu)
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        callback.onDestroyActionMode(mode)
    }

    override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: android.graphics.Rect?) {
        val rect = callback.rect
        outRect?.set(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
    }
}

@Suppress("DEPRECATION")
private class MyneSelectionToolbar(
    private val view: View,
    context: Context,
    private val onCopyRequest: (() -> Unit)?,
    private val onShareRequest: ((String) -> Unit)?,
    private val onWebSearchRequest: ((String) -> Unit)?,
    private val onTranslateRequest: ((String) -> Unit)?,
    private val onDictionaryRequest: ((String) -> Unit)?,
    private val onHighlightRequest: ((String, HighlightColor) -> Unit)? // NEW
) : TextToolbar {
    private var actionMode: ActionMode? = null
    private val callback = MyneTextActionModeCallback(context = context)

    val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override var status: TextToolbarStatus by mutableStateOf(TextToolbarStatus.Hidden)

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        callback.rect = rect

        fun <T> withSelectionThenRestore(block: (String) -> T): T? {
            val previous = clipboardManager.primaryClip
            onCopyRequested?.invoke() // copies current selection into clipboard
            val selection = clipboardManager.text?.toString().orEmpty()
            val result = block(selection)
            if (previous != null) clipboardManager.setPrimaryClip(previous)
            else clipboardManager.setPrimaryClip(ClipData.newPlainText(null, " "))
            return result
        }

        callback.onCopyRequested = {
            onCopyRequested?.invoke()
            onCopyRequest?.invoke()
        }
        callback.onShareRequested = {
            withSelectionThenRestore { selected -> onShareRequest?.invoke(selected) }
        }
        callback.onWebSearchRequested = {
            withSelectionThenRestore { selected -> onWebSearchRequest?.invoke(selected) }
        }
        callback.onTranslateRequested = {
            withSelectionThenRestore { selected -> onTranslateRequest?.invoke(selected) }
        }
        callback.onDictionaryRequested = {
            withSelectionThenRestore { selected -> onDictionaryRequest?.invoke(selected) }
        }

        callback.onHighlightColorRequested = onHighlightRequest?.let { handler ->
            { color -> withSelectionThenRestore { selected -> handler.invoke(selected, color) } }
        }

        if (actionMode == null) {
            status = TextToolbarStatus.Shown
            actionMode = view.startActionMode(
                FloatingTextActionModeCallback(callback),
                ActionMode.TYPE_FLOATING
            )
        } else {
            actionMode?.invalidate()
        }
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        actionMode?.finish()
        actionMode = null
    }
}

@Composable
fun MyneSelectionContainer(
    onCopyRequested: (() -> Unit),
    onShareRequested: ((String) -> Unit),
    onWebSearchRequested: ((String) -> Unit),
    onTranslateRequested: ((String) -> Unit),
    onDictionaryRequested: ((String) -> Unit),
    onHighlightRequested: ((String, HighlightColor) -> Unit)? = null, // NEW (optional)
    content: @Composable (toolbarHidden: Boolean) -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current

    val myneSelectionToolbar = remember {
        MyneSelectionToolbar(
            view = view,
            context = context,
            onCopyRequest = { onCopyRequested() },
            onShareRequest = { onShareRequested(it) },
            onWebSearchRequest = { onWebSearchRequested(it) },
            onTranslateRequest = { onTranslateRequested(it) },
            onDictionaryRequest = { onDictionaryRequested(it) },
            onHighlightRequest = onHighlightRequested
        )
    }
    val isToolbarHidden = remember(myneSelectionToolbar.status) {
        derivedStateOf { myneSelectionToolbar.status == TextToolbarStatus.Hidden }
    }

    CompositionLocalProvider(LocalTextToolbar provides myneSelectionToolbar) {
        SelectionContainer(Modifier.fillMaxSize()) {
            content(isToolbarHidden.value)
        }
    }
}
