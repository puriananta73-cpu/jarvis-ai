package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

object AppController {
    private const val TAG = "AppController"

    fun openAppByName(context: Context, appName: String): Boolean {
        val pm: PackageManager = context.packageManager
        val pkgName = when {
            appName.contains("whatsapp", ignoreCase = true) -> "com.whatsapp"
            appName.contains("instagram", ignoreCase = true) -> "com.instagram.android"
            appName.contains("spotify", ignoreCase = true) -> "com.spotify.music"
            appName.contains("youtube", ignoreCase = true) -> "com.google.android.youtube"
            appName.contains("settings", ignoreCase = true) -> android.provider.Settings.ACTION_SETTINGS
            else -> null
        }

        if (pkgName == android.provider.Settings.ACTION_SETTINGS) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open settings", e)
                return false
            }
        }

        if (pkgName != null) {
            try {
                val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch package $pkgName", e)
            }
        }
        return false
    }
}
