package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.JarvisAiEngine
import com.example.ai.StyleAnalyzerEngine
import com.example.data.AuditLogEntity
import com.example.data.JarvisDatabase
import com.example.data.StyleProfileEntity
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisNotificationListener
import com.example.util.AppController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IncomingMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String,
    val message: String,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class JarvisUiState(
    val isListening: Boolean = true,
    val isKillSwitchActive: Boolean = false,
    val currentResponse: String = "Hello Ananta! I am Jarvis, your 24/7 AI assistant. How can I help you today?",
    val isProcessing: Boolean = false,
    val securityPin: String = "1234",
    val isPinRequiredForSend: Boolean = false, // Authentication gate removed per user request
    val disclosureSignature: String = "-- Sent by Jarvis, AI assistant for Ananta",
    val quietHoursEnabled: Boolean = false,
    val selectedTab: Int = 0,
    val isAnalyzingStyle: Boolean = false,
    val draftedReply: String = "",
    val incomingTestMessage: String = "Hey Ananta, are we still meeting for coffee at 3pm?",
    val inboxQueue: List<IncomingMessage> = listOf(
        IncomingMessage(sender = "Mom (WhatsApp)", message = "Where are you? Call me when free.", appName = "WhatsApp"),
        IncomingMessage(sender = "Alex (Instagram)", message = "Check out this reel I sent u 🔥", appName = "Instagram")
    ),
    val activeDraftMessage: IncomingMessage? = null,
    val generatedDraftForQueue: String = ""
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private val database = JarvisDatabase.getDatabase(application)
    private val auditLogDao = database.auditLogDao()
    private val styleProfileDao = database.styleProfileDao()

    private val aiEngine = JarvisAiEngine()
    private val styleAnalyzer = StyleAnalyzerEngine()

    val auditLogs = auditLogDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val styleProfile = styleProfileDao.getStyleProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StyleProfileEntity())

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    init {
        JarvisNotificationListener.onMessageIntercepted = { sender, message, appName ->
            val newMsg = IncomingMessage(sender = "$sender ($appName)", message = message, appName = appName)
            _uiState.update { state ->
                state.copy(inboxQueue = listOf(newMsg) + state.inboxQueue)
            }
        }
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleKillSwitch() {
        _uiState.update { state ->
            val newState = !state.isKillSwitchActive
            if (newState) {
                viewModelScope.launch {
                    auditLogDao.insertLog(
                        AuditLogEntity(
                            actionType = "KILL_SWITCH",
                            recipient = "System",
                            content = "Jarvis listening and actions halted by master kill switch.",
                            status = "BLOCKED_KILL_SWITCH"
                        )
                    )
                }
            }
            state.copy(isKillSwitchActive = newState, isListening = !newState)
        }
    }

    fun toggleListening() {
        if (_uiState.value.isKillSwitchActive) return
        _uiState.update { it.copy(isListening = !it.isListening) }
    }

    fun processUserPrompt(prompt: String, onSpokenResult: (String) -> Unit) {
        if (_uiState.value.isKillSwitchActive) {
            _uiState.update { it.copy(currentResponse = "Master Kill Switch is active. Jarvis is offline.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, currentResponse = "Thinking...") }

            auditLogDao.insertLog(
                AuditLogEntity(
                    actionType = "VOICE_QUERY",
                    recipient = "Jarvis AI",
                    content = prompt,
                    status = "SUCCESS"
                )
            )

            val lowerPrompt = prompt.lowercase()
            val answer = if (lowerPrompt.startsWith("open ")) {
                val targetApp = lowerPrompt.substringAfter("open ").trim()
                val success = AppController.openAppByName(getApplication(), targetApp)
                if (success) {
                    "Opening $targetApp for you, Ananta."
                } else {
                    aiEngine.processCommand(prompt)
                }
            } else {
                aiEngine.processCommand(prompt)
            }

            _uiState.update { it.copy(isProcessing = false, currentResponse = answer) }
            onSpokenResult(answer)

            auditLogDao.insertLog(
                AuditLogEntity(
                    actionType = "AI_RESPONSE",
                    recipient = "Ananta",
                    content = answer,
                    status = "SUCCESS"
                )
            )
        }
    }

    fun selectQueueMessage(msg: IncomingMessage) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeDraftMessage = msg, generatedDraftForQueue = "Drafting reply...") }
            
            val currentProfile = styleProfile.value ?: StyleProfileEntity()
            val profileSummary = "Tone: ${currentProfile.tone}, Emojis: ${currentProfile.topEmojis}, Length: ${currentProfile.avgMessageLength}"
            val draft = styleAnalyzer.draftReplyInStyle(msg.message, profileSummary, _uiState.value.disclosureSignature)

            _uiState.update { it.copy(generatedDraftForQueue = draft) }
        }
    }

    fun dismissQueueMessage(msgId: Long) {
        _uiState.update { state ->
            state.copy(
                inboxQueue = state.inboxQueue.filter { it.id != msgId },
                activeDraftMessage = if (state.activeDraftMessage?.id == msgId) null else state.activeDraftMessage
            )
        }
    }

    fun approveAndSendReply(msg: IncomingMessage, finalReply: String) {
        viewModelScope.launch {
            JarvisAccessibilityService.sendMessageInCurrentApp(finalReply) { success, resultMessage ->
                viewModelScope.launch {
                    auditLogDao.insertLog(
                        AuditLogEntity(
                            actionType = "ACCESSIBILITY_SEND",
                            recipient = msg.sender,
                            content = "Reply: '$finalReply' | Result: $resultMessage",
                            status = if (success) "SUCCESS" else "GRACEFUL_FAILURE"
                        )
                    )
                }
            }
            dismissQueueMessage(msg.id)
            _uiState.update { it.copy(activeDraftMessage = null, generatedDraftForQueue = "") }
        }
    }

    fun analyzeChatExport(chatText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingStyle = true) }
            val result = styleAnalyzer.analyzeChatExport(chatText)

            val newProfile = StyleProfileEntity(
                id = 1L,
                profileName = "Ananta's Ingested Style",
                tone = result.tone,
                topEmojis = result.topEmojis,
                avgMessageLength = result.avgMessageLength,
                punctuationHabits = result.punctuationHabits,
                commonSignOffs = result.commonSignOffs,
                rawAnalysisSummary = result.summary
            )
            styleProfileDao.insertOrUpdateProfile(newProfile)

            auditLogDao.insertLog(
                AuditLogEntity(
                    actionType = "STYLE_ANALYSIS",
                    recipient = "Style Engine",
                    content = "Analyzed WhatsApp chat export and updated style profile.",
                    status = "SUCCESS"
                )
            )

            _uiState.update { it.copy(isAnalyzingStyle = false) }
        }
    }

    fun testDraftReply(incoming: String) {
        viewModelScope.launch {
            val currentProfile = styleProfile.value ?: StyleProfileEntity()
            val profileSummary = "Tone: ${currentProfile.tone}, Emojis: ${currentProfile.topEmojis}, Length: ${currentProfile.avgMessageLength}, Punctuation: ${currentProfile.punctuationHabits}"
            val draft = styleAnalyzer.draftReplyInStyle(incoming, profileSummary, _uiState.value.disclosureSignature)
            
            _uiState.update { it.copy(incomingTestMessage = incoming, draftedReply = draft) }

            auditLogDao.insertLog(
                AuditLogEntity(
                    actionType = "STYLE_DRAFT",
                    recipient = "Test Contact",
                    content = "Drafted reply for '$incoming': $draft",
                    status = "SUCCESS"
                )
            )
        }
    }

    fun updateSettings(pin: String, requirePin: Boolean, signature: String, quietHours: Boolean) {
        _uiState.update {
            it.copy(
                securityPin = pin,
                isPinRequiredForSend = false, // Keep disabled per user request
                disclosureSignature = signature,
                quietHoursEnabled = quietHours
            )
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            auditLogDao.clearLogs()
        }
    }
}
