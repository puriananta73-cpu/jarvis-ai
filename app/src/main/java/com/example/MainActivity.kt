package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.JarvisApp
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.example.viewmodel.JarvisViewModel
import com.example.voice.VoiceEngine

class MainActivity : ComponentActivity() {
    private var voiceEngine: VoiceEngine? = null
    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            configureAppCheck(this, intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "AppCheck init error", e)
        }

        try {
            voiceEngine = VoiceEngine(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "VoiceEngine init error", e)
        }

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                JarvisApp(
                    viewModel = viewModel,
                    onSpeak = { text -> voiceEngine?.speak(text) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            voiceEngine?.shutdown()
        } catch (e: Exception) {
            Log.e("MainActivity", "VoiceEngine shutdown error", e)
        }
    }
}
