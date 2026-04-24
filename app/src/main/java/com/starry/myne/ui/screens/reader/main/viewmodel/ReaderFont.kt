package com.starry.myne.ui.screens.reader.main.viewmodel

import androidx.annotation.Keep
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.starry.myne.R
import com.starry.myne.ui.theme.poppinsFont

@Keep
sealed class ReaderFont(val id: String, val name: String, val fontFamily: FontFamily) {

    companion object {
        private val fontMap by lazy {
            ReaderFont::class.sealedSubclasses
                .mapNotNull { it.objectInstance }
                .associateBy { it.id }
        }

        fun getAllFonts() = fontMap.values.toList()
        fun getFontById(id: String) = fontMap[id]!!
        fun getFontByName(name: String) = getAllFonts().find { it.name == name }!!
    }

    @Keep
    data object System : ReaderFont("system", "System Default", FontFamily.Default)

    @Keep
    data object Serif : ReaderFont("serif", "Serif", FontFamily.Serif)

    @Keep
    data object Cursive : ReaderFont("cursive", "Cursive", FontFamily.Cursive)

    @Keep
    data object SansSerif : ReaderFont("sans-serif", "SansSerif", FontFamily.SansSerif)

    @Keep
    data object Inter : ReaderFont("inter", "Inter", FontFamily(Font(R.font.reader_inter_font)))

    @Keep
    data object Dyslexic :
        ReaderFont("dyslexic", "OpenDyslexic", FontFamily(Font(R.font.reader_inter_font)))

    @Keep
    data object Lora : ReaderFont("poppins", "Poppins", poppinsFont)
}
