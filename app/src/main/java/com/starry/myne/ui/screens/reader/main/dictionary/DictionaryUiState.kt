package com.starry.myne.ui.screens.reader.main.dictionary

import DictionaryEntry


sealed class DictionaryUiState {
    data object Idle : DictionaryUiState()
    data object Loading : DictionaryUiState()
    data class Success(val entry: DictionaryEntry) : DictionaryUiState()
    data class Error(val message: String) : DictionaryUiState()
}
