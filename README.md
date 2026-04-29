# Smart Voice Assistant

A production-ready Android voice assistant that provides full phone control using voice commands and hand gestures. Supports **English**, **Amharic (አማርኛ)**, and **Afaan Oromo** with automatic language detection.

---

## Features

### Voice Control
- **Speech-to-Text** using Android's built-in `SpeechRecognizer`
- **Multilingual support**: English, Amharic, Afaan Oromo
- **Automatic language detection** based on character sets and keyword analysis
- **Natural Language Understanding (NLU)** — flexible command parsing, not just fixed phrases

### Command Execution
The assistant understands and executes commands like:

| Category | Example Commands (English) |
|----------|---------------------------|
| App Control | "Open WhatsApp", "Launch YouTube" |
| Calls | "Call John", "Dial 911" |
| Messaging | "Send message to Maria saying hello" |
| WiFi | "Turn on WiFi", "Disable WiFi" |
| Bluetooth | "Turn on Bluetooth" |
| Flashlight | "Flashlight on", "Turn off torch" |
| Media | "Play music", "Next track", "Pause" |
| Alarm | "Set alarm for 7:30 AM" |
| Timer | "Set timer for 5 minutes" |
| Volume | "Volume up", "Mute" |
| Camera | "Take photo", "Take selfie" |
| Navigation | "Navigate to Addis Ababa" |
| Search | "Search for weather today" |
| Battery | "Check battery level" |
| Settings | "Open settings" |

**Amharic examples**: "ዋትስአፕ ክፈት" (Open WhatsApp), "ወደ ማማ ደውል" (Call Mom), "ዋይፋይ አብራ" (Turn on WiFi)

**Afaan Oromo examples**: "WhatsApp bani" (Open WhatsApp), "gara haadha bilbili" (Call Mom), "waayifaayii banaa" (Turn on WiFi)

### Text-to-Speech
- Responds in the **same language** the user speaks
- Configurable speech rate and pitch
- Supports all three languages (with fallback to English if TTS data is unavailable)

### Hand Gesture Recognition
- Uses **MediaPipe GestureRecognizer** for on-device hand detection
- Supported gestures:
  - **Open palm** → Activate voice input
  - **Closed fist** → Stop / Cancel
  - **Thumbs up** → Confirm action
  - **Thumbs down** → Reject / Go back
  - **Point up** → Scroll up
  - **Victory/Peace** → Take screenshot

### Offline Support
- NLU engine (language detection + command parsing) works **fully offline**
- Android's on-device speech recognition used when available
- Room database for local command history storage

