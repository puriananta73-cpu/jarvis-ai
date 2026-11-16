package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.AuditLogEntity
import com.example.data.JarvisDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JarvisAccessibilityService : AccessibilityService() {
    private val TAG = "JarvisAccessService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set

        fun sendMessageInCurrentApp(text: String, onResult: (Boolean, String) -> Unit) {
            val svc = instance
            if (svc == null) {
                onResult(false, "Jarvis Accessibility Service is not enabled in Android Settings.")
                return
            }
            svc.performSend(text, onResult)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Jarvis Accessibility Service connected successfully.")
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // Monitor window content for automation readiness
    }

    override fun onInterrupt() {
        instance = null
        Log.d(TAG, "Jarvis Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    private fun performSend(text: String, onResult: (Boolean, String) -> Unit) {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            onResult(false, "Graceful Failure: Cannot access active window nodes. Open WhatsApp or Instagram chat first.")
            return
        }

        // Search for message input field (Common IDs: com.whatsapp:id/entry, editable text views)
        val inputNode = findEditableNode(rootNode)
        if (inputNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val setTextSuccess = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            
            if (setTextSuccess) {
                // Search for Send button (Common IDs: com.whatsapp:id/send, description "Send")
                val sendButton = findSendButton(rootNode)
                if (sendButton != null) {
                    val clicked = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) {
                        logAccessibilityAction("AUTO_SEND_SUCCESS", text)
                        onResult(true, "Message sent successfully with Jarvis signature.")
                        return
                    }
                }
            }
        }

        // Graceful failure check: if input or send button couldn't be located precisely
        logAccessibilityAction("AUTO_SEND_FAILED", "Could not locate chat input or send button securely.")
        onResult(false, "Graceful Failure: Could not locate chat input field. Please open the specific chat or send manually.")
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (desc.contains("send") || viewId.contains("send") || text == "send") {
            if (node.isClickable) return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findSendButton(child)
            if (found != null) return found
        }
        return null
    }

    private fun logAccessibilityAction(status: String, details: String) {
        serviceScope.launch {
            try {
                val db = JarvisDatabase.getDatabase(applicationContext)
                db.auditLogDao().insertLog(
                    AuditLogEntity(
                        actionType = "ACCESSIBILITY_AUTOMATION",
                        recipient = "Active App Chat",
                        content = details,
                        status = status
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log accessibility action", e)
            }
        }
    }
}
