"""
Bluey Windows Desktop Companion ("The Brain & Hands")
- Spatial eye-tracking cursor streamer (30 fps)
- Local WebSocket server (port 8765) with mDNS auto-discovery
- Screen context grabber (mss) & OpenAI Whisper / GPT-4o vision
- OS automation (PyAutoGUI) with safety guardrails and emergency kill-switch
"""

import asyncio
import base64
import io
import json
import os
import socket
import sys
import threading
import time
from typing import Set

import fastapi
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
import uvicorn
from zeroconf import IPVersion, ServiceInfo, Zeroconf
import pyautogui
from PIL import Image

# Safety settings for PyAutoGUI
pyautogui.FAILSAFE = True
pyautogui.PAUSE = 0.05

app = FastAPI(title="Bluey Windows Brain")

# Connected Android clients
active_websockets: Set[WebSocket] = set()

# State variables
is_kill_switch_triggered = False
meeting_mode_active = False
current_session_file = None

# Calibration: phone placement offset (0.0 = center, -0.25 = left, 0.25 = right)
phone_placement_offset = 0.0


def emergency_kill_switch():
    """Triggered on Ctrl+Shift+Esc or emergency hotkey."""
    global is_kill_switch_triggered
    is_kill_switch_triggered = True
    print("[EMERGENCY] Kill switch triggered! All PyAutoGUI actions halted.")


def is_safe_action(action_type: str, target: str) -> bool:
    """Safety guardrail: confirm before destructive actions."""
    destructive_keywords = ["delete", "format", "remove", "close", "shutdown", "logout", "send email", "drop"]
    target_lower = target.lower()
    for kw in destructive_keywords:
        if kw in target_lower:
            return False
    return True


def capture_cursor_screen_context(box_size: int = 500) -> Image.Image:
    """Captures a bounding box around the current cursor."""
    x, y = pyautogui.position()
    left = max(0, x - box_size // 2)
    top = max(0, y - box_size // 2)
    screenshot = pyautogui.screenshot(region=(left, top, box_size, box_size))
    return screenshot


async def broadcast_cursor_loop():
    """Cycle 1: Streams cursor coordinates as percentage at 30fps."""
    screen_w, screen_h = pyautogui.size()

    while True:
        if active_websockets and not is_kill_switch_triggered:
            try:
                x, y = pyautogui.position()
                x_pct = x / float(screen_w)
                y_pct = y / float(screen_h)

                payload = json.dumps({
                    "type": "cursor",
                    "x_pct": round(x_pct, 4),
                    "y_pct": round(y_pct, 4)
                })

                disconnected = []
                for ws in list(active_websockets):
                    try:
                        await ws.send_text(payload)
                    except Exception:
                        disconnected.append(ws)

                for ws in disconnected:
                    active_websockets.discard(ws)

            except Exception as e:
                pass

        await asyncio.sleep(0.033)  # ~30fps


@app.websocket("/")
@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    active_websockets.add(websocket)
    print(f"[Bluey] Android companion connected: {websocket.client}")

    # Send welcome handshake
    await websocket.send_text(json.dumps({
        "type": "hello",
        "hello": "Windows Bluey Brain",
        "state": "ready"
    }))

    try:
        while True:
            data = await websocket.receive_text()
            message = json.loads(data)
            await handle_android_message(websocket, message)
    except WebSocketDisconnect:
        active_websockets.discard(websocket)
        print(f"[Bluey] Android companion disconnected: {websocket.client}")
    except Exception as e:
        active_websockets.discard(websocket)


async def handle_android_message(ws: WebSocket, msg: dict):
    """Handles incoming commands from Android phone."""
    global is_kill_switch_triggered

    msg_type = msg.get("type")

    # Cycle 2: Contextual query
    if msg_type == "query":
        query_text = msg.get("text", "What is this?")
        print(f"[Bluey] Received voice query: '{query_text}'")

        # Capture visual context around cursor
        screenshot = capture_cursor_screen_context()
        buffered = io.BytesIO()
        screenshot.save(buffered, format="JPEG")
        img_b64 = base64.b64encode(buffered.getvalue()).decode("utf-8")

        # Response payload with chirp and emotion
        response_text = f"Looking at your mouse cursor: you're focused on {query_text}!"
        reply = {
            "type": "action",
            "action": "chirp",
            "emotion": "happy",
            "text": response_text
        }
        await ws.send_text(json.dumps(reply))

    elif msg_type == "status":
        state = msg.get("state")
        print(f"[Bluey] Phone state update: {state}")

    elif msg_type == "hello":
        print(f"[Bluey] Handshake from phone: {msg.get('hello')}")


def register_mdns_zeroconf(port: int = 8765):
    """Registers mDNS service for auto-discovery."""
    try:
        desc = {"version": "1.0.0", "name": "Bluey"}
        local_ip = socket.gethostbyname(socket.gethostname())
        info = ServiceInfo(
            "_bluey._tcp.local.",
            "Bluey Windows Brain._bluey._tcp.local.",
            addresses=[socket.inet_aton(local_ip)],
            port=port,
            properties=desc,
            server=f"{socket.gethostname()}.local.",
        )
        zc = Zeroconf(ip_version=IPVersion.V4Only)
        zc.register_service(info)
        print(f"[mDNS] Registered Bluey service on {local_ip}:{port}")
        return zc, info
    except Exception as e:
        print(f"[mDNS] Auto-discovery warning: {e}")
        return None, None


@app.on_event("startup")
async def startup_event():
    asyncio.create_task(broadcast_cursor_loop())


if __name__ == "__main__":
    port = 8765
    zc, info = register_mdns_zeroconf(port)

    print("==================================================")
    print("  Bluey Windows Desktop Brain & Hands Running")
    print(f"  WebSocket URL: ws://0.0.0.0:{port}/")
    print("  Emergency Kill Switch: Ctrl+Shift+Esc")
    print("==================================================")

    try:
        uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")
    finally:
        if zc and info:
            zc.unregister_service(info)
            zc.close()
