package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceEngine(context: Context, private val onTtsInitialized: () -> Unit = {}) {
    private val TAG = "VoiceEngine"
    private var tts: TextToSpeech? = null
    var isTtsReady = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS Language not supported")
                } else {
                    isTtsReady = true
                    onTtsInitialized()
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    fun speak(text: TextToSpeech, message: String) {
        // Handled via instance
    }

    fun speak(message: String) {
        if (isTtsReady) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "JarvisUtteranceId")
        } else {
            Log.w(TAG, "TTS not ready yet")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
