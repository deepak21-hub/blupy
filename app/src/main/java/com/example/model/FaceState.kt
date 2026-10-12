package com.example.model

enum class Mood(val title: String) {
    LISTENING("Listening"),
    RESTING("Resting"),
    THINKING("Thinking"),
    TALKING("Talking"),
    POINTING("Pointing"),
    HAPPY("Happy"),
    SLEEPY("Sleepy")
}

data class FaceState(
    val gazeX: Float = 0f,
    val gazeY: Float = 0f,
    val mood: Mood = Mood.LISTENING,
    val talk: Float = 0f
)
