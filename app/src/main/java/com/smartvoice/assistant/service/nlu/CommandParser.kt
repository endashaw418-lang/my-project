package com.smartvoice.assistant.service.nlu

import com.smartvoice.assistant.data.model.Command
import com.smartvoice.assistant.data.model.CommandType
import com.smartvoice.assistant.data.model.Language

/**
 * Parses natural language text into structured Command objects.
 *
 * Uses pattern matching with flexible keyword detection to handle
 * natural variations in phrasing. Supports English, Amharic, and Afaan Oromo.
 */
class CommandParser {

    /**
     * Parse the raw text into a structured Command.
     */
    fun parse(text: String, language: Language, confidence: Float = 1.0f): Command {
        val normalizedText = text.trim().lowercase()

        val (type, params) = when (language) {
            Language.ENGLISH -> parseEnglish(normalizedText)
            Language.AMHARIC -> parseAmharic(normalizedText)
            Language.AFAAN_OROMO -> parseOromo(normalizedText)
        }

        return Command(
            type = type,
            rawText = text,
            language = language,
            parameters = params,
            confidence = confidence
        )
    }

    // ─── English Parsing ──────────────────────────────────────────

    private fun parseEnglish(text: String): Pair<CommandType, Map<String, String>> {
        return when {
            // App control
            text.matchesAny("open", "launch", "start", "run") -> {
                val appName = text.extractAfter("open", "launch", "start", "run")
                CommandType.OPEN_APP to mapOf("app_name" to appName)
            }
            text.matchesAny("close", "exit", "quit", "stop") && !text.contains("music") -> {
                val appName = text.extractAfter("close", "exit", "quit", "stop")
                CommandType.CLOSE_APP to mapOf("app_name" to appName)
            }

            // Communication
            text.matchesAny("call", "phone", "dial", "ring") -> {
                val contactName = text.extractAfter("call", "phone", "dial", "ring")
                CommandType.CALL_CONTACT to mapOf("contact_name" to contactName)
            }
            text.matchesAny("send message", "text", "send sms", "message to", "send a message") -> {
                val parts = extractMessageParts(text)
                CommandType.SEND_MESSAGE to parts
            }
            text.matchesAny("send email", "email to", "send an email") -> {
                val parts = extractEmailParts(text)
                CommandType.SEND_EMAIL to parts
            }

            // System controls
            text.matchesAny("turn on wifi", "enable wifi", "wifi on", "connect wifi", "activate wifi") -> {
                CommandType.TOGGLE_WIFI to mapOf("state" to "on")
            }
            text.matchesAny("turn off wifi", "disable wifi", "wifi off", "disconnect wifi") -> {
                CommandType.TOGGLE_WIFI to mapOf("state" to "off")
            }
            text.matchesAny("turn on bluetooth", "enable bluetooth", "bluetooth on") -> {
                CommandType.TOGGLE_BLUETOOTH to mapOf("state" to "on")
            }
            text.matchesAny("turn off bluetooth", "disable bluetooth", "bluetooth off") -> {
                CommandType.TOGGLE_BLUETOOTH to mapOf("state" to "off")
            }
            text.matchesAny("turn on flashlight", "flashlight on", "torch on", "turn on torch") -> {
                CommandType.TOGGLE_FLASHLIGHT to mapOf("state" to "on")
            }
            text.matchesAny("turn off flashlight", "flashlight off", "torch off", "turn off torch") -> {
                CommandType.TOGGLE_FLASHLIGHT to mapOf("state" to "off")
            }

            // Volume
            text.matchesAny("volume up", "increase volume", "louder", "raise volume") -> {
                CommandType.SET_VOLUME to mapOf("direction" to "up")
            }
            text.matchesAny("volume down", "decrease volume", "quieter", "lower volume") -> {
                CommandType.SET_VOLUME to mapOf("direction" to "down")
            }
            text.matchesAny("mute", "silence", "volume off") -> {
                CommandType.SET_VOLUME to mapOf("direction" to "mute")
            }

            // Media
            text.matchesAny("play music", "play song", "play audio", "start music", "resume music") -> {
                CommandType.PLAY_MUSIC to emptyMap()
            }
            text.matchesAny("pause music", "stop music", "pause song", "stop playing") -> {
                CommandType.PAUSE_MUSIC to emptyMap()
            }
            text.matchesAny("next song", "next track", "skip song", "skip track") -> {
                CommandType.NEXT_TRACK to emptyMap()
            }
            text.matchesAny("previous song", "previous track", "go back", "last song") -> {
                CommandType.PREVIOUS_TRACK to emptyMap()
            }

            // Utilities
            text.matchesAny("set alarm", "create alarm", "alarm for", "wake me", "alarm at") -> {
                val time = extractTime(text)
                CommandType.SET_ALARM to mapOf("time" to time)
            }
            text.matchesAny("set timer", "timer for", "start timer", "countdown") -> {
                val duration = extractDuration(text)
                CommandType.SET_TIMER to mapOf("duration" to duration)
            }
            text.matchesAny("remind me", "set reminder", "reminder for", "don't forget") -> {
                val reminder = text.extractAfter("remind me", "set reminder", "reminder for")
                CommandType.SET_REMINDER to mapOf("reminder" to reminder)
            }
            text.matchesAny("take photo", "take picture", "take a photo", "capture", "take selfie") -> {
                CommandType.TAKE_PHOTO to emptyMap()
            }
            text.matchesAny("screenshot", "take screenshot", "screen capture") -> {
                CommandType.TAKE_SCREENSHOT to emptyMap()
            }

            // Navigation & Search
            text.matchesAny("navigate to", "directions to", "take me to", "how to get to") -> {
                val destination = text.extractAfter("navigate to", "directions to", "take me to", "how to get to")
                CommandType.NAVIGATE_TO to mapOf("destination" to destination)
            }
            text.matchesAny("search for", "search", "google", "look up", "find") -> {
                val query = text.extractAfter("search for", "search", "google", "look up", "find")
                CommandType.SEARCH_WEB to mapOf("query" to query)
            }

            // Device
            text.matchesAny("open settings", "go to settings", "device settings") -> {
                CommandType.OPEN_SETTINGS to emptyMap()
            }
            text.matchesAny("battery", "battery level", "how much battery", "check battery") -> {
                CommandType.CHECK_BATTERY to emptyMap()
            }
            text.matchesAny("read notification", "show notification", "what are my notifications") -> {
                CommandType.READ_NOTIFICATIONS to emptyMap()
            }

            // Brightness
            text.matchesAny("brightness up", "increase brightness", "brighter") -> {
                CommandType.SET_BRIGHTNESS to mapOf("direction" to "up")
            }
            text.matchesAny("brightness down", "decrease brightness", "dimmer", "dim") -> {
                CommandType.SET_BRIGHTNESS to mapOf("direction" to "down")
            }

            else -> CommandType.UNKNOWN to mapOf("raw" to text)
        }
    }

