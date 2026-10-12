package com.example.anim

import com.example.model.FaceState
import com.example.model.Mood
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class Spring(
    var value: Float,
    var velocity: Float = 0f,
    val stiffness: Float = 200f,
    val damping: Float = 0.7f
) {
    fun step(target: Float, dt: Float) {
        val c = 2f * sqrt(stiffness) * damping
        velocity += (stiffness * (target - value) - c * velocity) * dt
        value += velocity * dt
    }
}

class FaceAnimator {
    data class Frame(
        val gazeX: Float,
        val gazeY: Float,
        val mood: Mood,
        val talk: Float,
        val closed: Float,
        val breathe: Float,
        val time: Float,
        val pupil: Float,
        val browLift: Float,
        val browTilt: Float,
        val lean: Float,
        val hop: Float,
        val blush: Float,
        val squint: Float
    )

    private var target = FaceState()
    private var lastPacket = -100.0

    private val gazeX = Spring(0f, stiffness = 260f, damping = 0.82f)
    private val gazeY = Spring(0f, stiffness = 260f, damping = 0.82f)
    private val pupil = Spring(1f, stiffness = 160f, damping = 0.5f)
    private val browLift = Spring(0f, stiffness = 180f, damping = 0.55f)
    private val browTilt = Spring(0f, stiffness = 150f, damping = 0.6f)
    private val lean = Spring(0f, stiffness = 60f, damping = 0.8f)
    private val hop = Spring(0f, stiffness = 260f, damping = 0.35f)
    private val blush = Spring(0.25f, stiffness = 40f, damping = 1f)
    private val squint = Spring(0f, stiffness = 150f, damping = 0.7f)

    private var talkVal = 0f
    private var currentMood: Mood = Mood.LISTENING
    private var blinkStart = -1.0
    private var doubleBlink = false
    private var nextBlink = 2.0
    private var lastTime: Double? = null
    private var nextSaccade = 0.0
    private var wanderTargetX = 0f
    private var wanderTargetY = -0.3f
    private var jitterX = 0f
    private var jitterY = 0f
    private var nextJitter = 0.0
    private var nextHop = 6.0

    var touchGazeX: Float? = null
    var touchGazeY: Float? = null
    var localMood: Mood? = null
    var localTalkLevel: Float = 0f
    var isAwake = false

    fun receiveRemote(face: FaceState, now: Double) {
        target = face
        lastPacket = now
    }

