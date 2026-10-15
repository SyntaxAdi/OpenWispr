<p align="center">
  <img src="./app/src/main/res/drawable-nodpi/ic_logo_full.png" width="22%" alt="OpenWispr Logo" />
</p>
<p align="center">
    <h1 align="center">OPENWISPR</h1>
</p>
<p align="center">
    <em>Zero-Latency Voice Dictation for Android — Powered by Whisper & Groq Cloud with Nothing OS Aesthetics</em>
</p>
<p align="center">
	<img src="https://img.shields.io/badge/license-MIT-009688?style=flat-square" alt="license">
	<img src="https://img.shields.io/github/last-commit/SyntaxAdi/OpenWispr?style=flat-square&logo=git&logoColor=white&color=009688" alt="last-commit">
	<img src="https://img.shields.io/github/languages/top/SyntaxAdi/OpenWispr?style=flat-square&color=009688" alt="repo-top-language">
	<img src="https://img.shields.io/badge/minSdk-26-009688?style=flat-square&logo=android&logoColor=white" alt="minSdk">
	<img src="https://img.shields.io/badge/targetSdk-34-009688?style=flat-square&logo=android&logoColor=white" alt="targetSdk">
</p>
<p align="center">
	<em>Built with:</em>
</p>
<p align="center">
	<img src="https://img.shields.io/badge/Kotlin-7F52FF.svg?style=flat-square&logo=Kotlin&logoColor=white" alt="Kotlin">
	<img src="https://img.shields.io/badge/Jetpack_Compose-4285F4.svg?style=flat-square&logo=Android&logoColor=white" alt="Jetpack Compose">
	<img src="https://img.shields.io/badge/Android_SDK-3DDC84.svg?style=flat-square&logo=Android&logoColor=white" alt="Android SDK">
	<img src="https://img.shields.io/badge/Groq_Cloud-F55036.svg?style=flat-square&logo=Groq&logoColor=white" alt="Groq Cloud">
	<img src="https://img.shields.io/badge/Whisper_v3-412991.svg?style=flat-square&logo=OpenAI&logoColor=white" alt="Whisper">
	<img src="https://img.shields.io/badge/Gradle-02303A.svg?style=flat-square&logo=Gradle&logoColor=white" alt="Gradle">
