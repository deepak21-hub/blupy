package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketPacket(
    val type: String? = null,
    val command: String? = null,
    val gazeX: Float? = null,
    val gazeY: Float? = null,
    val x_pct: Float? = null,
    val y_pct: Float? = null,
    val state: String? = null,
    val mood: String? = null,
    val action: String? = null,
    val emotion: String? = null,
    val text: String? = null,
    val audio: String? = null,
    val hello: String? = null,
    val volume: Float? = null
)
