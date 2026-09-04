package com.keshav.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.keshav.ai.data.local.KeshavDatabase
import com.keshav.ai.data.remote.AnthropicRemote
import com.keshav.ai.data.repository.ChatRepositoryImpl
import com.keshav.ai.presentation.ChatViewModel
import com.keshav.ai.presentation.KeshavRoot
import com.keshav.ai.presentation.ui.KeshavTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(applicationContext, KeshavDatabase::class.java, "keshav.db").build()
        val repository = ChatRepositoryImpl(database.chatDao())
        val remote = AnthropicRemote("https://api.anthropic.com", "")
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(repository, remote) as T
        }
        val viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        setContent { KeshavTheme { KeshavRoot(viewModel) } }
    }
}