</p>
<br>

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Project Structure](#project-structure)
   3.1. [Project Index](#project-index)
4. [Getting Started](#getting-started)
   4.1. [Prerequisites](#prerequisites)
   4.2. [Installation & Build](#installation--build)
   4.3. [Configuration & Permissions](#configuration--permissions)
   4.4. [Usage](#usage)
   4.5. [Tests](#tests)
5. [Architecture](#architecture)
6. [Privacy & Security](#privacy--security)
7. [Roadmap](#roadmap)
8. [Contributing](#contributing)
9. [License](#license)
10. [Acknowledgments](#acknowledgments)

---

## Overview

**OpenWispr** is an ultra-lean, context-aware Android voice dictation utility engineered to mirror the fluid desktop typing experience of Wispr Flow. When an editable text input gains focus anywhere in the operating system (WhatsApp, Chrome, Gmail, Notes), OpenWispr surfaces an unobtrusive floating bubble. Tapping expands a tactile, Nothing OS-inspired capsule featuring real-time acoustic waveforms. 

Spoken speech is captured into local temporary audio and dispatched to Groq Cloud's ultra-low latency infrastructure:
1. **Transcription**: `whisper-large-v3-turbo` converts speech to raw text in sub-second time.
2. **Refinement**: `llama-3.1-8b-instant` systematically removes filler words, stuttering, false starts, and rambling while preserving original meaning.
3. **Direct Injection**: The refined text is injected directly into the active field via Android Accessibility APIs without touching the system clipboard.

Built with pure idiomatic Kotlin, Jetpack Compose, and native Android `HttpURLConnection`, OpenWispr operates without heavy external SDKs (no Retrofit, OkHttp, or Hilt), delivering an instant, privacy-respecting dictation tool with an APK footprint under 4MB.

---

## Features

|     | Feature | Summary |
| :-- | :---: | :------ |
| ⚙️ | **Architecture** | Single-Activity Jetpack Compose shell coupled with an independent `AccessibilityService` and window overlay manager. |
| 🔩 | **Code Quality** | Zero external networking bloatware; ~1,100 LOC Kotlin core; R8 resource and code shrinking. |
| 🎙️ | **Voice Engine** | Dual-stage Groq Cloud AI pipeline: Whisper Large v3 Turbo transcription + LLaMA 3.1 8B instant gibberish filtering. |
| 🌊 | **Waveform UI** | Dual-layer 28-bar audio visualizer (white voice overlay over grey baseline) with Nothing Red recording beacon. |
| 🫧 | **Dynamic Bubble** | System overlay floating bubble that automatically scales its expanded capsule and controls to user-configured DP. |
| 🔤 | **Direct Injection** | Direct `AccessibilityNodeInfo` `ACTION_SET_TEXT` injection with graceful fallback to clipboard paste and notifications. |
| 📊 | **On-Device Telemetry** | Local SQLite database tracking total words spoken, typing speed (WPM), usage streaks, and per-app distribution. |
| 🛡️ | **Privacy First** | Audio recorded strictly to local cache, uploaded directly over TLS to Groq, and purged immediately after dispatch. |

---

## Project Structure

```sh
└── OpenWispr/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle.properties
    ├── gradlew
    ├── app/
    │   ├── build.gradle.kts
    │   ├── proguard-rules.pro
    │   └── src/
    │       ├── main/
    │       │   ├── AndroidManifest.xml
    │       │   ├── java/com/openwispr/
    │       │   │   ├── MainActivity.kt
    │       │   │   ├── audio/
    │       │   │   │   └── AudioRecorder.kt
    │       │   │   ├── data/
    │       │   │   │   ├── GroqRepository.kt
    │       │   │   │   ├── HistoryDatabaseHelper.kt
    │       │   │   │   ├── HistoryRepository.kt
    │       │   │   │   └── SettingsRepository.kt
    │       │   │   ├── service/
    │       │   │   │   ├── BubbleManager.kt
    │       │   │   │   └── WisprAccessibilityService.kt
    │       │   │   ├── ui/
    │       │   │   │   ├── DotMatrixCanvas.kt
    │       │   │   │   ├── HistoryScreen.kt
    │       │   │   │   ├── HomeScreen.kt
    │       │   │   │   ├── SettingsScreen.kt
    │       │   │   │   ├── SideNavDrawer.kt
    │       │   │   │   └── WaveformView.kt
    │       │   │   └── util/
    │       │   │       ├── Constants.kt
    │       │   │       └── TextInjector.kt
    │       │   └── res/
    │       │       ├── drawable/
    │       │       ├── mipmap-anydpi-v26/
    │       │       └── values/
    │       └── test/java/com/openwispr/
    │           ├── HistoryLogicTest.kt
    │           └── SettingsAndSecurityTest.kt
```

### Project Index

<details open>
	<summary><b><code>OpenWispr Submodule & Component Index</code></b></summary>
	<details>
		<summary><b>Root & Build Configuration</b></summary>
		<blockquote>
			<table>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/build.gradle.kts'>build.gradle.kts</a></b></td>
				<td>Top-level Gradle build script configuring Android Gradle Plugin and Kotlin toolchains.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/build.gradle.kts'>app/build.gradle.kts</a></b></td>
				<td>App-level build definition configuring SDK 34, minSdk 26, Jetpack Compose BOM, Java 17 toolchain, and R8 shrinking rules.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/gradle.properties'>gradle.properties</a></b></td>
				<td>JVM configuration, AndroidX enablement, and Gradle build optimization properties.</td>
			</tr>
			</table>
		</blockquote>
	</details>
	<details>
		<summary><b>Core & Entrypoint</b></summary>
		<blockquote>
			<table>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/MainActivity.kt'>MainActivity.kt</a></b></td>
				<td>Root single-activity entry point orchestrating Compose theme, navigation drawer, and initial permissions verification.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/AndroidManifest.xml'>AndroidManifest.xml</a></b></td>
				<td>Declares application metadata, adaptive mipmap icons, audio/overlay permissions, and Accessibility Service bindings.</td>
			</tr>
			</table>
		</blockquote>
	</details>
	<details>
		<summary><b>Service & Overlay Management (<code>com.openwispr.service</code>)</b></summary>
		<blockquote>
			<table>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/service/WisprAccessibilityService.kt'>WisprAccessibilityService.kt</a></b></td>
				<td>Accessibility service detecting focused editable text inputs, managing recording locks, and binding WindowManager overlays.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/service/BubbleManager.kt'>BubbleManager.kt</a></b></td>
				<td>Controls floating overlay lifecycle: idle bubble state, drag gestures, expanded recording pill, waveform binding, and cancel/accept dispatch.</td>
			</tr>
			</table>
		</blockquote>
	</details>
	<details>
		<summary><b>Data Layer & AI Pipeline (<code>com.openwispr.data</code>)</b></summary>
		<blockquote>
			<table>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/data/GroqRepository.kt'>GroqRepository.kt</a></b></td>
				<td>Direct `HttpURLConnection` client for Groq Cloud: multipart audio upload to Whisper and streaming/JSON completion requests to LLaMA.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/data/SettingsRepository.kt'>SettingsRepository.kt</a></b></td>
				<td>Type-safe configuration storage backed by Android DataStore for API keys, model selections, bubble size, and timeout thresholds.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/data/HistoryRepository.kt'>HistoryRepository.kt</a></b></td>
				<td>Domain repository aggregating dictation telemetry, computing weekly streaks, words-per-minute (WPM), and application usage distributions.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/data/HistoryDatabaseHelper.kt'>HistoryDatabaseHelper.kt</a></b></td>
				<td>SQLite OpenHelper defining schemas and migrations for persistent transcription history and telemetry logs.</td>
			</tr>
			</table>
		</blockquote>
	</details>
	<details>
		<summary><b>Audio Engine (<code>com.openwispr.audio</code>)</b></summary>
		<blockquote>
			<table>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/audio/AudioRecorder.kt'>AudioRecorder.kt</a></b></td>
				<td>Wraps Android `MediaRecorder` targeting AAC-ELD/M4A; samples peak amplitude levels every 70ms for reactive visualizer feeds.</td>
			</tr>
			</table>
		</blockquote>
	</details>
	<details>
		<summary><b>User Interface & Graphics (<code>com.openwispr.ui</code>)</b></summary>
		<blockquote>
			<table>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/ui/HomeScreen.kt'>HomeScreen.kt</a></b></td>
				<td>Main dashboard displaying hardware permission status, real-time telemetry cards, streak trackers, and most-used app statistics.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/ui/SettingsScreen.kt'>SettingsScreen.kt</a></b></td>
				<td>Preferences interface allowing Groq key validation, model configuration, bubble size preview slider, and custom endpoint overrides.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/ui/HistoryScreen.kt'>HistoryScreen.kt</a></b></td>
				<td>Searchable transcription archive with timestamp groupings, word counts, package labels, and one-tap clipboard copy.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/ui/SideNavDrawer.kt'>SideNavDrawer.kt</a></b></td>
				<td>Nothing OS dot-matrix navigation drawer featuring live hardware telemetry, tab switching, and tactile close button.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/ui/WaveformView.kt'>WaveformView.kt</a></b></td>
				<td>Dual-layer custom canvas view rendering a 28-bar acoustic waveform with Nothing Red indicator dot and voice-reactive height scaling.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/ui/DotMatrixCanvas.kt'>DotMatrixCanvas.kt</a></b></td>
				<td>Custom 5x7 bitmap typography canvas rendering hardware-accelerated Nothing OS LED glyphs.</td>
			</tr>
			</table>
		</blockquote>
	</details>
	<details>
		<summary><b>Utilities (<code>com.openwispr.util</code>)</b></summary>
		<blockquote>
			<table>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/util/TextInjector.kt'>TextInjector.kt</a></b></td>
				<td>Executes direct `ACTION_SET_TEXT` injections into accessibility target nodes; provides fallback to system clipboard paste.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/main/java/com/openwispr/util/Constants.kt'>Constants.kt</a></b></td>
				<td>Global constants, default Groq endpoints, precompiled regexes, and system preference keys.</td>
			</tr>
			</table>
		</blockquote>
	</details>
	<details>
		<summary><b>Unit & Security Tests (<code>src/test</code>)</b></summary>
		<blockquote>
			<table>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/test/java/com/openwispr/SettingsAndSecurityTest.kt'>SettingsAndSecurityTest.kt</a></b></td>
				<td>Unit tests verifying boundary invariants, URL sanitization, API key validation patterns, and setting limits.</td>
			</tr>
			<tr>
				<td><b><a href='https://github.com/SyntaxAdi/OpenWispr/blob/main/app/src/test/java/com/openwispr/HistoryLogicTest.kt'>HistoryLogicTest.kt</a></b></td>
				<td>Unit tests covering streak calculation algorithms, word counting, formatting rules, and telemetry aggregations.</td>
			</tr>
			</table>
		</blockquote>
	</details>
</details>

---

## Getting Started

### Prerequisites

Ensure your development environment meets the following specifications:
- **Operating System:** Linux, macOS, or Windows
- **Java Development Kit (JDK):** JDK 17 (OpenJDK or Oracle)
- **Android SDK:** Platform 34 (Android 14) with Build-Tools 34.0.0
- **Build Tool:** Gradle 8.2+ (handled via included `./gradlew` wrapper)
- **Groq API Key:** Active account and API token from [console.groq.com](https://console.groq.com/)

---

### Installation & Build

Build the debug or production APK directly from source:

1. Clone the repository:
```sh
git clone https://github.com/SyntaxAdi/OpenWispr.git
cd OpenWispr
```

2. Configure local Android SDK path in `local.properties`:
```properties
sdk.dir=/path/to/your/android/sdk
```

3. Assemble the debug APK:
```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew assembleDebug
```

4. Install directly onto a connected device or emulator via ADB:
```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

### Configuration & Permissions

Upon launching OpenWispr for the first time:
1. **Grant Microphone Permission**: Required for capturing audio input.
2. **Enable Accessibility Service**: Navigate to `Settings > Accessibility > Installed Apps > OpenWispr` and enable the service. This allows OpenWispr to detect when an editable input field is active and directly inject transcribed text.
3. **Grant Display Over Other Apps**: (If prompted on Android 10+) Required for overlaying the floating action bubble.
4. **Enter Groq API Key**: Open the Settings tab in the app, paste your `gsk_...` key, tap **Validate** to confirm connectivity, and tap **Save**.

---

### Usage

1. **Activate an Input**: Tap on any text input box (e.g. WhatsApp chat, Chrome address bar, Google Keep note).
2. **Surface Bubble**: The OpenWispr floating circular bubble will appear on the bottom-right corner. It is freely draggable to any screen edge.
3. **Start Recording**: Tap the bubble. The bubble expands into a tactile pill capsule with a voice-reactive waveform.
4. **Speak Freely**: Dictate naturally. The dual-layer visualizer will register speech volume in real time.
5. **Finish & Clean**:
   - Tap **✓ (Checkmark)**: Captures audio, invokes Whisper + LLaMA, cleans up pauses and filler words, and pastes text directly into the focused field.
   - Tap **✕ (Cancel)**: Immediately aborts recording and purges the cached audio file without making network requests.

---

### Tests

Execute the automated test suite with JUnit:

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew testDebugUnitTest
```

---

## Architecture

```
                                  [ User Input Field Focused ]
                                                │
                                                ▼
                                    WisprAccessibilityService
                                                │
                                    ┌───────────┴───────────┐
                                    ▼                       ▼
                            TextInjector             BubbleManager
                                (Direct Injection)          │ (WindowManager Overlay)
                                                            ▼
                                                       AudioRecorder
                                                   (70ms Amplitude Polling)
                                                            │
                                                            ▼
                                                       WaveformView
                                                 (Dual-Layer UI Animation)
                                                            │
                                                [ User Taps Finish (✓) ]
                                                            │
                                                            ▼
                                                      GroqRepository
                                            (Direct TLS HttpURLConnection)
                                            ┌───────────────┴───────────────┐
                                            ▼                               ▼
                                  Whisper Large v3 Turbo           LLaMA 3.1 8B Instant
                                    (Speech-to-Text)               (Gibberish Refinement)
                                            │                               │
                                            └───────────────┬───────────────┘
                                                            ▼
                                                   TextInjector.injectText()
```

---

## Privacy & Security

- **Ephemeral Audio Storage:** Audio is stored strictly in `context.cacheDir/wispr_<timestamp>.m4a` during the active recording session.
- **Immediate Cache Purge:** Audio files are systematically deleted from local storage immediately following network dispatch or upon pressing Cancel.
- **Zero Third-Party Trackers:** No analytics SDKs, advertising frameworks, crash trackers, or telemetry beacons are bundled.
- **Strict Network Routing:** Network transmissions occur exclusively over direct HTTPS/TLS connections to `api.groq.com` (or user-configured custom endpoints).

---

## Roadmap

- [x] Context-aware floating bubble overlay with accessibility focus locks.
- [x] Dual-layer Nothing OS waveform visualizer with red recording indicator.
- [x] Dynamic capsule and button scaling matching user bubble size settings.
- [x] Nothing OS dot-matrix LED typographic headers and custom canvas glyphs.
- [x] Complete brand identity integration with adaptive vector layers and monochrome Nothing icon pack support.
- [ ] Offline transcription fallback using on-device Whisper models via GGML / Whisper.cpp.
- [ ] Multilingual auto-detection and custom vocabulary prompt injection.

---

## Contributing

Contributions are welcome!

1. **Fork the Repository**: Fork the repository to your GitHub account.
2. **Create a Feature Branch**:
   ```sh
   git checkout -b feature/amazing-feature
   ```
3. **Commit Your Changes**:
   ```sh
   git commit -m 'feat: implement amazing feature'
   ```
4. **Push to GitHub**:
   ```sh
   git push origin feature/amazing-feature
   ```
5. **Open a Pull Request**: Submit your pull request against the `main` branch with a clear description of changes.

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for more information.

---

## Acknowledgments

- Inspired by the typing experience of **Wispr Flow**.
- Designed with **Nothing OS** minimalist dot-matrix aesthetics.
- Accelerated by **Groq Cloud** ultra-fast Whisper and LLaMA inference.
