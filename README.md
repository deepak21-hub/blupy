# Bluey: Multi-Device AI Companion (Android + Windows)

Bluey is an expressive AI companion who lives on an Android phone physically resting beneath your Windows monitor. The phone acts as Bluey's face and microphone, while your Windows PC acts as his brain and hands.

## System Architecture

### 1. Android Client (`app/`)
* **Tech Stack:** Kotlin, Jetpack Compose, Material Design 3, OkHttp WebSockets.
* **UI/UX:** Fullscreen animated organic blueberry character with realistic googly eyes, spring-damped physics, autonomous saccades, natural blinks, expressiveness (7 moods: listening, resting, thinking, talking, pointing, happy, sleepy), mouth-nose audio reaction, and pop-up speech bubbles.
* **Audio & Haptics:** AudioTrack cartoon chirp synthesizer with frequency sweep, touch and double-tap gesture interactions.
* **Networking & Synchronization:** Low-latency local WebSocket client connecting to the Windows desktop server (`ws://<IP>:8765`), calibration for physical phone placement (Left, Center, Right), plus a standalone smart fallback mode.
* **Dashboard & Meeting Notes:** Full transcript viewer with speaker diarization tags (`Speaker A`, `Speaker B`...), questions asked, replies, collapsible research report cards, and "Copy prompt for agent" button.

### 2. Windows Companion (`windows/`)
* **Tech Stack:** Python, FastAPI, WebSockets, PyAutoGUI, mDNS Zeroconf, mss / Pillow.
* **Spatial Eye-Tracking (Cycle 1):** Samples mouse cursor `X, Y` and streams percentage coordinates at 30 fps over WebSocket.
* **Vision & Context (Cycle 2):** Captures bounding box around the cursor and pairs with speech queries for multimodal answers.
* **OS Automation (Cycle 3):** Executes clicks and keypresses with built-in safety guardrails and an emergency kill-switch (`Ctrl+Shift+Esc`).
* **Meeting Notes (Cycle 4):** Records and organizes meetings into Markdown and JSON files.

## Project Structure

```
├── app/                      # Android application (Kotlin + Jetpack Compose)
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt   # App entry point with edge-to-edge
│   │   ├── anim/             # Damped spring physics and face animator
│   │   ├── audio/            # Cartoon chirp audio synthesizer
│   │   ├── model/            # Palette, FaceState, Session, and Protocol models
│   │   ├── network/          # OkHttp WebSocket client & calibration
│   │   ├── ui/               # Compose screens, FaceCanvas, and custom BlobShape
│   │   └── viewmodel/        # BlueyViewModel state management
│   └── src/main/res/         # Authentic Fredoka, IBM Plex fonts, drawables & icons
├── windows/                  # Windows desktop companion ("Brain & Hands")
│   ├── server.py             # FastAPI + WebSockets server, PyAutoGUI, mDNS
│   ├── requirements.txt      # Python dependencies
│   └── README.md             # Windows companion setup guide
├── metadata.json             # AI Studio applet metadata
└── build.gradle.kts          # Root build configuration
```

## Running the Apps

* **Android App:** Built and previewable in the AI Studio Streaming Emulator or deployed as an APK.
* **Windows App:** Run `pip install -r windows/requirements.txt` and `python windows/server.py`.
