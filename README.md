# Open Wispr — Minimal Wispr Flow Clone (Android / Kotlin)

**Speech-to-text with Groq Cloud** — Tap bubble → speak → auto-paste cleaned text.

## Features
- Floating bubble visible **only when text field is focused**; stays locked while recording (won't disappear if you tap outside).
- **Tap-to-record**: collapsed mic circle → expands to pill `[ X | ~waveform~ | ✓ ]`. Waveform grows/shrinks with voice volume (70ms polling of MediaRecorder amplitude).
- **Voice-reactive** waveform, cancel (X), finish (✓), auto-timeout (default 60s, adjustable 15–120s in Settings).
- **Groq Cloud**: `whisper-large-v3-turbo` → `llama-3.1-8b-instant` to remove stutter/rambling. Both models selectable.
- **Direct paste** via Accessibility `ACTION_SET_TEXT`; fallback to clipboard + `ACTION_PASTE` + notification if focus lost.
- **Settings**: API key + Validate (GET /models), Whisper/LLaMA dropdowns, bubble size 40–100dp, max duration slider, custom Base URL.
- **Tiny**: ~1100 LOC Kotlin, no Retrofit/OkHttp/Hilt, HttpURLConnection + DataStore only, R8 minify, target APK <4MB.
- **Principles**: SRP, DRY, KISS, OCP, DIP, fail-fast.

## Build
```bash
# Install Android SDK + JDK 17, then:
cd OpenWispr
./gradlew assembleDebug   # or assembleRelease
adb install app/build/outputs/apk/debug/app-debug.apk
```
Set `sdk.dir` in `local.properties` to your SDK path.

## Setup
1. Install APK, open **Open Wispr**.
2. Grant **Microphone**, enable **Accessibility Service** (required), optionally overlay.
3. Enter **Groq API key** (`gsk_...` from console.groq.com) → **Validate** → Save.
4. Tap any EditText (WhatsApp, Chrome, Keep) → bubble appears (bottom-right, draggable).
5. Tap mic → pill expands → speak → tap ✓ → cleaned text pastes. Or tap X to cancel. Bubble hides on back / unfocus (unless recording).

## Logo
Vector `ic_logo.xml` / `ic_launcher.xml` — gradient purple/blue mic with sound waves, Wispr Flow style, no bitmap.

## Min SDK
26 (Android 8.0) — covers 95% devices, enables TYPE_ACCESSIBILITY_OVERLAY, best size/perf tradeoff. Lower to 24 by changing `minSdk` in `app/build.gradle.kts`.

## Architecture
```
SettingsRepository (DataStore) → SettingsScreen (Compose)
WisprAccessibilityService (focus detection, locks during recording)
  └─ BubbleManager (WindowManager, State: Idle/Recording/Processing, drag, animate, timeout)
       ├─ AudioRecorder (MediaRecorder → cache .m4a, getMaxAmplitude)
       ├─ WaveformView (28 bars, voice-reactive)
       ├─ GroqRepository (transcribe + refine via HttpURLConnection)
       └─ TextInjector (ACTION_SET_TEXT → clipboard fallback)
```

## Permissions
- `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, `BIND_ACCESSIBILITY_SERVICE`

## Privacy
Audio is stored only in `cacheDir/wispr_*.m4a`, sent to Groq, then deleted. No analytics.
