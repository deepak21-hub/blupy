package com.example.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Authentic blueberry color palette from Bluey design.
 */
object Palette {
    val Berry1 = Color(0xFFA9BCFF) // light periwinkle
    val Berry2 = Color(0xFF6C86F5) // vibrant blueberry
    val Berry3 = Color(0xFF4254D6) // rich blueberry
    val Berry4 = Color(0xFF2B2F8F) // deep indigo
    val Nose = Color(0xFF1C1F66)   // dark blueberry
    val Ink = Color(0xFF17151F)    // deep eye ink
    val InkSoft = Color(0xFFB9B2CC)// soft muted text
    val Panel = Color(0xFF1E1B29)  // dark card panel
    val Green = Color(0xFF5BE49B)  // connection active / live
    val Rose = Color(0xFFE5547A)   // end session rose
    val Blush = Color(0xFFF3A6D8)  // happy cheek blush
    val Yellow = Color(0xFFFFD66B) // waking up warm yellow
    val Purple = Color(0xFFC79BFF) // thinking purple
    val Pink = Color(0xFFFF9AD0)   // replying pink
    val Blue = Color(0xFF4F8BFF)   // listening blue

    val GradientColors = listOf(Berry1, Berry2, Berry3, Berry4)
    val GradientStops = listOf(0f, 0.34f, 0.68f, 1f)

    fun blobBrush(): Brush {
        return Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to Berry1,
                0.34f to Berry2,
                0.68f to Berry3,
                1.0f to Berry4
            )
        )
    }
}
