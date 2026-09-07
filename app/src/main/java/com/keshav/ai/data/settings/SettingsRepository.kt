package com.keshav.ai.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.keshavDataStore by preferencesDataStore(name = "keshav_settings")

data class AppSettings(
    val endpoint: String = "https://api.anthropic.com",
    val model: String = "claude-sonnet-4-5",
    val darkMode: Boolean = true,
    val agentMode: Boolean = false,
    val responseMode: String = "normal"
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val endpoint = stringPreferencesKey("endpoint")
        val model = stringPreferencesKey("model")
        val darkMode = booleanPreferencesKey("dark_mode")
        val agentMode = booleanPreferencesKey("agent_mode")
        val responseMode = stringPreferencesKey("response_mode")
    }

    val settings: Flow<AppSettings> = context.keshavDataStore.data.map { p ->
        AppSettings(
            endpoint = p[Keys.endpoint] ?: AppSettings().endpoint,
            model = p[Keys.model] ?: AppSettings().model,
            darkMode = p[Keys.darkMode] ?: true,
            agentMode = p[Keys.agentMode] ?: false,
            responseMode = p[Keys.responseMode] ?: "normal"
        )
    }

    suspend fun update(endpoint: String, model: String, darkMode: Boolean, agentMode: Boolean, responseMode: String) {
        context.keshavDataStore.edit { p ->
            p[Keys.endpoint] = endpoint.trim().trimEnd('/')
            p[Keys.model] = model.trim()
            p[Keys.darkMode] = darkMode
            p[Keys.agentMode] = agentMode
            p[Keys.responseMode] = responseMode.lowercase().trim().ifBlank { "normal" }
        }
    }
}