### Security & Permissions
- Granular runtime permission requests
- Accessibility Service for advanced phone control (user must enable manually)
- No data sent to external servers (except Google's speech API when online)

---

## Architecture

```
MVVM (Model-View-ViewModel)
├── data/                    # Data layer
│   ├── model/               # Data classes (Command, Language, etc.)
│   ├── local/               # Room database (CommandHistoryEntity, DAO)
│   └── repository/          # Repositories (CommandRepository, ContactRepository)
├── service/                 # Business logic layer
│   ├── speech/              # SpeechRecognitionService, TextToSpeechService
│   ├── nlu/                 # NLUEngine, CommandParser, LanguageDetector
│   ├── command/             # CommandExecutor, AppLauncher, PhoneController, SystemController
│   ├── gesture/             # GestureRecognitionService (MediaPipe)
│   └── accessibility/       # VoiceAccessibilityService
├── ui/                      # Presentation layer
│   ├── main/                # MainActivity, MainViewModel
│   ├── history/             # HistoryAdapter (RecyclerView)
│   ├── settings/            # SettingsFragment, SettingsViewModel
│   └── gesture/             # GestureCameraFragment
└── util/                    # Utilities
    ├── PermissionManager.kt
    └── Constants.kt
```

---

## Module Explanations

### 1. Data Layer (`data/`)

| File | Purpose |
|------|---------|
| `Language.kt` | Enum of supported languages with BCP-47 codes |
| `Command.kt` | Parsed command with type, parameters, language |
| `CommandResult.kt` | Execution result with success status and message |
| `GestureAction.kt` | Gesture-to-action mapping enum |
| `CommandHistoryEntity.kt` | Room entity for persistence |
| `CommandHistoryDao.kt` | Room DAO with Flow-based queries |
| `AppDatabase.kt` | Room database singleton |
| `CommandRepository.kt` | Repository pattern for command history |
| `ContactRepository.kt` | Content provider access for device contacts |

### 2. Speech Services (`service/speech/`)

| File | Purpose |
|------|---------|
| `SpeechRecognitionService.kt` | Wraps Android SpeechRecognizer with StateFlow-based state management. Supports per-language and auto-detect modes. |
| `TextToSpeechService.kt` | Wraps Android TTS with language-matched responses. Automatically selects the correct Locale for each language. |

### 3. NLU Engine (`service/nlu/`)

| File | Purpose |
|------|---------|
| `NLUEngine.kt` | Pipeline: raw text → detect language → parse command → Command object |
| `LanguageDetector.kt` | Offline language detection using Unicode ranges (Ethiopic) and keyword scoring |
| `CommandParser.kt` | Flexible keyword-based command parsing for all three languages. Uses pattern matching with multiple synonyms per command. |

### 4. Command Execution (`service/command/`)

| File | Purpose |
|------|---------|
| `CommandExecutor.kt` | Central dispatcher — routes parsed commands to the appropriate controller |
| `AppLauncher.kt` | Resolves app names to package names and launches them via Intent |
| `PhoneController.kt` | Handles calls (ACTION_CALL) and SMS (SmsManager) |
| `SystemController.kt` | WiFi, Bluetooth, flashlight, volume, alarms, media control, navigation, search |

### 5. Gesture Recognition (`service/gesture/`)

| File | Purpose |
|------|---------|
| `GestureRecognitionService.kt` | MediaPipe GestureRecognizer integration for live hand gesture detection |

### 6. Accessibility (`service/accessibility/`)

| File | Purpose |
|------|---------|
| `VoiceAccessibilityService.kt` | Enables global actions (back, home, screenshot, notifications) and reading on-screen content |

### 7. UI Layer (`ui/`)

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Main screen with mic button, language selector, result card, command history |
| `MainViewModel.kt` | Orchestrates the full pipeline: STT → NLU → Execute → TTS |
| `HistoryAdapter.kt` | RecyclerView adapter with DiffUtil for command history |
| `SettingsFragment.kt` | Settings UI for language, speech rate, gesture toggle, offline mode |
| `GestureCameraFragment.kt` | CameraX preview with MediaPipe gesture processing |

---

## Setup Instructions

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17**
- **Android SDK 34** (compileSdk)
- **Min SDK 26** (Android 8.0 Oreo)

### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd SmartVoiceAssistant
```

### Step 2: Open in Android Studio
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `SmartVoiceAssistant` folder
4. Wait for Gradle sync to complete

### Step 3: Download MediaPipe Model (for Gesture Recognition)
1. Download `gesture_recognizer.task` from [MediaPipe Models](https://developers.google.com/mediapipe/solutions/vision/gesture_recognizer#models)
2. Place it in `app/src/main/assets/gesture_recognizer.task`
3. If you skip this step, the app still works — gesture recognition will be disabled

### Step 4: Build and Run
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### Step 5: Grant Permissions
On first launch, the app will request:
- **Microphone** — Required for voice recognition
- **Camera** — Optional, for gesture recognition
- **Phone** — For making calls
- **Contacts** — For looking up contact names
- **SMS** — For sending text messages

### Step 6: Enable Accessibility Service (Optional)
For advanced features (screenshot, go back, read notifications):
1. Go to **Settings → Accessibility**
2. Find **Smart Voice Assistant**
3. Enable the service

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| AndroidX Core KTX | 1.12.0 | Kotlin extensions |
| Material Components | 1.11.0 | UI components |
| Lifecycle ViewModel | 2.7.0 | MVVM architecture |
| Navigation | 2.7.7 | Fragment navigation |
| Room | 2.6.1 | Local database |
| CameraX | 1.3.1 | Camera for gestures |
| MediaPipe Tasks Vision | 0.10.9 | Hand gesture recognition |
| Coroutines | 1.7.3 | Async operations |
| Lottie | 6.3.0 | Animations |
| Preferences KTX | 1.2.1 | SharedPreferences |

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| Architecture | MVVM |
| Speech-to-Text | Android SpeechRecognizer (Google Speech API) |
| Text-to-Speech | Android TextToSpeech |
| Gesture Recognition | MediaPipe GestureRecognizer |
| Database | Room |
| Camera | CameraX |
| UI | Material Design 3, ViewBinding |
| Async | Kotlin Coroutines + Flow |

---

## Android Limitations & Workarounds

| Feature | Limitation | Workaround |
|---------|-----------|------------|
| WiFi Toggle | Android 10+ prevents direct toggle | Opens WiFi settings panel |
| Bluetooth Toggle | Direct toggle deprecated | Opens Bluetooth settings |
| Close Apps | Requires Accessibility Service | User must enable manually |
| Screenshots | Requires Accessibility Service | GLOBAL_ACTION_TAKE_SCREENSHOT |
| Read Notifications | Requires Accessibility Service | Notification listener |
| Brightness Control | Requires WRITE_SETTINGS permission | Opens display settings |
| Amharic/Oromo TTS | May not be installed on device | Falls back to English |

---

## Project Structure

```
SmartVoiceAssistant/
├── app/
│   ├── build.gradle.kts            # App-level build config
│   ├── proguard-rules.pro          # ProGuard rules for release
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml  # Permissions & components
│       │   ├── java/com/smartvoice/assistant/
│       │   │   ├── SmartVoiceApp.kt
│       │   │   ├── data/
│       │   │   │   ├── model/       # Command, Language, etc.
│       │   │   │   ├── local/       # Room DB
│       │   │   │   └── repository/  # Data repositories
│       │   │   ├── service/
│       │   │   │   ├── speech/      # STT & TTS
│       │   │   │   ├── nlu/         # NLU engine
│       │   │   │   ├── command/     # Command execution
│       │   │   │   ├── gesture/     # MediaPipe gestures
│       │   │   │   └── accessibility/
│       │   │   ├── ui/
│       │   │   │   ├── main/        # MainActivity + ViewModel
│       │   │   │   ├── history/     # History adapter
│       │   │   │   ├── settings/    # Settings UI
│       │   │   │   └── gesture/     # Camera gesture fragment
│       │   │   └── util/            # Constants, PermissionManager
│       │   └── res/
│       │       ├── layout/          # XML layouts
│       │       ├── values/          # Colors, strings, themes
│       │       ├── drawable/        # Icons, backgrounds
│       │       ├── menu/            # Toolbar menu
│       │       └── xml/             # Accessibility config
│       ├── test/                    # Unit tests (NLU, parsing)
│       └── androidTest/             # Instrumented tests
├── build.gradle.kts                 # Root build config
├── settings.gradle.kts              # Module settings
├── gradle.properties                # Gradle properties
└── README.md                        # This file
```

---

## Testing

Run unit tests:
```bash
./gradlew test
```

The test suite covers:
- Language detection for all three languages
- Command parsing for English, Amharic, and Afaan Oromo
- Full NLU pipeline integration tests
- Edge cases and unknown commands

---

## Play Store Readiness

This MVP is designed for Play Store publication:
- [x] Proper permission handling with runtime requests
- [x] ProGuard/R8 configuration for release builds
- [x] Adaptive icons (mipmap)
- [x] Material Design UI
- [x] Privacy-conscious (on-device processing where possible)
- [x] Proper error handling and user feedback
- [x] minSdk 26 (covers 95%+ of active devices)

Before publishing:
1. Add a privacy policy URL
2. Create a signing keystore
3. Test on multiple device sizes
4. Add Crashlytics or similar crash reporting
5. Download Amharic & Oromo TTS language packs on target devices

---

## License

MIT License — see LICENSE file for details.
