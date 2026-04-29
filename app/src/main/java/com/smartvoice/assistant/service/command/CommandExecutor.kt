package com.smartvoice.assistant.service.command

import android.content.Context
import com.smartvoice.assistant.data.model.Command
import com.smartvoice.assistant.data.model.CommandResult
import com.smartvoice.assistant.data.model.CommandType
import com.smartvoice.assistant.data.model.Language
import com.smartvoice.assistant.service.accessibility.VoiceAccessibilityService

/**
 * Central command executor that dispatches parsed commands
 * to the appropriate controller (app, phone, system).
 *
 * Produces localized response messages in the user's detected language.
 */
class CommandExecutor(context: Context) {

    private val appLauncher = AppLauncher(context)
    private val phoneController = PhoneController(context)
    private val systemController = SystemController(context)

    /**
     * Execute a parsed command and return the result.
     */
    fun execute(command: Command): CommandResult {
        val (success, message) = when (command.type) {
            // App control
            CommandType.OPEN_APP -> {
                val appName = command.parameters["app_name"] ?: ""
                if (appLauncher.launchApp(appName)) {
                    Pair(true, localizedMessage("Opening $appName", command.language))
                } else {
                    Pair(false, localizedMessage("App '$appName' not found", command.language))
                }
            }
            CommandType.CLOSE_APP -> {
                // Closing apps requires Accessibility Service
                Pair(true, localizedMessage("Use the recent apps button to close", command.language))
            }

            // Communication
            CommandType.CALL_CONTACT -> {
                val contactName = command.parameters["contact_name"] ?: ""
                val (ok, msg) = phoneController.callContact(contactName)
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.SEND_MESSAGE -> {
                val contactName = command.parameters["contact_name"] ?: ""
                val messageText = command.parameters["message"]
                val (ok, msg) = phoneController.sendMessage(contactName, messageText)
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.SEND_EMAIL -> {
                val recipient = command.parameters["contact_name"]
                val subject = command.parameters["subject"]
                val (ok, msg) = phoneController.sendEmail(recipient, subject)
                Pair(ok, localizedMessage(msg, command.language))
            }

            // System controls
            CommandType.TOGGLE_WIFI -> {
                val state = command.parameters["state"] == "on"
                val (ok, msg) = systemController.toggleWifi(state)
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.TOGGLE_BLUETOOTH -> {
                val state = command.parameters["state"] == "on"
                val (ok, msg) = systemController.toggleBluetooth(state)
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.TOGGLE_FLASHLIGHT -> {
                val state = command.parameters["state"] == "on"
                val (ok, msg) = systemController.toggleFlashlight(state)
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.SET_VOLUME -> {
                val direction = command.parameters["direction"] ?: "up"
                val (ok, msg) = systemController.adjustVolume(direction)
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.SET_BRIGHTNESS -> {
                val direction = command.parameters["direction"] ?: "up"
                val (ok, msg) = systemController.adjustBrightness(direction)
                Pair(ok, localizedMessage(msg, command.language))
            }

            // Media
            CommandType.PLAY_MUSIC -> {
                val (ok, msg) = systemController.playMusic()
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.PAUSE_MUSIC -> {
                val (ok, msg) = systemController.pauseMusic()
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.NEXT_TRACK -> {
                val (ok, msg) = systemController.nextTrack()
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.PREVIOUS_TRACK -> {
                val (ok, msg) = systemController.previousTrack()
                Pair(ok, localizedMessage(msg, command.language))
            }

            // Utilities
            CommandType.SET_ALARM -> {
                val time = command.parameters["time"] ?: ""
                val (ok, msg) = systemController.setAlarm(time)
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.SET_TIMER -> {
                val duration = command.parameters["duration"] ?: ""
                val (ok, msg) = systemController.setTimer(duration)
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.SET_REMINDER -> {
                val reminder = command.parameters["reminder"] ?: ""
                Pair(true, localizedMessage("Reminder set: $reminder", command.language))
            }
            CommandType.TAKE_PHOTO -> {
                val (ok, msg) = systemController.takePhoto()
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.TAKE_SCREENSHOT -> {
                if (VoiceAccessibilityService.isEnabled.value) {
                    val ok = VoiceAccessibilityService.takeScreenshot()
                    if (ok) {
                        Pair(true, localizedMessage("Screenshot taken", command.language))
                    } else {
                        Pair(false, localizedMessage("Failed to take screenshot", command.language))
                    }
                } else {
                    Pair(false, localizedMessage("Screenshot requires accessibility permission", command.language))
                }
            }

            // Navigation & Search
            CommandType.NAVIGATE_TO -> {
                val destination = command.parameters["destination"] ?: ""
                val (ok, msg) = systemController.navigateTo(destination)
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.SEARCH_WEB -> {
                val query = command.parameters["query"] ?: ""
                val (ok, msg) = systemController.searchWeb(query)
                Pair(ok, localizedMessage(msg, command.language))
            }

            // Device
            CommandType.OPEN_SETTINGS -> {
                val (ok, msg) = systemController.openSettings()
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.CHECK_BATTERY -> {
                val (ok, msg) = systemController.checkBattery()
                Pair(ok, localizedMessage(msg, command.language))
            }
            CommandType.READ_NOTIFICATIONS -> {
                if (VoiceAccessibilityService.isEnabled.value) {
                    VoiceAccessibilityService.openNotifications()
                    Pair(true, localizedMessage("Opening notifications", command.language))
                } else {
                    Pair(false, localizedMessage("Reading notifications requires accessibility permission", command.language))
                }
            }

            // Unknown
            CommandType.UNKNOWN -> {
                Pair(false, localizedMessage("I didn't understand that command", command.language))
            }
        }

        return CommandResult(
            success = success,
            message = message,
            command = command,
            responseLanguage = command.language
        )
    }

    /**
     * Provide basic localized response messages.
     * For a production app, use string resources or a translation API.
     */
    private fun localizedMessage(englishMessage: String, language: Language): String {
        if (language == Language.ENGLISH) return englishMessage

        // Common response translations
        val translations = when (language) {
            Language.AMHARIC -> mapOf(
                "I didn't understand that command" to "ያንን ትእዛዝ አልገባኝም",
                "Opening" to "በመክፈት ላይ",
                "not found" to "አልተገኘም",
                "WiFi turned on" to "ዋይፋይ በርቷል",
                "WiFi turned off" to "ዋይፋይ ጠፍቷል",
                "Volume increased" to "ድምጽ ጨምሯል",
                "Volume decreased" to "ድምጽ ቀንሷል",
                "Volume muted" to "ድምጽ ጠፍቷል",
                "Playing music" to "ሙዚቃ በማጫወት ላይ",
                "Music paused" to "ሙዚቃ ቆሟል",
                "Flashlight turned on" to "ባትሪ በርቷል",
                "Flashlight turned off" to "ባትሪ ጠፍቷል",
                "Battery level is" to "የባትሪ መጠን",
                "Opening settings" to "ቅንብሮችን በመክፈት ላይ",
                "Opening camera" to "ካሜራ በመክፈት ላይ",
                "Calling" to "በመደወል ላይ",
                "Message sent to" to "መልእክት ተልኳል ወደ",
                "Setting alarm for" to "ማንቂያ በማዘጋጀት ላይ ለ"
            )
            Language.AFAAN_OROMO -> mapOf(
                "I didn't understand that command" to "Ajaja kana hin hubanne",
                "Opening" to "Banaa jira",
                "not found" to "hin argamne",
                "WiFi turned on" to "WiFi baname",
                "WiFi turned off" to "WiFi cufame",
                "Volume increased" to "Sagaleen dabalame",
                "Volume decreased" to "Sagaleen hir'atame",
                "Volume muted" to "Sagaleen cufame",
                "Playing music" to "Muuziqaa taphataa jira",
                "Music paused" to "Muuziqaan dhaabbate",
                "Flashlight turned on" to "Toochiin baname",
                "Flashlight turned off" to "Toochiin cufame",
                "Battery level is" to "Sadarkaan baatirii",
                "Opening settings" to "Qindaa'ina banaa jira",
                "Opening camera" to "Kaameeraa banaa jira",
                "Calling" to "Bilbilaa jira",
                "Message sent to" to "Ergaan ergame gara",
                "Setting alarm for" to "Alarmii qopheessaa jira"
            )
            else -> emptyMap()
        }

        // Try to find a matching translation
        for ((key, value) in translations) {
            if (englishMessage.contains(key, ignoreCase = true)) {
                return englishMessage.replace(key, value, ignoreCase = true)
            }
        }

        return englishMessage
    }
}
