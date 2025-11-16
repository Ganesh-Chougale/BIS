package com.example.bis.pkgs.main_activity_pkg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Handles all permission-related operations for MainActivity
 * Responsible for checking and requesting overlay permissions
 */
class Permission_MainActivity(private val activity: Activity) {
    
    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1001
    }
    
    /**
     * Check if the app has overlay permission
     */
    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(activity)
    }
    
    /**
     * Request overlay permission by opening system settings
     */
    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivity(intent)
    }
}
