package fumi.day.literalgallery.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalgallery.data.prefs.UserPrefs
import fumi.day.literalgallery.data.prefs.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    prefs: UserPreferences
) : ViewModel() {
    val userPrefs: StateFlow<UserPrefs> = prefs.prefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPrefs())
}
