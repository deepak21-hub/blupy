package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

class ChirpSynthesizer {

    private val sampleRate = 24000

    suspend fun playChirp(volume: Float = 1.0f) = withContext(Dispatchers.Default) {
        try {
            val durationSec = 0.22f
            val numSamples = (sampleRate * durationSec).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                // Cheerful double-peaked frequency sweep
                val freq = if (t < 0.11f) {
                    val p = t / 0.11f
                    650.0 + 950.0 * p
                } else {
                    val p = (t - 0.11f) / 0.11f
                    1100.0 + 1300.0 * p
                }

                // Envelope to avoid click
                val env = sin((PI * (i.toDouble() / numSamples))).toFloat()
                val sample = (sin(2.0 * PI * freq * t) * env * 32767.0 * volume).toInt()
                buffer[i] = sample.coerceIn(-32768, 32767).toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // Wait for playback and release
            kotlinx.coroutines.delay(260)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {
            // AudioTrack failure fallback gracefully
        }
    }
}
