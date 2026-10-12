package com.example.network

import com.example.model.FaceState
import com.example.model.Mood
import com.example.model.WebSocketPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

enum class PhonePlacement(val label: String) {
    LEFT("Left of Monitor"),
    CENTER("Center Under Monitor"),
    RIGHT("Right of Monitor")
}

class BlueyWebSocketClient(
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var shouldReconnect = true

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _hostAddress = MutableStateFlow("10.0.2.2:8765")
    val hostAddress = _hostAddress.asStateFlow()

    private val _placement = MutableStateFlow(PhonePlacement.CENTER)
    val placement = _placement.asStateFlow()

    private val _cursorFlow = MutableSharedFlow<Pair<Float, Float>>(extraBufferCapacity = 64)
    val cursorFlow = _cursorFlow.asSharedFlow()

    private val _actionFlow = MutableSharedFlow<WebSocketPacket>(extraBufferCapacity = 16)
    val actionFlow = _actionFlow.asSharedFlow()

    fun updateHost(host: String) {
        _hostAddress.value = host
        reconnect()
    }

    fun setPlacement(placement: PhonePlacement) {
        _placement.value = placement
    }

    fun connect() {
        shouldReconnect = true
        startConnection()
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _status.value = ConnectionStatus.DISCONNECTED
    }

    private fun reconnect() {
        disconnect()
        connect()
    }

    private fun startConnection() {
        val host = _hostAddress.value.trim()
        val url = if (host.startsWith("ws://") || host.startsWith("wss://")) host else "ws://$host"

        _status.value = ConnectionStatus.CONNECTING

        val request = try {
            Request.Builder().url(url).build()
        } catch (_: Exception) {
            _status.value = ConnectionStatus.DISCONNECTED
            return
        }

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = ConnectionStatus.CONNECTED
                // Send handshake hello
                val helloPacket = WebSocketPacket(
                    type = "hello",
                    hello = "Bluey Android Companion",
                    state = "awake"
                )
                webSocket.send(json.encodeToString(WebSocketPacket.serializer(), helloPacket))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _status.value = ConnectionStatus.DISCONNECTED
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _status.value = ConnectionStatus.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(3000)
            if (shouldReconnect && _status.value != ConnectionStatus.CONNECTED) {
                startConnection()
            }
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val packet = json.decodeFromString(WebSocketPacket.serializer(), text)

            // Cycle 1: Spatial Eye-Tracking
            if (packet.type == "cursor" && packet.x_pct != null && packet.y_pct != null) {
                val rawX = packet.x_pct
                val rawY = packet.y_pct

                // Offset based on phone physical position under monitor
                val offset = when (_placement.value) {
                    PhonePlacement.LEFT -> 0.25f
                    PhonePlacement.CENTER -> 0.0f
                    PhonePlacement.RIGHT -> -0.25f
                }

                // Map 0..1 percentage to -1..1 gaze units with calibration
                val gazeX = ((rawX * 2f - 1f) + offset).coerceIn(-1f, 1f)
                val gazeY = (rawY * 1.6f - 1f).coerceIn(-1f, 0.4f) // Look up toward monitor above

                _cursorFlow.tryEmit(Pair(gazeX, gazeY))
            }

            // Cycle 2, 3, 4 actions & events
            _actionFlow.tryEmit(packet)
        } catch (_: Exception) {
            // Ignore malformed packet
        }
    }

    fun sendPacket(packet: WebSocketPacket) {
        val ws = webSocket
        if (ws != null && _status.value == ConnectionStatus.CONNECTED) {
            try {
                val str = json.encodeToString(WebSocketPacket.serializer(), packet)
                ws.send(str)
            } catch (_: Exception) { }
        }
    }

    fun sendStatus(state: String) {
        sendPacket(WebSocketPacket(type = "status", state = state))
    }

    fun sendQuery(queryText: String) {
        sendPacket(WebSocketPacket(type = "query", text = queryText))
    }
}
