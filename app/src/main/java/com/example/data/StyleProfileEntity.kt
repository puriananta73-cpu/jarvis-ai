package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "style_profiles")
data class StyleProfileEntity(
    @PrimaryKey
    val id: Long = 1L,
    val profileName: String = "Ananta's Personal Style",
    val tone: String = "Casual, friendly, direct",
    val topEmojis: String = "😂, 👍, 🚀, 🔥",
    val avgMessageLength: String = "Short & punchy (1-2 sentences)",
    val punctuationHabits: String = "Minimal punctuation, lowercase start often",
    val commonSignOffs: String = "Cheers, talk soon, catch ya",
    val rawAnalysisSummary: String = "No chat export analyzed yet. Paste your WhatsApp chat export below to train Jarvis in your style."
)
