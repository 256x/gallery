package fumi.day.literalgallery.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserPrefs(
    val keyColorHex: String = ""
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyColorKey = stringPreferencesKey("key_color")

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(keyColorHex = p[keyColorKey] ?: "")
    }

    suspend fun setKeyColor(hex: String) {
        context.dataStore.edit { it[keyColorKey] = hex }
    }
}