    // ─── Amharic Parsing ──────────────────────────────────────────

    private fun parseAmharic(text: String): Pair<CommandType, Map<String, String>> {
        return when {
            // Open app: ክፈት (kfet)
            text.containsAny("ክፈት", "ክፈተ", "አስጀምር") -> {
                val appName = text.removePatterns("ክፈት", "ክፈተ", "አስጀምር").trim()
                CommandType.OPEN_APP to mapOf("app_name" to appName)
            }
            // Close: ዝጋ (zga)
            text.containsAny("ዝጋ", "ዝጋው", "አቁም") -> {
                val appName = text.removePatterns("ዝጋ", "ዝጋው", "አቁም").trim()
                CommandType.CLOSE_APP to mapOf("app_name" to appName)
            }
            // Call: ደውል (dewl), ጥራ (t'ra)
            text.containsAny("ደውል", "ደውለ", "ጥራ", "ስልክ") -> {
                val contact = text.removePatterns("ደውል", "ደውለ", "ጥራ", "ስልክ", "ወደ", "ለ").trim()
                CommandType.CALL_CONTACT to mapOf("contact_name" to contact)
            }
            // Send message: መልእክት ላክ
            text.containsAny("መልእክት", "ላክ", "ጻፍ", "ኤስኤምኤስ") -> {
                val contact = text.removePatterns("መልእክት", "ላክ", "ጻፍ", "ወደ", "ለ").trim()
                CommandType.SEND_MESSAGE to mapOf("contact_name" to contact)
            }
            // WiFi
            text.containsAny("ዋይፋይ") && text.containsAny("አብራ", "ክፈት", "አብር") -> {
                CommandType.TOGGLE_WIFI to mapOf("state" to "on")
            }
            text.containsAny("ዋይፋይ") && text.containsAny("አጥፋ", "ዝጋ") -> {
                CommandType.TOGGLE_WIFI to mapOf("state" to "off")
            }
            // Play music: ሙዚቃ
            text.containsAny("ሙዚቃ", "ዘፈን") && text.containsAny("አጫውት", "ክፈት", "ጀምር") -> {
                CommandType.PLAY_MUSIC to emptyMap()
            }
            // Set alarm: ማንቂያ
            text.containsAny("ማንቂያ", "ቀስቃሽ", "አንቃኝ") -> {
                CommandType.SET_ALARM to mapOf("time" to text)
            }
            // Flashlight
            text.containsAny("ባትሪ", "ብርሃን", "ቶርች") && text.containsAny("አብራ", "ክፈት") -> {
                CommandType.TOGGLE_FLASHLIGHT to mapOf("state" to "on")
            }
            text.containsAny("ባትሪ", "ብርሃን", "ቶርች") && text.containsAny("አጥፋ", "ዝጋ") -> {
                CommandType.TOGGLE_FLASHLIGHT to mapOf("state" to "off")
            }
            // Volume
            text.containsAny("ድምጽ") && text.containsAny("ጨምር", "ከፍ") -> {
                CommandType.SET_VOLUME to mapOf("direction" to "up")
            }
            text.containsAny("ድምጽ") && text.containsAny("ቀንስ", "ዝቅ") -> {
                CommandType.SET_VOLUME to mapOf("direction" to "down")
            }
            // Search
            text.containsAny("ፈልግ", "አፈላልግ") -> {
                val query = text.removePatterns("ፈልግ", "አፈላልግ").trim()
                CommandType.SEARCH_WEB to mapOf("query" to query)
            }
            // Battery
            text.containsAny("ባትሪ", "ኃይል") && text.containsAny("ስንት", "ምን ያህል") -> {
                CommandType.CHECK_BATTERY to emptyMap()
            }
            // Settings
            text.containsAny("ቅንብር", "ሴቲንግ") -> {
                CommandType.OPEN_SETTINGS to emptyMap()
            }
            else -> CommandType.UNKNOWN to mapOf("raw" to text)
        }
    }

