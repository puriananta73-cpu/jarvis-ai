package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class JarvisAiEngine {
    private val TAG = "JarvisAiEngine"

    private val model: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun processCommand(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            withTimeout(45_000L) {
                val systemPrompt = "You are Jarvis, Ananta's personal 24/7 always-on AI assistant on Android. " +
                        "Be concise, helpful, and natural when spoken aloud. If the user asks about live data, sports scores, or weather, provide direct, up-to-date sounding answers. " +
                        "Always maintain a respectful, professional, yet witty tone."
                
                val fullPrompt = "$systemPrompt\n\nUser command: $prompt"
                val response = model.generateContent(fullPrompt)
                response.text ?: "I'm sorry, I couldn't generate a response."
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI processing failed", e)
            "Error communicating with Jarvis AI brain: ${e.localizedMessage ?: "Unknown error"}"
        }
    }
}
