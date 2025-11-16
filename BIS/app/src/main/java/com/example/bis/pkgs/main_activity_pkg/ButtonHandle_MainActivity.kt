package com.example.bis.pkgs.main_activity_pkg

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import com.example.bis.AboutActivity

/**
 * Handles all button click events for MainActivity
 * Responsible for managing button interactions and navigation
 */
class ButtonHandle_MainActivity(private val activity: Activity) {
    
    /**
     * Handle toggle button click
     * Checks permission and either starts or stops the service
     */
    fun handleToggleButtonClick(
        permissionManager: Permission_MainActivity,
        serviceManager: Service_MainActivity
    ) {
        if (permissionManager.canDrawOverlays()) {
            if (serviceManager.isRunning()) {
                serviceManager.stopService()
            } else {
                serviceManager.startScreenCapture()
            }
        } else {
            permissionManager.requestOverlayPermission()
        }
    }
    
    /**
     * Handle about button click
     * Navigates to AboutActivity
     */
    fun handleAboutButtonClick() {
        val intent = Intent(activity, AboutActivity::class.java)
        activity.startActivity(intent)
    }
    
    /**
     * Handle exit button click
     * Exits the application completely
     */
    fun handleExitButtonClick(serviceManager: Service_MainActivity) {
        serviceManager.exitAppCompletely()
    }
}
