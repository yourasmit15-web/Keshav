# keshav

Keshav is a native Android AI assistant built with Kotlin, Jetpack Compose, Room, DataStore and Ktor SSE.

## Working features
- Claude streaming chat through the Anthropic Messages API
- Persistent local chat history and session selection
- Retry and Stop with partial-response persistence
- Image attachment and multimodal image requests
- Markdown-style headings, bullets, bold and inline code rendering
- Android Keystore-backed AES-GCM API-key encryption
- Configurable API endpoint and model
- Light/dark theme
- Agent Mode with coding-focused system behavior
- Sandboxed local agent workspace abstraction
- GitHub Actions build/test workflow with debug APK artifact

## Setup
1. Open the project in Android Studio with JDK 17.
2. Build and install the debug APK.
3. Open **Settings** and enter your Anthropic API key, endpoint and model.
4. Save and start chatting.

## Security
The API key is encrypted at rest with Android Keystore, but any client-side API credential can potentially be extracted from a compromised/rooted device. For a public production release, use a backend/token broker so the provider credential is not shipped to clients.

## Agent Mode
Keshav can provide complete code, patches, plans and verification steps. A full Claude Code-equivalent agent also needs an execution backend or terminal environment with compilers, package managers, git and long-running processes. The Android client deliberately does not execute arbitrary remote commands.
