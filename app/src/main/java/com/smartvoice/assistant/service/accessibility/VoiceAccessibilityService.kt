package com.smartvoice.assistant.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Accessibility Service enabling advanced phone control capabilities.
 *
 * This service allows the app to:
 * - Read on-screen content (for notification reading)
 * - Perform global actions (back, home, recents, screenshot)
 * - Interact with other app UIs
 *
 * SETUP REQUIRED: The user must manually enable this service in
 * Settings → Accessibility → Smart Voice Assistant.
 */
class VoiceAccessibilityService : AccessibilityService() {

    companion object {
        private val _isEnabled = MutableStateFlow(false)
        val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

        private val _notificationText = MutableSharedFlow<String>(extraBufferCapacity = 10)
        val notificationText: SharedFlow<String> = _notificationText.asSharedFlow()

        private var instance: VoiceAccessibilityService? = null

        fun performGlobalAction(action: Int): Boolean {
            return instance?.performGlobalAction(action) ?: false
        }

        fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
        fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
        fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
        fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        fun takeScreenshot(): Boolean = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        fun lockScreen(): Boolean = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isEnabled.value = true

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    val text = it.text.joinToString(", ")
                    if (text.isNotBlank()) {
                        _notificationText.tryEmit(text)
                    }
                }
                else -> { /* Other events */ }
            }
        }
    }

    override fun onInterrupt() {
        // Called when the system interrupts the accessibility service
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        _isEnabled.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        _isEnabled.value = false
        super.onDestroy()
    }
}
