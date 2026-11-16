package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class StyleAnalyzerEngine {
    private val TAG = "StyleAnalyzerEngine"

    private val model: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun analyzeChatExport(chatText: String): StyleResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(60_000L) {
                val prompt = "Analyze the following WhatsApp chat export and extract the user's texting style profile. " +
                        "Identify tone, top emojis used, average message length style, punctuation habits, and common sign-offs. " +
                        "Return a valid JSON object with keys: tone, topEmojis, avgMessageLength, punctuationHabits, commonSignOffs, summary.\n\n" +
                        "Chat Export:\n$chatText"

                val response = model.generateContent(prompt)
                val text = response.text ?: return@withTimeout defaultResult("Empty response from AI")

                try {
                    val cleanJson = text.trim().removePrefix("```json").removeSuffix("```").trim()
                    val json = JSONObject(cleanJson)
                    StyleResult(
                        tone = json.optString("tone", "Casual and direct"),
                        topEmojis = json.optString("topEmojis", "😂, 👍"),
                        avgMessageLength = json.optString("avgMessageLength", "Short & punchy"),
                        punctuationHabits = json.optString("punctuationHabits", "Casual punctuation"),
                        commonSignOffs = json.optString("commonSignOffs", "Cheers"),
                        summary = json.optString("summary", "Successfully analyzed chat export.")
                    )
                } catch (e: Exception) {
                    StyleResult(
                        tone = "Custom analyzed",
                        topEmojis = "👍, 😂",
                        avgMessageLength = "Standard",
                        punctuationHabits = "Normal",
                        commonSignOffs = "Best",
                        summary = text
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Style analysis failed", e)
            defaultResult("Failed to analyze: ${e.localizedMessage}")
        }
    }

    suspend fun draftReplyInStyle(incomingMessage: String, profileSummary: String, signature: String): String = withContext(Dispatchers.IO) {
        try {
            withTimeout(30_000L) {
                val prompt = "You are drafting a WhatsApp reply on behalf of Ananta, matching his exact texting style.\n" +
                        "User Style Profile: $profileSummary\n" +
                        "Incoming Message: \"$incomingMessage\"\n" +
                        "Draft a short reply in Ananta's style. Append this disclosure signature at the end: \" $signature\""

                val response = model.generateContent(prompt)
                response.text ?: "Hey, sounds good! $signature"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Draft reply failed", e)
            "Got it! $signature"
        }
    }

    private fun defaultResult(msg: String) = StyleResult(
        tone = "Default",
        topEmojis = "👍",
        avgMessageLength = "Short",
        punctuationHabits = "Standard",
        commonSignOffs = "Cheers",
        summary = msg
    )
}

data class StyleResult(
    val tone: String,
    val topEmojis: String,
    val avgMessageLength: String,
    val punctuationHabits: String,
    val commonSignOffs: String,
    val summary: String
)
