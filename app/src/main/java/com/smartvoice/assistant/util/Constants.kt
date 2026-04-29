package com.smartvoice.assistant.util

/**
 * Application-wide constants.
 */
object Constants {
    const val COMMAND_HISTORY_LIMIT = 100
    const val SPEECH_TIMEOUT_MS = 10_000L
    const val GESTURE_CONFIDENCE_THRESHOLD = 0.6f
    const val DEFAULT_SPEECH_RATE = 1.0f
    const val DEFAULT_PITCH = 1.0f

    // Preference keys
    const val PREF_LANGUAGE = "pref_language"
    const val PREF_AUTO_DETECT_LANGUAGE = "pref_auto_detect"
    const val PREF_GESTURE_ENABLED = "pref_gesture_enabled"
    const val PREF_OFFLINE_MODE = "pref_offline_mode"
    const val PREF_SPEECH_RATE = "pref_speech_rate"
    const val PREF_PITCH = "pref_pitch"
    const val PREF_CONTINUOUS_LISTENING = "pref_continuous_listening"

    // Request codes
    const val REQUEST_CODE_PERMISSIONS = 1001
    const val REQUEST_CODE_ACCESSIBILITY = 1002
}
