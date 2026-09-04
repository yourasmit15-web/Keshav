package com.keshav.ai.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keshav.ai.domain.model.ChatRole
import kotlinx.coroutines.launch

@Composable
fun KeshavRoot(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            Column(Modifier.padding(24.dp)) {
                Text("Keshav", modifier = Modifier.padding(bottom = 20.dp))
                Button(onClick = { viewModel.newChat(); scope.launch { drawer.close() } }) { Text("New Chat") }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { scope.launch { drawer.open() } }) { Text("☰") }
                Text("Keshav", modifier = Modifier.padding(top = 12.dp))
                Button(onClick = { viewModel.stop() }) { Text("Stop") }
            }

            if (messages.isEmpty()) {
                Column(Modifier.fillMaxSize().weight(1f), verticalArrangement = Arrangement.Center) {
                    Text("Hello, I am Keshav. How can I help you today?")
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(messages, key = { it.id }) { message ->
                        Text(if (message.role == ChatRole.USER) "You: ${message.content}" else "Keshav: ${message.content}")
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message Keshav…") }
                )
                Button(onClick = { viewModel.send(input); input = "" }) { Text("Send") }
            }
        }
    }
}
