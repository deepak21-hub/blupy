package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.example.anim.FaceAnimator
import com.example.model.Mood
import com.example.model.Palette
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun FaceCanvas(
    frame: FaceAnimator.Frame,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawFace(frame)
    }
}

private fun DrawScope.drawFace(f: FaceAnimator.Frame) {
    drawRect(Color.Black)

    val isPortrait = size.height > size.width
    val designW = 844f
    val designH = 390f

    val scaleFactor: Float
    val transX: Float
    val transY: Float

    if (isPortrait) {
        // On portrait phones: scale Bluey so his face fits nicely with pleasant margins
        scaleFactor = (size.width / designW) * 1.08f
        transX = (size.width - designW * scaleFactor) / 2f
        // Position eyes around 38-42% down the screen, perfectly centered
        transY = size.height * 0.40f - 195f * scaleFactor
    } else {
        // On landscape displays: Bluey peeks up from the bottom edge
        scaleFactor = max(size.width / designW, size.height / designH)
        transX = (size.width - designW * scaleFactor) / 2f
        transY = size.height - designH * scaleFactor
    }

    translate(left = transX, top = transY) {
        scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
            val bounce = -f.talk * 16f + f.breathe * 3f - f.hop
            // Ensure body extends to bottom of screen in portrait
            val neededBottom = if (isPortrait) {
                max(744f, (size.height - transY) / scaleFactor + 60f)
            } else {
                744f
            }
            val body = Rect(12f, 44f + bounce, 832f, neededBottom + bounce)
            val pivot = Offset(body.left + body.width / 2f, body.bottom)

            translate(left = pivot.x, top = pivot.y) {
                rotateRad(radians = f.lean) {
                    val stretch = 1f + min(0.03f, max(-0.03f, f.hop / 900f))
                    scale(scaleX = 2f - stretch, scaleY = stretch, pivot = Offset.Zero) {
                        translate(left = -pivot.x, top = -pivot.y) {
                            drawBodyAndFace(f, body)
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawBodyAndFace(f: FaceAnimator.Frame, body: Rect) {
    val blob = BlobShape.createBlobPath(body)

    // Outer glow simulation
    drawPath(
        path = blob,
        color = Palette.Berry3.copy(alpha = 0.5f)
    )

    // 150-degree blueberry linear gradient
    val blobBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Palette.Berry1,
            0.34f to Palette.Berry2,
            0.68f to Palette.Berry3,
            1.0f to Palette.Berry4
        ),
        start = Offset(body.left + 0.25f * body.width, body.top + 0.067f * body.height),
        end = Offset(body.left + 0.75f * body.width, body.top + 0.933f * body.height)
    )
    drawPath(path = blob, brush = blobBrush)

    // Radial shine on forehead
    clipPath(blob) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(body.left + 246f, body.top + 126f),
                radius = 243f
            ),
            radius = 243f,
            center = Offset(body.left + 246f, body.top + 126f)
        )
        // Glossy sparkle oval
        drawOval(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(body.left + 190f, body.top + 40f),
            size = androidx.compose.ui.geometry.Size(46f, 22f)
        )
    }

    // Blueberry crown on top
    val crownRect = Rect(
        body.left + 368f,
        body.top - 20f - f.browLift * 0.3f,
        body.left + 452f,
        body.top + 36f - f.browLift * 0.3f
    )
    val crown = BlobShape.createCrownPath(crownRect)
    drawPath(crown, color = Palette.Nose)

    val eyeY = body.top + 157f
    val eyes = listOf(
        Offset(body.left + 280f, eyeY),
        Offset(body.left + 540f, eyeY)
    )
    val squash = 1f - f.talk * 0.12f

    // Blushy cheeks
    for (i in eyes.indices) {
        val side = if (i == 0) -1f else 1f
        val cheekCenter = Offset(eyes[i].x + side * 62f, eyes[i].y + 96f)
        drawOval(
            color = Palette.Blush.copy(alpha = 0.25f + 0.45f * f.blush),
            topLeft = Offset(cheekCenter.x - 45f, cheekCenter.y - 21f),
            size = androidx.compose.ui.geometry.Size(90f, 42f)
        )
    }

    // Eyebrows and Eyes
    for (i in eyes.indices) {
        val c = eyes[i]
        val side = if (i == 0) -1f else 1f

        // Eyebrows
        val browY = c.y - 112f - min(18f, max(-10f, f.browLift)) * 0.7f
        translate(left = c.x + side * 6f, top = browY) {
            val thinkingExtra = if (f.mood == Mood.THINKING && i == 1) -0.25f else 0f
            rotateRad(f.browTilt * side * -1f + thinkingExtra) {
                val arch = Path().apply {
                    moveTo(-38f, 6f)
                    quadraticTo(0f, -12f, 38f, 6f)
                }
                drawPath(
                    path = arch,
                    color = Palette.Nose,
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )
            }
        }

        // Eyes based on mood
        when {
            f.mood == Mood.RESTING -> {
                // Peaceful closed curve
                val shut = Path().apply {
                    moveTo(c.x - 62f, c.y + 6f)
                    quadraticTo(c.x, c.y + 52f, c.x + 62f, c.y + 6f)
                }
                drawPath(
                    path = shut,
                    color = Palette.Ink,
                    style = Stroke(width = 18f, cap = StrokeCap.Round)
                )
            }
            f.mood == Mood.HAPPY && f.squint > 0.6f -> {
                // Joyful upward curve
                val arc = Path().apply {
                    moveTo(c.x - 60f, c.y + 34f)
                    quadraticTo(c.x, c.y - 54f, c.x + 60f, c.y + 34f)
                }
                drawPath(
                    path = arc,
                    color = Palette.Ink,
                    style = Stroke(width = 24f, cap = StrokeCap.Round)
                )
            }
            else -> {
                val open = max(0.06f, 1f - f.closed) * squash
                val whiteRect = Rect(c.x - 95f, c.y - 95f * open, c.x + 95f, c.y + 95f * open)
                val whitePath = Path().apply { addOval(whiteRect) }
                drawPath(whitePath, Color.White)

                if (open > 0.2f) {
                    var gx = f.gazeX
                    var gy = f.gazeY
                    val len = hypot(gx, gy)
                    if (len > 1f) {
                        gx /= len
                        gy /= len
                    }

                    val reach = 50f
                    val r = 46f * f.pupil
                    var pupilCenter = Offset(
                        c.x + gx * reach - side * 3f,
                        c.y + gy * reach * open
                    )
                    if (f.mood == Mood.SLEEPY) {
                        pupilCenter = Offset(pupilCenter.x, max(pupilCenter.y, c.y) + 28f)
                    }

                    clipPath(whitePath) {
                        // Pupil ink
                        drawCircle(
                            color = Palette.Ink,
                            radius = r,
                            center = pupilCenter
                        )
                        // Catchlights
                        drawCircle(
                            color = Color.White,
                            radius = r * 0.28f,
                            center = Offset(pupilCenter.x - r * 0.58f, pupilCenter.y - r * 0.72f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = r * 0.12f,
                            center = Offset(pupilCenter.x + r * 0.28f, pupilCenter.y + r * 0.22f)
                        )

                        // Happy cheeks push up from below
                        if (f.squint > 0.02f) {
                            val lidRect = Rect(c.x - 110f, c.y + 95f * open - 70f * f.squint, c.x + 110f, c.y + 255f)
                            drawOval(
                                brush = blobBrush,
                                topLeft = Offset(lidRect.left, lidRect.top),
                                size = androidx.compose.ui.geometry.Size(lidRect.width, lidRect.height)
                            )
                        }

                        // Sleepy droopy lid
                        if (f.mood == Mood.SLEEPY) {
                            val droop = 0.5f + 0.06f * sin(f.time * 1.3f)
                            val lidH = whiteRect.height * droop + 10f
                            drawRect(
                                brush = blobBrush,
                                topLeft = Offset(c.x - 100f, whiteRect.top - 10f),
                                size = androidx.compose.ui.geometry.Size(200f, lidH)
                            )
                            drawLine(
                                color = Palette.Nose.copy(alpha = 0.7f),
                                start = Offset(c.x - 92f, whiteRect.top + whiteRect.height * droop),
                                end = Offset(c.x + 92f, whiteRect.top + whiteRect.height * droop),
                                strokeWidth = 8f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Pointing focus lid
                        if (f.mood == Mood.POINTING) {
                            drawRect(
                                brush = blobBrush,
                                topLeft = Offset(c.x - 100f, whiteRect.top - 20f),
                                size = androidx.compose.ui.geometry.Size(200f, 40f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Resting Z's
    if (f.mood == Mood.RESTING) {
        for (i in 0 until 3) {
            val phase = ((f.time * 0.35f + i / 3f) % 1f)
            val zAlpha = (0.9f * sin(PI.toFloat() * phase)).coerceIn(0f, 1f)
            val zX = body.left + 660f + phase * 70f + sin(phase * 6f) * 8f
            val zY = body.top + 90f - phase * 110f
            drawCircle(
                color = Color.White.copy(alpha = zAlpha * 0.7f),
                radius = 6f + i * 3f,
                center = Offset(zX, zY)
            )
        }
    }

    // Thinking dots
    if (f.mood == Mood.THINKING) {
        for (i in 0 until 3) {
            val wobble = sin(f.time * 3f + i.toFloat()) * 4f
            val dotCenter = Offset(body.left + 690f + i * 26f, body.top + 40f - i * 12f + wobble)
            drawCircle(
                color = Color.White.copy(alpha = (1f - i * 0.3f).coerceIn(0f, 1f)),
                radius = 8f,
                center = dotCenter
            )
        }
    }

    // Mouth / Nose
    val noseW = 60f - f.talk * 8f
    val noseH = 38f + f.talk * 34f
    val noseTopLeft = Offset(body.left + 410f - noseW / 2f, body.top + 272f - f.talk * 6f)

    if (f.mood == Mood.HAPPY && f.talk < 0.1f) {
        val smile = Path().apply {
            moveTo(noseTopLeft.x - 8f, noseTopLeft.y + 8f)
            quadraticTo(
                noseTopLeft.x + noseW / 2f, noseTopLeft.y + 58f,
                noseTopLeft.x + noseW + 8f, noseTopLeft.y + 8f
            )
            close()
        }
        drawPath(smile, Palette.Nose)
    } else {
        drawOval(
            color = Palette.Nose,
            topLeft = noseTopLeft,
            size = androidx.compose.ui.geometry.Size(noseW, noseH)
        )
        // Little pink tongue when talking
        if (f.talk > 0.25f) {
            val tongueW = noseW * 0.56f
            val tongueH = noseH * 0.3f
            drawOval(
                color = Palette.Blush.copy(alpha = 0.9f),
                topLeft = Offset(noseTopLeft.x + (noseW - tongueW) / 2f, noseTopLeft.y + noseH - tongueH),
                size = androidx.compose.ui.geometry.Size(tongueW, tongueH)
            )
        }
    }
}
