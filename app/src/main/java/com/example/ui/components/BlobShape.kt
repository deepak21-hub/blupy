package com.example.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class BlobShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(createBlobPath(Rect(0f, 0f, size.width, size.height)))
    }

    companion object {
        fun createBlobPath(r: Rect): Path {
            val w = r.width
            val h = r.height
            val tlW = 0.52f * w
            val tlH = 0.58f * h
            val trW = 0.48f * w
            val trH = 0.56f * h
            val brW = 0.46f * w
            val brH = 0.44f * h
            val blW = 0.54f * w
            val blH = 0.42f * h
            val k = 0.5523f

            val path = Path()
            path.moveTo(r.left + tlW, r.top)
            path.lineTo(r.right - trW, r.top)

            path.cubicTo(
                r.right - trW * (1f - k), r.top,
                r.right, r.top + trH * (1f - k),
                r.right, r.top + trH
            )

            path.lineTo(r.right, r.bottom - brH)

            path.cubicTo(
                r.right, r.bottom - brH * (1f - k),
                r.right - brW * (1f - k), r.bottom,
                r.right - brW, r.bottom
            )

            path.lineTo(r.left + blW, r.bottom)

            path.cubicTo(
                r.left + blW * (1f - k), r.bottom,
                r.left, r.bottom - blH * (1f - k),
                r.left, r.bottom - blH
            )

            path.lineTo(r.left, r.top + tlH)

            path.cubicTo(
                r.left, r.top + tlH * (1f - k),
                r.left + tlW * (1f - k), r.top,
                r.left + tlW, r.top
            )

            path.close()
            return path
        }

        fun createCrownPath(r: Rect): Path {
            val pts = listOf(
                Pair(30f, 4f),
                Pair(36f, 16f),
                Pair(52f, 12f),
                Pair(40f, 24f),
                Pair(46f, 36f),
                Pair(30f, 28f),
                Pair(14f, 36f),
                Pair(20f, 24f),
                Pair(8f, 12f),
                Pair(24f, 16f)
            )
            val path = Path()
            for ((i, pt) in pts.withIndex()) {
                val x = r.left + (pt.first / 60f) * r.width
                val y = r.top + (pt.second / 40f) * r.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            return path
        }
    }
}
