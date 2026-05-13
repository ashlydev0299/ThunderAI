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
    private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    private val PROACTIVE_MESSAGES_KEY = booleanPreferencesKey("proactive_messages")
    private val LOCATION_ACCESS_KEY = booleanPreferencesKey("location_access")
    private val IN_APP_NOTIFICATIONS_KEY = booleanPreferencesKey("in_app_notifications")
    private val POPUP_NOTIFICATIONS_KEY = booleanPreferencesKey("popup_notifications")

    // Tema
    fun getDarkMode(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[DARK_MODE_KEY] ?: false }
    }

    suspend fun saveDarkMode(context: Context, isDark: Boolean) {
        context.dataStore.edit { settings -> settings[DARK_MODE_KEY] = isDark }
    }

    // Nombre de usuario
    fun getUserName(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences -> preferences[USER_NAME_KEY] ?: "Usuario" }
    }

    suspend fun saveUserName(context: Context, name: String) {
        context.dataStore.edit { settings -> settings[USER_NAME_KEY] = name }
    }

    // Tamaño de fuente
    fun getFontSize(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences -> preferences[FONT_SIZE_KEY] ?: 14 }
    }

    suspend fun saveFontSize(context: Context, size: Int) {
        context.dataStore.edit { settings -> settings[FONT_SIZE_KEY] = size }
    }

    // Notificaciones
    fun getNotificationsEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[NOTIFICATIONS_ENABLED_KEY] ?: true }
    }

    suspend fun saveNotificationsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { settings -> settings[NOTIFICATIONS_ENABLED_KEY] = enabled }
    }

    // Mensajes proactivos
    fun getProactiveMessages(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[PROACTIVE_MESSAGES_KEY] ?: true }
    }

    suspend fun saveProactiveMessages(context: Context, enabled: Boolean) {
        context.dataStore.edit { settings -> settings[PROACTIVE_MESSAGES_KEY] = enabled }
    }

    // Acceso a ubicación
    fun getLocationAccess(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[LOCATION_ACCESS_KEY] ?: false }
    }

    suspend fun saveLocationAccess(context: Context, enabled: Boolean) {
        context.dataStore.edit { settings -> settings[LOCATION_ACCESS_KEY] = enabled }
    }

    // Notificaciones en la app
    fun getInAppNotifications(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[IN_APP_NOTIFICATIONS_KEY] ?: true }
    }

    suspend fun saveInAppNotifications(context: Context, enabled: Boolean) {
        context.dataStore.edit { settings -> settings[IN_APP_NOTIFICATIONS_KEY] = enabled }
    }

    // Notificaciones emergentes
    fun getPopupNotifications(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[POPUP_NOTIFICATIONS_KEY] ?: false }
    }

    suspend fun savePopupNotifications(context: Context, enabled: Boolean) {
        context.dataStore.edit { settings -> settings[POPUP_NOTIFICATIONS_KEY] = enabled }
    }
}