    // ─── Afaan Oromo Parsing ──────────────────────────────────────

    private fun parseOromo(text: String): Pair<CommandType, Map<String, String>> {
        return when {
            // Open app: bani/banaa
            text.containsAny("bani", "banaa", "jalqabi") -> {
                val appName = text.removePatterns("bani", "banaa", "jalqabi").trim()
                CommandType.OPEN_APP to mapOf("app_name" to appName)
            }
            // Close: cufi/cufni
            text.containsAny("cufi", "cufni", "dhaabi") -> {
                val appName = text.removePatterns("cufi", "cufni", "dhaabi").trim()
                CommandType.CLOSE_APP to mapOf("app_name" to appName)
            }
            // Call: bilbili
            text.containsAny("bilbili", "bilbila") -> {
                val contact = text.removePatterns("bilbili", "bilbila", "gara").trim()
                CommandType.CALL_CONTACT to mapOf("contact_name" to contact)
            }
            // Send message: ergaa/dhaamsa
            text.containsAny("ergaa", "dhaamsa", "ergi") -> {
                val contact = text.removePatterns("ergaa", "dhaamsa", "ergi", "gara").trim()
                CommandType.SEND_MESSAGE to mapOf("contact_name" to contact)
            }
            // WiFi
            text.containsAny("waayifaayii", "wifi") && text.containsAny("bani", "banaa", "qabsiisi") -> {
                CommandType.TOGGLE_WIFI to mapOf("state" to "on")
            }
            text.containsAny("waayifaayii", "wifi") && text.containsAny("cufi", "dhaabi") -> {
                CommandType.TOGGLE_WIFI to mapOf("state" to "off")
            }
            // Play music: muuziqaa
            text.containsAny("muuziqaa", "faaruu", "weedduu") -> {
                CommandType.PLAY_MUSIC to emptyMap()
            }
            // Set alarm: sa'aatii/alarm
            text.containsAny("sa'aatii", "alarmi", "dammaqsi") -> {
                CommandType.SET_ALARM to mapOf("time" to text)
            }
            // Flashlight
            text.containsAny("toochii", "ibsaa") && text.containsAny("bani", "qabsiisi") -> {
                CommandType.TOGGLE_FLASHLIGHT to mapOf("state" to "on")
            }
            text.containsAny("toochii", "ibsaa") && text.containsAny("cufi", "dhaabi") -> {
                CommandType.TOGGLE_FLASHLIGHT to mapOf("state" to "off")
            }
            // Volume
            text.containsAny("sagalee") && text.containsAny("dabal", "ol") -> {
                CommandType.SET_VOLUME to mapOf("direction" to "up")
            }
            text.containsAny("sagalee") && text.containsAny("hir'isi", "gad") -> {
                CommandType.SET_VOLUME to mapOf("direction" to "down")
            }
            // Search
            text.containsAny("barbaadi", "argadhu") -> {
                val query = text.removePatterns("barbaadi", "argadhu").trim()
                CommandType.SEARCH_WEB to mapOf("query" to query)
            }
            // Battery
            text.containsAny("baatirii", "humna") -> {
                CommandType.CHECK_BATTERY to emptyMap()
            }
            // Settings
            text.containsAny("qindaa'ina", "settings") -> {
                CommandType.OPEN_SETTINGS to emptyMap()
            }
            else -> CommandType.UNKNOWN to mapOf("raw" to text)
        }
    }

