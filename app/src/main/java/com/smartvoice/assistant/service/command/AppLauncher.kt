package com.smartvoice.assistant.service.command

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Handles launching and closing applications by name.
 * Resolves user-friendly app names to package names using the PackageManager.
 */
class AppLauncher(private val context: Context) {

    // Common app name aliases mapped to their package names
    private val appAliases = mapOf(
        "whatsapp" to "com.whatsapp",
        "facebook" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "telegram" to "org.telegram.messenger",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "camera" to "com.android.camera2",
        "calculator" to "com.google.android.calculator",
        "calendar" to "com.google.android.calendar",
        "clock" to "com.google.android.deskclock",
        "contacts" to "com.google.android.contacts",
        "files" to "com.google.android.documentsui",
        "gallery" to "com.google.android.apps.photos",
        "photos" to "com.google.android.apps.photos",
        "play store" to "com.android.vending",
        "settings" to "com.android.settings",
        "spotify" to "com.spotify.music",
        "tiktok" to "com.zhiliaoapp.musically",
        "snapchat" to "com.snapchat.android",
        "messenger" to "com.facebook.orca",
        "zoom" to "us.zoom.videomeetings",
        "teams" to "com.microsoft.teams",
        "slack" to "com.Slack",
        "netflix" to "com.netflix.mediaclient",
        "uber" to "com.ubercab",
        "music" to "com.google.android.music"
    )

    /**
     * Launch an application by its user-friendly name.
     *
     * @return true if the app was found and launched successfully
     */
    fun launchApp(appName: String): Boolean {
        val packageName = resolvePackageName(appName) ?: return false

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }

    /**
     * Resolve a user-friendly app name to its package name.
     * First checks aliases, then searches installed apps by label.
     */
    fun resolvePackageName(appName: String): String? {
        val normalized = appName.trim().lowercase()

        // Check known aliases first
        appAliases[normalized]?.let { return it }

        // Search installed apps by label
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in installedApps) {
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (label.contains(normalized) || normalized.contains(label)) {
                return appInfo.packageName
            }
        }

        return null
    }

    /**
     * Check if an app is installed on the device.
     */
    fun isAppInstalled(appName: String): Boolean {
        val packageName = resolvePackageName(appName) ?: return false
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get a list of all installed apps with their labels.
     */
    fun getInstalledApps(): List<Pair<String, String>> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { appInfo ->
                pm.getApplicationLabel(appInfo).toString() to appInfo.packageName
            }
            .sortedBy { it.first }
    }
}
