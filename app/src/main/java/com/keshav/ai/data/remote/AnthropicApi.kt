package com.keshav.ai.data.remote

import com.keshav.ai.domain.model.PromptMessage
import com.keshav.ai.domain.model.StreamEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class AnthropicRemote(private val baseUrl: String, private val apiKey: String, private val model: String = "claude-sonnet-4-5") {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation)
        install(HttpTimeout) { requestTimeoutMillis = 120_000; connectTimeoutMillis = 20_000; socketTimeoutMillis = 120_000 }
    }
    fun stream(messages: List<PromptMessage>, systemPrompt: String? = null): Flow<StreamEvent> = flow {
        if (apiKey.isBlank()) { emit(StreamEvent.Error("Add your Anthropic API key in Settings first.")); return@flow }
        try {
            client.sse(urlString = "${baseUrl.trimEnd('/')}/v1/messages", request = {
                method = io.ktor.http.HttpMethod.Post; contentType(ContentType.Application.Json)
                header("x-api-key", apiKey); header("anthropic-version", "2023-06-01"); header("accept", "text/event-stream")
                setBody(buildJsonObject {
                    put("model", model); put("max_tokens", 4096); put("stream", true)
                    if (!systemPrompt.isNullOrBlank()) put("system", systemPrompt)
                    putJsonArray("messages") {
                        messages.forEach { message -> add(buildJsonObject {
                            put("role", if (message.role.name == "USER") "user" else "assistant")
                            if (message.attachments.isEmpty()) put("content", message.content) else putJsonArray("content") {
                                if (message.content.isNotBlank()) add(buildJsonObject { put("type", "text"); put("text", message.content) })
                                message.attachments.forEach { a -> add(buildJsonObject { put("type", "image"); putJsonObject("source") { put("type", "base64"); put("media_type", a.mimeType); put("data", a.base64) } }) }
                            }
                        }) }
                    }
                }.toString())
            }) {
                incoming.collect { event ->
                    val data = event.data ?: return@collect
                    when (event.event) {
                        "content_block_delta" -> {
                            val text = runCatching {
                                val delta = Json.parseToJsonElement(data).jsonObject["delta"]?.jsonObject
                                if (delta?.get("type")?.toString()?.contains("text_delta") == true) delta["text"]?.toString()?.trim('"') else null
                            }.getOrNull()
                            if (!text.isNullOrEmpty()) emit(StreamEvent.TextDelta(text))
                        }
                        "message_stop" -> emit(StreamEvent.MessageCompleted)
                        "error" -> emit(StreamEvent.Error(parseError(data), retryable = true))
                    }
                }
            }
        } catch (t: CancellationException) { throw t }
        catch (t: Throwable) { emit(StreamEvent.Error(t.message ?: "Network request failed", retryable = true)) }
    }
    private fun parseError(data: String): String = runCatching { Json.parseToJsonElement(data).jsonObject["error"]?.jsonObject?.get("message")?.toString()?.trim('"') ?: data }.getOrDefault(data)
    fun close() = client.close()
}
