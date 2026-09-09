package com.keshav.ai.data.remote

import com.keshav.ai.domain.model.PromptMessage
import com.keshav.ai.domain.model.StreamEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.call.bodyAsText
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class AnthropicRemote(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String = "claude-opus-4-8"
) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation)
        install(SSE)
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 120_000
        }
    }

    fun stream(messages: List<PromptMessage>, systemPrompt: String? = null): Flow<StreamEvent> = flow {
        if (apiKey.isBlank()) {
            emit(StreamEvent.Error("AgentRouter API key is missing. Open Settings and add a valid key."))
            return@flow
        }

        val normalizedBase = baseUrl.trim().trimEnd('/').removeSuffix("/v1")
        if (normalizedBase.isBlank()) {
            emit(StreamEvent.Error("AI endpoint is empty. Use https://co.agentrouter.org for AgentRouter."))
            return@flow
        }

        try {
            client.sse(urlString = "$normalizedBase/v1/messages", request = {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                header("accept", "text/event-stream")
                setBody(buildJsonObject {
                    put("model", model.ifBlank { "claude-opus-4-8" })
                    put("max_tokens", 4096)
                    put("stream", true)
                    if (!systemPrompt.isNullOrBlank()) put("system", systemPrompt)
                    putJsonArray("messages") {
                        messages.forEach { message ->
                            add(buildJsonObject {
                                put("role", if (message.role.name == "USER") "user" else "assistant")
                                if (message.attachments.isEmpty()) {
                                    put("content", message.content)
                                } else {
                                    putJsonArray("content") {
                                        if (message.content.isNotBlank()) add(buildJsonObject {
                                            put("type", "text")
                                            put("text", message.content)
                                        })
                                        message.attachments.forEach { a ->
                                            add(buildJsonObject {
                                                put("type", "image")
                                                putJsonObject("source") {
                                                    put("type", "base64")
                                                    put("media_type", a.mimeType)
                                                    put("data", a.base64)
                                                }
                                            })
                                        }
                                    }
                                }
                            })
                        }
                    }
                }.toString())
            }) {
                incoming.collect { event ->
                    val data = event.data ?: return@collect
                    when (event.event) {
                        "content_block_delta" -> {
                            val text = runCatching {
                                val delta = Json.parseToJsonElement(data).jsonObject["delta"]?.jsonObject
                                if (delta?.get("type")?.jsonPrimitive?.content == "text_delta") {
                                    delta["text"]?.jsonPrimitive?.content
                                } else null
                            }.getOrNull()
                            if (!text.isNullOrEmpty()) emit(StreamEvent.TextDelta(text))
                        }
                        "message_stop" -> emit(StreamEvent.MessageCompleted)
                        "error" -> emit(StreamEvent.Error(parseError(data), retryable = true))
                    }
                }
            }
        } catch (t: CancellationException) {
            throw t
        } catch (e: ResponseException) {
            val status = e.response.status.value
            val body = runCatching { e.response.bodyAsText() }.getOrNull().orEmpty()
            emit(StreamEvent.Error(formatHttpError(status, body), retryable = status == 429 || status >= 500))
        } catch (t: Throwable) {
            emit(StreamEvent.Error(t.message ?: "Unable to connect to AgentRouter. Check your internet connection and endpoint.", retryable = true))
        }
    }

    private fun formatHttpError(status: Int, body: String): String {
        val providerMessage = parseError(body)
        return when (status) {
            400 -> "AgentRouter rejected the request (400): $providerMessage"
            401 -> "AgentRouter API key is invalid or expired (401). Create/rotate a valid key and enter it in Settings."
            403 -> "AgentRouter denied this request (403). Check the key permissions or account access."
            404 -> "AgentRouter endpoint/model was not found (404). Use https://co.agentrouter.org and a model available to your key."
            429 -> "AgentRouter rate limit or quota reached (429). Check your account quota and try again."
            in 500..599 -> "AgentRouter server error ($status). Try again shortly."
            else -> "AgentRouter request failed ($status): $providerMessage"
        }
    }

    private fun parseError(data: String): String = runCatching {
        Json.parseToJsonElement(data).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
            ?: Json.parseToJsonElement(data).jsonObject["message"]?.jsonPrimitive?.content
            ?: data.ifBlank { "No error details returned by the provider." }
    }.getOrDefault(data.ifBlank { "No error details returned by the provider." })

    fun close() = client.close()
}
