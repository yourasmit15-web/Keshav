# Keshav

> A native Android AI assistant built with Kotlin, Jetpack Compose, MVVM, Room, Coroutines, StateFlow, and streaming AI responses.

## Architecture

- **Presentation:** Jetpack Compose + Material 3 + ViewModel + StateFlow
- **Domain:** repository contracts and chat models
- **Data:** Room persistence + Ktor/OkHttp streaming client
- **Networking:** Anthropic Messages API with SSE streaming
- **Build:** Gradle Kotlin DSL, Android Gradle Plugin, Kotlin 2.3.x

## Current capabilities

- Native Compose chat UI
- User/assistant conversation state
- Streaming response handling
- Room-backed chat sessions and messages
- Navigation drawer foundation
- Stop/new-chat controls
- Release build configuration with R8

## Security note

API credentials must **never** be committed to Git. The current scaffold intentionally does not contain a real API key. Production hardening should add secure credential storage and a settings flow before release.

## Build

Open the project in Android Studio with a compatible JDK/Android SDK, allow Gradle to sync, then build the `app` module.

## Roadmap

1. Secure API-key settings and encrypted storage
2. Photo/document picker and attachment pipeline
3. Markdown rendering
4. Full chat history navigation and session management
5. Offline-first synchronization and robust retry/error states
6. Unit/UI tests and CI validation

## License

Project-specific licensing can be added before public distribution.
