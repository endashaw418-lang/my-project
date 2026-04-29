package com.smartvoice.assistant.data.model

/**
 * Represents recognized hand gestures mapped to actions.
 */
enum class GestureAction(val description: String) {
    OPEN_PALM("Open palm — activate voice"),
    CLOSED_FIST("Closed fist — stop/cancel"),
    THUMBS_UP("Thumbs up — confirm"),
    THUMBS_DOWN("Thumbs down — reject"),
    POINTING_UP("Point up — scroll up"),
    VICTORY("Victory/peace — take screenshot"),
    SWIPE_LEFT("Swipe left — go back"),
    SWIPE_RIGHT("Swipe right — go forward"),
    NONE("No gesture detected")
}
