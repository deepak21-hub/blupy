# Bluey Windows Desktop Companion ("The Brain & Hands")

This Python application acts as Bluey's brain and hands on Windows, while your Android phone sits beneath the monitor acting as Bluey's face and microphone.

## Features

1. **Spatial Eye-Tracking (Cycle 1)**:
   - Tracks the live mouse cursor `X, Y` at 30 fps.
   - Calculates relative percentages `x_pct, y_pct` and streams them via WebSocket to the Android app.
   - Bluey's eyes on the phone track your Windows cursor seamlessly in real time!

2. **Contextual Query & Vision (Cycle 2)**:
   - When you press and hold Bluey's face on the phone, audio/text is sent to Windows.
   - Captures screenshot context around the mouse cursor (`mss` / `Pillow`).
   - Responds with cartoon chirps, facial expressions, and desktop speech bubbles.

3. **OS Automation & Safety Guardrails (Cycle 3)**:
   - Automates mouse clicks and keystrokes via `PyAutoGUI`.
   - **Safety First**: Confirms destructive actions before executing.
   - **Privacy**: Avoids password and sensitive fields.
   - **Emergency Kill Switch**: Press `Ctrl+Shift+Esc` anytime to instantly freeze all automated actions.

4. **Meeting Notes & Transcription (Cycle 4)**:
   - Records session chunks and saves structured transcripts and Markdown notes to `~/Documents/Bluey Notes/`.

## Quick Start

1. Install requirements:
   ```bash
   pip install -r requirements.txt
   ```

2. Run the companion server:
   ```bash
   python server.py
   ```

3. Ensure your Android phone and Windows PC are on the same Wi-Fi. The Android app connects to `ws://<YOUR_PC_IP>:8765/` (or `10.0.2.2:8765` if testing in the Android emulator).
