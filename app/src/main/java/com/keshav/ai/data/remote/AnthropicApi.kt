package com.keshav.ai.data.remote

import com.keshav.ai.domain.model.PromptMessage
import com.keshav.ai.domain.model.StreamEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class AnthropicRemote(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String = "claude-sonnet-4-5"
) {
    private val client = HttpClient(OkHttp) { install(ContentNegotiation) }

    fun stream(messages: List<PromptMessage>): Flow<StreamEvent> = flow {
        try {
            client.sse(
                urlString = "$baseUrl/v1/messages",
                request = {
                    method = io.ktor.http.HttpMethod.Post
                    contentType(ContentType.Application.Json)
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                    header("accept", "text/event-stream")
                    setBody(buildJsonObject {
                        put("model", model)
                        put("max_tokens", 2048)
                        put("stream", true)
                        putJsonArray("messages") {
                            messages.filter { it.role.name == "USER" || it.role.name == "ASSISTANT" }.forEach { message ->
                                add(buildJsonObject {
                                    put("role", if (message.role.name == "USER") "user" else "assistant")
                                    put("content", message.content)
                                })
                            }
                        }
                    }.toString())
                }
            ) {
                incoming.collect { event ->
                    if (event.event == "content_block_delta") {
                        val text = event.data?.let { data ->
                            runCatching { Json.parseToJsonElement(data).jsonObject["delta"]?.jsonObject?.get("text")?.toString()?.trim('"') }.getOrNull()
                        }
                        if (!text.isNullOrEmpty()) emit(StreamEvent.TextDelta(text))
                    } else if (event.event == "message_stop") {
                        emit(StreamEvent.MessageCompleted)
                    }
                }
            }
        } catch (t: Throwable) {
            emit(StreamEvent.Error(t.message ?: "Network request failed"))
        }
    }

    fun close() = client.close()
}
