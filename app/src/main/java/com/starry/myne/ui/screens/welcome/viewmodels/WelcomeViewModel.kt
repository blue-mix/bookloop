package com.starry.myne.ui.screens.welcome.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starry.myne.helpers.PreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val welcomeDataStore: WelcomeDataStore,
    private val preferenceUtil: PreferenceUtil
) : ViewModel() {

    fun saveOnBoardingState(completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            welcomeDataStore.saveOnBoardingState(completed = completed)
        }
    }


    fun setInternalReaderSetting(newValue: Boolean) {
        preferenceUtil.putBoolean(PreferenceUtil.INTERNAL_READER_BOOL, newValue)
    }

    fun getInternalReaderSetting() = preferenceUtil.getBoolean(
        PreferenceUtil.INTERNAL_READER_BOOL, true
    )
}