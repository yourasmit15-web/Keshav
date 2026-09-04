package com.keshav.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.keshav.ai.data.repository.ChatRepositoryImpl
import com.keshav.ai.data.settings.ApiKeyStore
import com.keshav.ai.data.settings.SettingsRepository
import com.keshav.ai.presentation.ChatViewModel
import com.keshav.ai.presentation.KeshavRoot
import com.keshav.ai.presentation.ui.KeshavTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as KeshavApplication
        val repository = ChatRepositoryImpl(app.database.chatDao())
        val settings = SettingsRepository(applicationContext)
        val keyStore = ApiKeyStore(applicationContext)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(repository, settings, keyStore, contentResolver) as T
        }
        val viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]
        setContent {
            val appSettings by viewModel.settings.collectAsState()
            KeshavTheme(darkTheme = appSettings.darkMode) { KeshavRoot(viewModel) }
        }
    }
}
