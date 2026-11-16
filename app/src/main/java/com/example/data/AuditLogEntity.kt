package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // e.g., "VOICE_QUERY", "SEND_MESSAGE", "APP_CONTROL", "WEB_SEARCH"
    val recipient: String,  // e.g., "WhatsApp: Mom", "Instagram: John", "System"
    val content: String,    // prompt, drafted reply, or command executed
    val status: String      // e.g., "SUCCESS", "CONFIRMATION_PENDING", "BLOCKED_KILL_SWITCH", "ERROR"
)