    fun step(now: Double): Frame {
        val dt = min(((now - (lastTime ?: now))).toFloat(), 1f / 20f)
        lastTime = now

        val isLive = (now - lastPacket) < 2.5
        val wantedMood = localMood ?: if (isLive) target.mood else Mood.LISTENING
        if (wantedMood != currentMood) {
            currentMood = wantedMood
            blinkStart = now
            if (currentMood != Mood.SLEEPY && currentMood != Mood.RESTING) {
                hop.velocity += 260f
                pupil.velocity += 3f
            }
        }

        var wantX: Float
        var wantY: Float
        val tgX = touchGazeX
        val tgY = touchGazeY

        if (tgX != null && tgY != null) {
            wantX = tgX
            wantY = tgY
        } else if (isLive) {
            wantX = target.gazeX
            wantY = target.gazeY
        } else {
            if (now > nextSaccade) {
                wanderTargetX = Random.nextDouble(-0.8, 0.8).toFloat()
                wanderTargetY = Random.nextDouble(-0.7, 0.3).toFloat()
                nextSaccade = now + Random.nextDouble(0.6, 2.2)
            }
            wantX = wanderTargetX
            wantY = wanderTargetY
        }

        if (currentMood == Mood.THINKING) {
            wantX = (0.55 + 0.08 * sin(now * 1.3)).toFloat()
            wantY = -0.8f
        }

        if (now > nextJitter) {
            val amount = if (!isAwake) 0.015f else when (currentMood) {
                Mood.TALKING -> 0.06f
                Mood.THINKING -> 0.045f
                else -> 0.02f
            }
            jitterX = Random.nextDouble(-amount.toDouble(), amount.toDouble()).toFloat()
            jitterY = Random.nextDouble(-amount.toDouble(), amount.toDouble()).toFloat()
            nextJitter = now + Random.nextDouble(0.5, 1.4)
        }

        gazeX.step(wantX + jitterX, dt)
        gazeY.step(wantY + jitterY, dt)

        var wantTalk = if (isLive) target.talk else if (localMood == Mood.TALKING) {
            (0.5 + 0.5 * sin(now * 19) * sin(now * 7.3)).toFloat()
        } else 0f
        wantTalk = max(wantTalk, localTalkLevel)
        talkVal += (wantTalk - talkVal) * min(1f, dt * 25f)

        var wantPupil = 1.0f
        var wantLift = 0f
        var wantTilt = 0f
        var wantBlush = 0.25f
        var wantSquint = 0f

        when (currentMood) {
            Mood.LISTENING -> {
                wantPupil = 1.12f
                wantLift = 10f
            }
            Mood.TALKING -> {
                wantPupil = 1.05f
                wantLift = 6f + talkVal * 18f
                wantTilt = (0.05 * sin(now * 2.3)).toFloat()
            }
            Mood.POINTING -> {
                wantPupil = 0.95f
                wantLift = -4f
                wantTilt = -0.14f
            }
            Mood.THINKING -> {
                wantPupil = 0.9f
                wantLift = 4f
                wantTilt = 0.22f
            }
            Mood.HAPPY -> {
                wantPupil = 1.2f
                wantLift = 16f
                wantBlush = 0.85f
                wantSquint = 1f
            }
            Mood.RESTING -> {
                wantPupil = 0.9f
                wantLift = -8f
                wantBlush = 0.15f
            }
            Mood.SLEEPY -> {
                wantPupil = 0.88f
                wantLift = -10f
                wantTilt = 0.12f
                wantBlush = 0.15f
            }
        }

        pupil.step(wantPupil, dt)
        browLift.step(wantLift, dt)
        browTilt.step(wantTilt, dt)
        blush.step(wantBlush, dt)
        squint.step(wantSquint, dt)
        lean.step(-gazeX.value * 0.045f, dt)

        if (now > nextHop) {
            if (talkVal < 0.05f && currentMood != Mood.SLEEPY && currentMood != Mood.RESTING) {
                hop.velocity += Random.nextDouble(180.0, 320.0).toFloat()
            }
            nextHop = now + Random.nextDouble(7.0, 14.0)
        }
        hop.step(0f, dt)

        val drowsy = currentMood == Mood.SLEEPY
        if (now > nextBlink) {
            blinkStart = now
            doubleBlink = !drowsy && Random.nextDouble(0.0, 1.0) < 0.25
            nextBlink = now + if (drowsy) Random.nextDouble(1.6, 3.0) else Random.nextDouble(2.0, 5.0)
        }

        val blinkDuration = if (drowsy) 0.7 else 0.15
        val p = ((now - blinkStart) / blinkDuration).toFloat()
        var closed = if (p in 0f..1f) sin(PI.toFloat() * p) else 0f
        if (doubleBlink && p in 1.3f..2.3f) {
            closed = sin(PI.toFloat() * (p - 1.3f))
        }

        val breathe = sin(now * if (currentMood == Mood.RESTING) 1.1 else 1.8).toFloat()

        return Frame(
            gazeX = gazeX.value,
            gazeY = gazeY.value,
            mood = currentMood,
            talk = talkVal,
            closed = closed,
            breathe = breathe,
            time = now.toFloat(),
            pupil = pupil.value,
            browLift = browLift.value,
            browTilt = browTilt.value,
            lean = lean.value,
            hop = hop.value,
            blush = blush.value,
            squint = max(0f, min(1f, squint.value))
        )
    }
}
