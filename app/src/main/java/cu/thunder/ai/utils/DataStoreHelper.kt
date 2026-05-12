package cu.thunder.ai.utils

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "thunderai_settings")

object DataStoreHelper {

    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val USER_NAME_KEY = stringPreferencesKey("user_name")
    private val FONT_SIZE_KEY = intPreferencesKey("font_size")

    fun getDarkMode(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[DARK_MODE_KEY] ?: false }
    }

    suspend fun saveDarkMode(context: Context, isDark: Boolean) {
        context.dataStore.edit { settings -> settings[DARK_MODE_KEY] = isDark }
    }

    fun getUserName(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences -> preferences[USER_NAME_KEY] ?: "Usuario" }
    }

    suspend fun saveUserName(context: Context, name: String) {
        context.dataStore.edit { settings -> settings[USER_NAME_KEY] = name }
    }

    fun getFontSize(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences -> preferences[FONT_SIZE_KEY] ?: 14 }
    }

    suspend fun saveFontSize(context: Context, size: Int) {
        context.dataStore.edit { settings -> settings[FONT_SIZE_KEY] = size }
    }
}