# Gemini API Compose Starter

An Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3** that demonstrates how to build a modern, secure AI chat experience powered by the **Google Generative AI SDK** (`gemini-3.6-flash`).

---

## Features

- **Hardware-Backed API Key Encryption**: The Gemini API key is encrypted at rest using AES-256-GCM via the Android Keystore and persisted securely in Preferences DataStore.
- **Material 3 Chat UI**: Rendered as a `LazyColumn` of styled, rounded chat bubbles (user messages aligned right in `primaryContainer`, assistant replies aligned left in `secondaryContainer`).
- **Unidirectional Data Flow (UDF)**: `ChatViewModel` hoists immutable `ChatUiState` as a `StateFlow`, keeping UI composables stateless.
- **Responsive Layout**: Adapts layout across phones, foldables, and tablets using Compose `WindowSizeClass`.
- **Loading & Error Handling**: Displays an inline assistant thinking indicator while awaiting responses, and surfaces network errors via a `SnackbarHost` with a **Retry** action.
- **Voice Input**: Integrates Android Speech-to-Text (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) next to the prompt input field.
- **User Preferences**: Persists theme mode choices (System Default / Light / Dark) and dynamic color toggles using DataStore Preferences (`user_preferences`).
- **Offline Room Chat History**: Local message persistence backed by Room Database (`chat_history.db`) with a clear chat history option.
- **Comprehensive Testing**: Includes unit tests for `ChatViewModel` using fakes, and instrumented Compose UI tests for `ChatScreen`.

---

## Setup & Configuration

### 1. Obtain a Gemini API Key
Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/).

### 2. Configure Local Properties
1. Copy `local.properties.example` in the project root to `local.properties` (if not already present).
2. Add your Gemini API key:
   ```properties
   GEMINI_API_KEY=your_actual_api_key_here
   ```
> **Note**: `local.properties` is git-ignored and will never be committed to version control.

### 3. Continuous Integration (CI) Fallback
For build pipelines or CI runners, pass the API key as an environment variable:
```bash
export GEMINI_API_KEY="your_actual_api_key_here"
```
The Gradle build script checks `local.properties` first, falling back to `System.getenv("GEMINI_API_KEY")`.

---

## Hardware-Backed Encryption Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Build Time: local.properties / System.getenv             │
│    --> Embedded into generated BuildConfig.GEMINI_API_KEY    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. First Run (SecureKeyStore):                              │
│    --> Generates AES-256-GCM Key in Android Keystore        │
│        (alias: "gemini_key_alias")                          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Encryption at Rest:                                      │
│    --> Encrypts BuildConfig key using AES-256-GCM           │
│    --> Stores Base64 ciphertext & IV in Preferences         │
│        DataStore ("secure_key_store")                       │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Runtime On-Demand Decryption:                            │
│    --> SecureKeyStore.getApiKey() decrypts key in memory    │
│        only when GenerativeModel is instantiated            │
│    --> Key is never logged, cached, or printed to disk      │
└─────────────────────────────────────────────────────────────┘
```

---

## Security Limits & Production Best Practices

- **Client-Side Limits**: Obfuscating and encrypting keys in the Android Keystore raises the bar against reverse engineering, but no client-side secret is 100% immune on a rooted or compromised device.
- **Production Architecture**: For production apps, route Gemini requests through a secure Backend-for-Frontend (BFF) proxy or enforce [Firebase App Check](https://firebase.google.com/docs/app-check) with API key restrictions in Google Cloud Console.
- **R8 Minification**: Release builds have code minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) enabled, with ProGuard rules protecting Generative AI, DataStore, and Room.

---

## Running Tests

### Unit Tests (`app/src/test`)
Unit tests verify `ChatViewModel` state transitions using coroutines test dispatchers and in-memory fakes.

- **Command Line**:
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```
- **Android Studio**: Right-click `app/src/test/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatViewModelTest.kt` and select **Run 'ChatViewModelTest'**.

### Compose UI Tests (`app/src/androidTest`)
Instrumented tests verify UI composables, loading indicators, send button states, and click callbacks using `createComposeRule()`.

- **Command Line** (requires connected device/emulator):
  ```bash
  ./gradlew :app:connectedDebugAndroidTest
  ```
- **Android Studio**: Right-click `app/src/androidTest/java/com/fahim/geminiApiComposeStarter/ui/chat/ChatScreenTest.kt` and select **Run 'ChatScreenTest'**.

---

## Architecture & Project Structure

The project follows **MVVM** architecture with **Unidirectional Data Flow (UDF)**:

```
com.fahim.geminiApiComposeStarter
 ├── data
 │    ├── history           # ChatHistoryRepository interface & Room implementation
 │    ├── local             # Room Entity (ChatMessageEntity), DAO, and ChatDatabase
 │    ├── preferences       # UserPreferences DataStore repository
 │    ├── GeminiRepository.kt
 │    └── GeminiRepositoryImpl.kt
 ├── security               # SecureKeyStore (Android Keystore AES-256-GCM)
 ├── ui
 │    ├── chat              # ChatScreen, ChatViewModel, ChatUiState, ChatMessage
 │    ├── text              # Markdown bold formatting utilities
 │    └── theme             # Material 3 Color, Theme, and Typography
 └── MainActivity.kt        # App entry point & dependency injection
```
