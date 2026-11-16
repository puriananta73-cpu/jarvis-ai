package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuditLogEntity
import com.example.data.StyleProfileEntity
import com.example.viewmodel.IncomingMessage
import com.example.viewmodel.JarvisUiState
import com.example.viewmodel.JarvisViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisApp(viewModel: JarvisViewModel, onSpeak: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.auditLogs.collectAsState()
    val styleProfile by viewModel.styleProfile.collectAsState()

    var promptInput by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pendingActionText by remember { mutableStateOf("") }

    // Settings state temporary variables
    var editPin by remember { mutableStateOf(uiState.securityPin) }
    var editSignature by remember { mutableStateOf(uiState.disclosureSignature) }
    var editQuietHours by remember { mutableStateOf(uiState.quietHoursEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Jarvis AI",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("app_title")
                        )
                    }
                },
                actions = {
                    // Kill Switch Button (Master Emergency Stop)
                    IconButton(
                        onClick = { viewModel.toggleKillSwitch() },
                        modifier = Modifier.testTag("kill_switch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Kill Switch",
                            tint = if (uiState.isKillSwitchActive) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_navigation")) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                    label = { Text("Core") },
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    modifier = Modifier.testTag("nav_core_tab")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
                    label = { Text("Inbox") },
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    modifier = Modifier.testTag("nav_inbox_tab")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Audit") },
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setTab(2) },
                    modifier = Modifier.testTag("nav_audit_tab")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Style, contentDescription = null) },
                    label = { Text("Style") },
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.setTab(3) },
                    modifier = Modifier.testTag("nav_style_tab")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = uiState.selectedTab == 4,
                    onClick = { viewModel.setTab(4) },
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.selectedTab) {
                0 -> DashboardTab(uiState, onPromptSubmit = { text ->
                    if (uiState.isPinRequiredForSend) {
                        pendingActionText = text
                        showPinDialog = true
                    } else {
                        viewModel.processUserPrompt(text, onSpeak)
                    }
                }, onToggleListening = { viewModel.toggleListening() })
                1 -> InboxQueueTab(
                    queue = uiState.inboxQueue,
                    activeMsg = uiState.activeDraftMessage,
                    draftText = uiState.generatedDraftForQueue,
                    onSelectMsg = { msg -> viewModel.selectQueueMessage(msg) },
                    onDismiss = { msgId -> viewModel.dismissQueueMessage(msgId) },
                    onApproveSend = { msg, finalReply -> viewModel.approveAndSendReply(msg, finalReply) }
                )
                2 -> AuditLogTab(logs, onClearLogs = { viewModel.clearAuditLogs() })
                3 -> StyleLearningTab(
                    styleProfile = styleProfile ?: StyleProfileEntity(),
                    isAnalyzing = uiState.isAnalyzingStyle,
                    draftedReply = uiState.draftedReply,
                    onAnalyzeExport = { chatText -> viewModel.analyzeChatExport(chatText) },
                    onTestDraft = { incoming -> viewModel.testDraftReply(incoming) }
                )
                4 -> SettingsTab(
                    pin = editPin,
                    onPinChange = { editPin = it },
                    signature = editSignature,
                    onSignatureChange = { editSignature = it },
                    quietHours = editQuietHours,
                    onQuietHoursChange = { editQuietHours = it },
                    onSave = {
                        viewModel.updateSettings(editPin, true, editSignature, editQuietHours)
                    }
                )
            }

            // Security PIN Dialog Gate
            if (showPinDialog) {
                AlertDialog(
                    onDismissRequest = { showPinDialog = false },
                    title = { Text("Security Verification Required") },
                    text = {
                        Column {
                            Text("Enter your Jarvis Security PIN to authorize this command or send request:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = enteredPin,
                                onValueChange = { enteredPin = it },
                                label = { Text("PIN (Default: 1234)") },
                                singleLine = true,
                                modifier = Modifier.testTag("security_pin_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (enteredPin == uiState.securityPin) {
                                    showPinDialog = false
                                    enteredPin = ""
                                    viewModel.processUserPrompt(pendingActionText, onSpeak)
                                }
                            },
                            modifier = Modifier.testTag("pin_confirm_button")
                        ) {
                            Text("Authenticate")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showPinDialog = false
                            enteredPin = ""
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    uiState: JarvisUiState,
    onPromptSubmit: (String) -> Unit,
    onToggleListening: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (uiState.isKillSwitchActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().testTag("kill_switch_active_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MASTER KILL SWITCH ACTIVE. Jarvis is completely offline.", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(
                    containerColor = if (uiState.isListening) Color(0xFF4CAF50) else Color(0xFFFF9800)
                ) {
                    Text(
                        text = if (uiState.isListening) "Listening for 'Hey Jarvis'" else "Paused",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Switch(
                    checked = uiState.isListening,
                    onCheckedChange = { onToggleListening() },
                    modifier = Modifier.testTag("listening_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "Waveform")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Scale"
            )

            Canvas(modifier = Modifier.size(120.dp)) {
                drawCircle(
                    color = Color.Cyan.copy(alpha = 0.5f),
                    radius = (size.minDimension / 2) * if (uiState.isListening && !uiState.isKillSwitchActive) scale else 0.8f
                )
            }

            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Microphone Active",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("response_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Jarvis is thinking...", fontSize = 16.sp)
                } else {
                    Text(
                        text = uiState.currentResponse,
                        fontSize = 16.sp,
                        modifier = Modifier.testTag("ai_response_text")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask Jarvis anything...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("voice_prompt_input"),
                singleLine = true,
                enabled = !uiState.isKillSwitchActive
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onPromptSubmit(textInput)
                        textInput = ""
                    }
                },
                enabled = !uiState.isKillSwitchActive && textInput.isNotBlank(),
                modifier = Modifier.testTag("send_prompt_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun InboxQueueTab(
    queue: List<IncomingMessage>,
    activeMsg: IncomingMessage?,
    draftText: String,
    onSelectMsg: (IncomingMessage) -> Unit,
    onDismiss: (Long) -> Unit,
    onApproveSend: (IncomingMessage, String) -> Unit
) {
    var editableDraft by remember(draftText) { mutableStateOf(draftText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("WhatsApp & Instagram Inbox", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Intercepted messages awaiting your decision: 'Reply for you, or will you?'", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(12.dp))

        if (queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No pending messages in queue. You're all caught up!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("inbox_queue_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(queue) { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeMsg?.id == msg.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(msg.sender, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Badge { Text(msg.appName) }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("\"${msg.message}\"", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onSelectMsg(msg) },
                                    modifier = Modifier.weight(1f).testTag("reply_for_you_button")
                                ) {
                                    Text("Reply For You")
                                }
                                OutlinedButton(
                                    onClick = { onDismiss(msg.id) },
                                    modifier = Modifier.weight(1f).testTag("you_will_reply_button")
                                ) {
                                    Text("You Will")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Draft Review Modal / Card
        if (activeMsg != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().testTag("active_draft_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Review Draft for ${activeMsg.sender}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editableDraft,
                        onValueChange = { editableDraft = it },
                        modifier = Modifier.fillMaxWidth().testTag("editable_draft_input"),
                        label = { Text("Jarvis Style Draft (with disclosure)") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onDismiss(activeMsg.id) }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onApproveSend(activeMsg, editableDraft) },
                            modifier = Modifier.testTag("approve_send_button")
                        ) {
                            Text("Approve & Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StyleLearningTab(
    styleProfile: StyleProfileEntity,
    isAnalyzing: Boolean,
    draftedReply: String,
    onAnalyzeExport: (String) -> Unit,
    onTestDraft: (String) -> Unit
) {
    var chatExportInput by remember { mutableStateOf("") }
    var testIncomingInput by remember { mutableStateOf("Hey, are we still meeting today at 4pm?") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Style Learning Engine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Train Jarvis to text exactly like you by ingesting your WhatsApp chat export.", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = chatExportInput,
            onValueChange = { chatExportInput = it },
            label = { Text("Paste WhatsApp Chat Export (.txt)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("chat_export_input"),
            placeholder = { Text("[10/12/24, 2:30 PM] Ananta: Hey! Yeah sounds good 😂\n[10/12/24, 2:31 PM] Friend: You coming?") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val sampleExport = "[10/12/24, 2:30 PM] Ananta: yo! yeah im down 😂\n[10/12/24, 2:31 PM] Alex: u coming to office?\n[10/12/24, 2:32 PM] Ananta: yeah heading over now 🔥\n[10/12/24, 3:00 PM] Ananta: sounds good bro 👍"
                    chatExportInput = sampleExport
                },
                modifier = Modifier.weight(1f).testTag("load_sample_export_button")
            ) {
                Text("Load Sample Export")
            }

            Button(
                onClick = {
                    if (chatExportInput.isNotBlank()) {
                        onAnalyzeExport(chatExportInput)
                    }
                },
                enabled = !isAnalyzing && chatExportInput.isNotBlank(),
                modifier = Modifier.weight(1f).testTag("analyze_style_button")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Analyze & Train")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("style_profile_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Extracted Style Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Tone: ${styleProfile.tone}")
                Text("Top Emojis: ${styleProfile.topEmojis}")
                Text("Message Length: ${styleProfile.avgMessageLength}")
                Text("Punctuation: ${styleProfile.punctuationHabits}")
                Text("Sign-offs: ${styleProfile.commonSignOffs}")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Summary: ${styleProfile.rawAnalysisSummary}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Divider()

        Text("Test Your Style Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = testIncomingInput,
            onValueChange = { testIncomingInput = it },
            label = { Text("Simulate Incoming Message") },
            modifier = Modifier.fillMaxWidth().testTag("test_incoming_input")
        )

        Button(
            onClick = {
                if (testIncomingInput.isNotBlank()) {
                    onTestDraft(testIncomingInput)
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("test_draft_button")
        ) {
            Text("Draft Reply in My Style")
        }

        if (draftedReply.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("drafted_reply_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Drafted Reply (with Jarvis disclosure):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(draftedReply, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
fun AuditLogTab(logs: List<AuditLogEntity>, onClearLogs: () -> Unit) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Action Audit Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = onClearLogs,
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Text("Clear Log")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No actions logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("audit_log_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.actionType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(dateFormatter.format(Date(log.timestamp)), style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Recipient: ${log.recipient}", style = MaterialTheme.typography.bodyMedium)
                            Text("Content: ${log.content}", style = MaterialTheme.typography.bodySmall)
                            Text("Status: ${log.status}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    pin: String,
    onPinChange: (String) -> Unit,
    signature: String,
    onSignatureChange: (String) -> Unit,
    quietHours: Boolean,
    onQuietHoursChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    var savedMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Jarvis Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("Security PIN (Voice/Action Auth)") },
            modifier = Modifier.fillMaxWidth().testTag("settings_pin_input")
        )

        OutlinedTextField(
            value = signature,
            onValueChange = onSignatureChange,
            label = { Text("Message Disclosure Signature") },
            modifier = Modifier.fillMaxWidth().testTag("settings_signature_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quiet Hours / DND Awareness")
            Switch(
                checked = quietHours,
                onCheckedChange = onQuietHoursChange,
                modifier = Modifier.testTag("quiet_hours_switch")
            )
        }

        Button(
            onClick = {
                onSave()
                savedMessage = true
            },
            modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
        ) {
            Text("Save Settings")
        }

        if (savedMessage) {
            Text("Settings saved successfully!", color = MaterialTheme.colorScheme.primary)
        }
    }
}
