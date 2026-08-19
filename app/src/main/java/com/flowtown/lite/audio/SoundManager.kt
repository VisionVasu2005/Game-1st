package com.flowtown.lite.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

object SoundManager {

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    var isSoundEnabled: Boolean = true

    // 1. Crisp pop when connecting road nodes
    fun playNodeConnect() {
        if (!isSoundEnabled) return
        audioScope.launch {
            val durationMs = 45
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // Pitch sweep 700Hz -> 1400Hz
                val freq = 700.0 + (700.0 * (i.toDouble() / numSamples))
                val sample = sin(2.0 * PI * freq * t)
                val envelope = (1.0 - (i.toDouble() / numSamples)) // Decay
                buffer[i] = (sample * envelope * 22000).toInt().toShort()
            }
            playPcmBuffer(buffer)
        }
    }

    // 2. Realistic tire screech / drift squeal sound
    fun playDriftSound() {
        if (!isSoundEnabled) return
        audioScope.launch {
            val durationMs = 120
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // Modulated squeal with friction noise
                val tone = sin(2.0 * PI * 1850.0 * t) * 0.6 + sin(2.0 * PI * 2400.0 * t) * 0.4
                val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.35
                val envelope = sin(PI * (i.toDouble() / numSamples)) // smooth bell curve
                buffer[i] = ((tone + noise) * envelope * 18000).toInt().toShort()
            }
            playPcmBuffer(buffer)
        }
    }

    // 3. Cheerful melodic chime on delivery success 🔔
    fun playDeliverySuccess() {
        if (!isSoundEnabled) return
        audioScope.launch {
            val durationMs = 280
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
            val noteDuration = numSamples / notes.size

            for (i in 0 until numSamples) {
                val noteIdx = (i / noteDuration).coerceIn(0, notes.size - 1)
                val freq = notes[noteIdx]
                val localI = i % noteDuration
                val t = localI.toDouble() / SAMPLE_RATE
                val sample = sin(2.0 * PI * freq * t) + 0.3 * sin(4.0 * PI * freq * t)
                val envelope = 1.0 - (localI.toDouble() / noteDuration)
                buffer[i] = (sample * envelope * 24000).toInt().toShort()
            }
            playPcmBuffer(buffer)
        }
    }

    // 4. Impact thud on collision 💥
    fun playCrashSound() {
        if (!isSoundEnabled) return
        audioScope.launch {
            val durationMs = 180
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val lowThud = sin(2.0 * PI * (120.0 - 80.0 * (i.toDouble() / numSamples)) * t)
                val crunch = (Random.nextDouble() * 2.0 - 1.0) * 0.7
                val envelope = 1.0 - (i.toDouble() / numSamples)
                buffer[i] = ((lowThud * 0.5 + crunch * 0.5) * envelope * 26000).toInt().toShort()
            }
            playPcmBuffer(buffer)
        }
    }

    // 5. Engine rev on simulation start
    fun playEngineRev() {
        if (!isSoundEnabled) return
        audioScope.launch {
            val durationMs = 150
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val freq = 180.0 + (320.0 * (i.toDouble() / numSamples))
                val sample = sin(2.0 * PI * freq * t) + 0.4 * sin(4.0 * PI * freq * t)
                val envelope = (i.toDouble() / numSamples).coerceAtMost(1.0 - (i.toDouble() / numSamples)) * 2.0
                buffer[i] = (sample * envelope * 20000).toInt().toShort()
            }
            playPcmBuffer(buffer)
        }
    }

    private fun playPcmBuffer(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            audioTrack.setNotificationMarkerPosition(buffer.size)
            audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(track: AudioTrack?) {}
                override fun onMarkerReached(track: AudioTrack?) {
                    track?.release()
                }
            })
        } catch (_: Exception) {
            // Graceful fallback on devices without audio hardware
        }
    }
}
