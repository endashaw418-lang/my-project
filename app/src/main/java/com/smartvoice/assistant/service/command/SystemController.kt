package com.smartvoice.assistant.service.command

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import java.util.Calendar

/**
 * Controls system-level device functions: WiFi, Bluetooth,
 * flashlight, volume, brightness, alarms, and more.
 */
class SystemController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // ─── WiFi ─────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    fun toggleWifi(enable: Boolean): Pair<Boolean, String> {
        return try {
            // On Android 10+, apps cannot toggle WiFi directly.
            // Open WiFi settings instead.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, if (enable) "Opening WiFi settings to turn on" else "Opening WiFi settings to turn off")
            } else {
                wifiManager.isWifiEnabled = enable
                Pair(true, if (enable) "WiFi turned on" else "WiFi turned off")
            }
        } catch (e: Exception) {
            Pair(false, "Failed to toggle WiFi: ${e.message}")
        }
    }

    // ─── Bluetooth ────────────────────────────────────────────────

    fun toggleBluetooth(enable: Boolean): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, if (enable) "Opening Bluetooth settings" else "Opening Bluetooth settings")
        } catch (e: Exception) {
            Pair(false, "Failed to open Bluetooth settings: ${e.message}")
        }
    }

    // ─── Flashlight ───────────────────────────────────────────────

    fun toggleFlashlight(enable: Boolean): Pair<Boolean, String> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return Pair(false, "No camera available for flashlight")
            cameraManager.setTorchMode(cameraId, enable)
            Pair(true, if (enable) "Flashlight turned on" else "Flashlight turned off")
        } catch (e: Exception) {
            Pair(false, "Failed to toggle flashlight: ${e.message}")
        }
    }

    // ─── Volume ───────────────────────────────────────────────────

    fun adjustVolume(direction: String): Pair<Boolean, String> {
        return try {
            when (direction.lowercase()) {
                "up" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Pair(true, "Volume increased")
                }
                "down" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Pair(true, "Volume decreased")
                }
                "mute" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Pair(true, "Volume muted")
                }
                else -> Pair(false, "Unknown volume direction: $direction")
            }
        } catch (e: Exception) {
            Pair(false, "Failed to adjust volume: ${e.message}")
        }
    }

    // ─── Alarm ────────────────────────────────────────────────────

    fun setAlarm(timeString: String): Pair<Boolean, String> {
        return try {
            val (hour, minute) = parseTime(timeString)
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Setting alarm for $hour:${minute.toString().padStart(2, '0')}")
        } catch (e: Exception) {
            Pair(false, "Failed to set alarm: ${e.message}")
        }
    }

    fun setTimer(durationString: String): Pair<Boolean, String> {
        return try {
            val seconds = parseDuration(durationString)
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Setting timer for $durationString")
        } catch (e: Exception) {
            Pair(false, "Failed to set timer: ${e.message}")
        }
    }

    // ─── Media ────────────────────────────────────────────────────

    fun playMusic(): Pair<Boolean, String> {
        return try {
            // Send media play key event
            audioManager.dispatchMediaKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
            )
            audioManager.dispatchMediaKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
            )
            Pair(true, "Playing music")
        } catch (e: Exception) {
            // Fallback: try to open a music app
            try {
                val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "Opening music player")
            } catch (e2: Exception) {
                Pair(false, "No music player found")
            }
        }
    }

    fun pauseMusic(): Pair<Boolean, String> {
        return try {
            audioManager.dispatchMediaKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            )
            audioManager.dispatchMediaKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            )
            Pair(true, "Music paused")
        } catch (e: Exception) {
            Pair(false, "Failed to pause music")
        }
    }

    fun nextTrack(): Pair<Boolean, String> {
        return try {
            audioManager.dispatchMediaKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            )
            audioManager.dispatchMediaKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            )
            Pair(true, "Playing next track")
        } catch (e: Exception) {
            Pair(false, "Failed to skip track")
        }
    }

    fun previousTrack(): Pair<Boolean, String> {
        return try {
            audioManager.dispatchMediaKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            )
            audioManager.dispatchMediaKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            )
            Pair(true, "Playing previous track")
        } catch (e: Exception) {
            Pair(false, "Failed to go to previous track")
        }
    }

    // ─── Camera ───────────────────────────────────────────────────

    fun takePhoto(): Pair<Boolean, String> {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening camera")
        } catch (e: Exception) {
            Pair(false, "Failed to open camera")
        }
    }

    // ─── Navigation & Search ──────────────────────────────────────

    fun navigateTo(destination: String): Pair<Boolean, String> {
        return try {
            val uri = android.net.Uri.parse("google.navigation:q=${android.net.Uri.encode(destination)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Navigating to $destination")
        } catch (e: Exception) {
            Pair(false, "Google Maps not available")
        }
    }

    fun searchWeb(query: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Searching for: $query")
        } catch (e: Exception) {
            Pair(false, "Failed to search the web")
        }
    }

    // ─── Settings ─────────────────────────────────────────────────

    fun openSettings(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening settings")
        } catch (e: Exception) {
            Pair(false, "Failed to open settings")
        }
    }

    // ─── Battery ──────────────────────────────────────────────────

    fun checkBattery(): Pair<Boolean, String> {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return Pair(true, "Battery level is $level%")
    }

    // ─── Brightness ───────────────────────────────────────────────

    fun adjustBrightness(direction: String): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening display settings to adjust brightness")
        } catch (e: Exception) {
            Pair(false, "Failed to open display settings")
        }
    }

    // ─── Utility Parsers ──────────────────────────────────────────

    private fun parseTime(text: String): Pair<Int, Int> {
        val timePattern = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*([ap]m)?", RegexOption.IGNORE_CASE)
        val match = timePattern.find(text)

        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: 8
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val ampm = match.groupValues[3].lowercase()

            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            return Pair(hour, minute)
        }

        // Default to next hour
        val cal = Calendar.getInstance()
        return Pair(cal.get(Calendar.HOUR_OF_DAY) + 1, 0)
    }

    private fun parseDuration(text: String): Int {
        val pattern = Regex("(\\d+)\\s*(minute|min|hour|hr|second|sec)s?", RegexOption.IGNORE_CASE)
        val match = pattern.find(text)

        if (match != null) {
            val value = match.groupValues[1].toIntOrNull() ?: 0
            val unit = match.groupValues[2].lowercase()
            return when {
                unit.startsWith("hour") || unit.startsWith("hr") -> value * 3600
                unit.startsWith("min") -> value * 60
                else -> value
            }
        }
        return 60 // Default: 1 minute
    }
}
