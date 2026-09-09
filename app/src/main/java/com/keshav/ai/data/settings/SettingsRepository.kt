package com.keshav.ai.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.keshavDataStore by preferencesDataStore(name = "keshav_settings")

private const val AGENT_ROUTER_ENDPOINT = "https://co.agentrouter.org"
private const val AGENT_ROUTER_MODEL = "claude-opus-4-8"

data class AppSettings(
    val endpoint: String = AGENT_ROUTER_ENDPOINT,
    val model: String = AGENT_ROUTER_MODEL,
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
        val storedEndpoint = p[Keys.endpoint].orEmpty().trim().trimEnd('/')
        val storedModel = p[Keys.model].orEmpty().trim()
        // Older Keshav builds stored Anthropic's direct endpoint. Migrate it so an
        // existing installation does not keep silently calling the wrong provider.
        val endpoint = when {
            storedEndpoint.isBlank() || storedEndpoint == "https://api.anthropic.com" -> AGENT_ROUTER_ENDPOINT
            else -> storedEndpoint
        }
        val model = when {
            storedModel.isBlank() || storedModel == "claude-sonnet-4-5" -> AGENT_ROUTER_MODEL
            else -> storedModel
        }
        AppSettings(
            endpoint = endpoint,
            model = model,
            darkMode = p[Keys.darkMode] ?: true,
            agentMode = p[Keys.agentMode] ?: false,
            responseMode = p[Keys.responseMode] ?: "normal"
        )
    }

    suspend fun update(endpoint: String, model: String, darkMode: Boolean, agentMode: Boolean, responseMode: String) {
        context.keshavDataStore.edit { p ->
            p[Keys.endpoint] = endpoint.trim().trimEnd('/').ifBlank { AGENT_ROUTER_ENDPOINT }
            p[Keys.model] = model.trim().ifBlank { AGENT_ROUTER_MODEL }
            p[Keys.darkMode] = darkMode
            p[Keys.agentMode] = agentMode
            p[Keys.responseMode] = responseMode.lowercase().trim().ifBlank { "normal" }
        }
    }
}
