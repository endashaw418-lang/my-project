package com.smartvoice.assistant.data.model

/**
 * Represents a parsed voice command with its type, parameters, and metadata.
 */
data class Command(
    val type: CommandType,
    val rawText: String,
    val language: Language,
    val parameters: Map<String, String> = emptyMap(),
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * All supported command types that the assistant can execute.
 */
enum class CommandType {
    // App control
    OPEN_APP,
    CLOSE_APP,

    // Communication
    CALL_CONTACT,
    SEND_MESSAGE,
    SEND_EMAIL,

    // System controls
    TOGGLE_WIFI,
    TOGGLE_BLUETOOTH,
    TOGGLE_FLASHLIGHT,
    SET_VOLUME,
    SET_BRIGHTNESS,

    // Media
    PLAY_MUSIC,
    PAUSE_MUSIC,
    NEXT_TRACK,
    PREVIOUS_TRACK,

    // Utilities
    SET_ALARM,
    SET_TIMER,
    SET_REMINDER,
    TAKE_PHOTO,
    TAKE_SCREENSHOT,

    // Navigation
    NAVIGATE_TO,
    SEARCH_WEB,

    // Device
    OPEN_SETTINGS,
    CHECK_BATTERY,
    READ_NOTIFICATIONS,

    // Unknown / not recognized
    UNKNOWN
}
