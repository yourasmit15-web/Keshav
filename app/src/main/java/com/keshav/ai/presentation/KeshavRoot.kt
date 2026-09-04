package com.keshav.ai.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.keshav.ai.R
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.keshav_logo),
                        contentDescription = "Keshav logo",
                        modifier = Modifier.size(48.dp)
                    )
                    Text("keshav", modifier = Modifier.padding(start = 12.dp))
                }
                Button(
                    onClick = { viewModel.newChat(); scope.launch { drawer.close() } },
                    modifier = Modifier.padding(top = 20.dp)
                ) { Text("New Chat") }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { scope.launch { drawer.open() } }) { Text("☰") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.keshav_logo),
                        contentDescription = "Keshav logo",
                        modifier = Modifier.size(34.dp)
                    )
                    Text("keshav", modifier = Modifier.padding(start = 8.dp))
                }
                Button(onClick = { viewModel.stop() }) { Text("Stop") }
            }

            if (messages.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.keshav_logo),
                        contentDescription = "Keshav AI logo",
                        modifier = Modifier.size(140.dp)
                    )
                    Text("keshav", modifier = Modifier.padding(top = 12.dp))
                    Text("Your AI assistant", modifier = Modifier.padding(top = 4.dp))
                    Text("How can I help you today?", modifier = Modifier.padding(top = 16.dp))
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
                    placeholder = { Text("Message keshav…") }
                )
                Button(onClick = { viewModel.send(input); input = "" }) { Text("Send") }
            }
        }
    }
}
