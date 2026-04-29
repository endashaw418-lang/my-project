package com.smartvoice.assistant.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Centralized manager for Android runtime permissions.
 *
 * Handles requesting and checking all permissions needed by the app:
 * - RECORD_AUDIO (voice recognition)
 * - CAMERA (gesture recognition)
 * - CALL_PHONE (making calls)
 * - READ_CONTACTS (contact lookup)
 * - SEND_SMS (sending messages)
 * - POST_NOTIFICATIONS (Android 13+)
 */
class PermissionManager(private val context: Context) {

    companion object {
        val REQUIRED_PERMISSIONS = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.SEND_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        val ESSENTIAL_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }

    /**
     * Check if all required permissions are granted.
     */
    fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if essential permissions (microphone) are granted.
     */
    fun hasEssentialPermissions(): Boolean {
        return ESSENTIAL_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if a specific permission is granted.
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request all required permissions from the user.
     */
    fun requestAllPermissions(activity: Activity) {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }
    }

    /**
     * Request only essential permissions.
     */
    fun requestEssentialPermissions(activity: Activity) {
        val missingPermissions = ESSENTIAL_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }
    }

    /**
     * Get list of permissions that have not been granted yet.
     */
    fun getMissingPermissions(): List<String> {
        return REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if the accessibility service is enabled.
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "${context.packageName}/.service.accessibility.VoiceAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(serviceName)
    }

    /**
     * Open the accessibility settings so the user can enable the service.
     */
    fun openAccessibilitySettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    /**
     * Get a human-readable description of a permission.
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> "Microphone access for voice recognition"
            Manifest.permission.CAMERA -> "Camera access for gesture recognition"
            Manifest.permission.CALL_PHONE -> "Phone permission for making calls"
            Manifest.permission.READ_CONTACTS -> "Contacts access for finding people"
            Manifest.permission.SEND_SMS -> "SMS permission for sending messages"
            Manifest.permission.POST_NOTIFICATIONS -> "Notification permission for alerts"
            else -> permission
        }
    }
}