    // ─── Helper Extensions ────────────────────────────────────────

    private fun String.matchesAny(vararg patterns: String): Boolean {
        return patterns.any { this.contains(it) }
    }

    private fun String.containsAny(vararg words: String): Boolean {
        return words.any { this.contains(it, ignoreCase = true) }
    }

    private fun String.extractAfter(vararg triggers: String): String {
        for (trigger in triggers) {
            val idx = this.indexOf(trigger, ignoreCase = true)
            if (idx >= 0) {
                return this.substring(idx + trigger.length).trim()
            }
        }
        return this
    }

    private fun String.removePatterns(vararg patterns: String): String {
        var result = this
        for (pattern in patterns) {
            result = result.replace(pattern, "", ignoreCase = true)
        }
        return result
    }

    private fun extractMessageParts(text: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        // Pattern: "send message to [name] saying [message]"
        val toPattern = Regex("(?:to|for)\\s+(.+?)(?:\\s+(?:saying|that|message)\\s+(.+))?$", RegexOption.IGNORE_CASE)
        val match = toPattern.find(text)
        if (match != null) {
            params["contact_name"] = match.groupValues[1].trim()
            if (match.groupValues.size > 2 && match.groupValues[2].isNotBlank()) {
                params["message"] = match.groupValues[2].trim()
            }
        }
        return params
    }

    private fun extractEmailParts(text: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val toPattern = Regex("(?:to|for)\\s+(.+?)(?:\\s+(?:saying|about|subject)\\s+(.+))?$", RegexOption.IGNORE_CASE)
        val match = toPattern.find(text)
        if (match != null) {
            params["contact_name"] = match.groupValues[1].trim()
            if (match.groupValues.size > 2 && match.groupValues[2].isNotBlank()) {
                params["subject"] = match.groupValues[2].trim()
            }
        }
        return params
    }

    private fun extractTime(text: String): String {
        // Try to find time patterns like "7:30", "7 am", "7 o'clock"
        val timePattern = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*([ap]m|o'clock)?", RegexOption.IGNORE_CASE)
        val match = timePattern.find(text)
        return match?.value ?: text
    }

    private fun extractDuration(text: String): String {
        // Try to find duration patterns like "5 minutes", "1 hour"
        val durationPattern = Regex("(\\d+)\\s*(minute|min|hour|hr|second|sec)s?", RegexOption.IGNORE_CASE)
        val match = durationPattern.find(text)
        return match?.value ?: text
    }
}
