package com.colormagic.kids.presentation.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Tiny, asset-free celebration sound for delight moments (save success, streak
 * advance). Uses the system [ToneGenerator] so there are no audio files to
 * bundle. Wrapped in runCatching — audio is a nice-to-have, never a crash risk.
 *
 * To upgrade to richer custom SFX later, drop .ogg files in res/raw and swap
 * this for a SoundPool-backed implementation; the call sites stay the same.
 */
object CelebrationFx {

    fun playSuccess() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
            // A short positive acknowledgement chime.
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 300)
            // Release a moment later so the tone finishes playing.
            Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 700)
        }
    }

    private const val VOLUME = 80 // 0..100
}
