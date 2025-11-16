package com.example.bis.pkgs.main_activity_pkg

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.widget.Toast
import com.example.bis.config.MagnifierShape
import com.example.bis.service.OverlayService

/**
 * Handles all service-related operations for MainActivity
 * Responsible for managing the OverlayService lifecycle and communication
 */
class Service_MainActivity(private val activity: Activity) {
    
    private val mediaProjectionManager = activity.getSystemService(Activity.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var isServiceRunning = false
    
    companion object {
        private const val TAG = "Service_MainActivity"
        private const val REQUEST_MEDIA_PROJECTION = 1001
    }
    
    /**
     * Get the current service running state
     */
    fun isRunning(): Boolean = isServiceRunning
    
    /**
     * Set the service running state
     */
    fun setRunning(running: Boolean) {
        isServiceRunning = running
    }
    
    /**
     * Start the screen capture process
     * This requests screen capture permission from the user
     */
    fun startScreenCapture() {
        activity.startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            REQUEST_MEDIA_PROJECTION
        )
    }
    
    /**
     * Stop the overlay service
     */
    fun stopService() {
        if (isServiceRunning) {
            Log.d(TAG, "Stopping OverlayService...")
            val stopIntent = Intent(activity, OverlayService::class.java).apply {
                action = "STOP_SERVICE"
            }
            activity.startService(stopIntent)
            isServiceRunning = false
        }
    }
    
    /**
     * Start the magnifier service with the given configuration
     */
    fun startMagnifier(
        selectedShape: MagnifierShape,
        selectedSize: Int,
        selectedOutputSize: Int,
        isInputDraggable: Boolean,
        isOutputDraggable: Boolean,
        isWidgetDraggable: Boolean,
        showCrosshair: Boolean,
        crosshairColor: Int,
        colorFilterMode: String,
        showZoomSlider: Boolean,
        minZoom: Float,
        maxZoom: Float,
        selectedShader: String,
        captureData: Intent?
    ) {
        if (captureData == null) {
            Toast.makeText(activity, "Capture data is null", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Starting magnifier service...")
        Log.d(TAG, "Sending config - isWidgetDraggable: $isWidgetDraggable")
        
        // Store the capture data in service companion object
        OverlayService.setPendingCaptureData(captureData)
        
        // Send the capture command to the service with configuration
        val captureIntent = Intent(activity, OverlayService::class.java).apply {
            action = "START_CAPTURE"
            putExtra("RESULT_CODE", Activity.RESULT_OK)
            putExtra("SHAPE", selectedShape.name)
            putExtra("INPUT_SIZE", selectedSize)
            putExtra("OUTPUT_SIZE", selectedOutputSize)
            putExtra("INPUT_DRAGGABLE", isInputDraggable)
            putExtra("OUTPUT_DRAGGABLE", isOutputDraggable)
            putExtra("WIDGET_DRAGGABLE", isWidgetDraggable)
            putExtra("SHOW_CROSSHAIR", showCrosshair)
            putExtra("CROSSHAIR_COLOR", crosshairColor)
            putExtra("COLOR_FILTER_MODE", colorFilterMode)
            putExtra("SHOW_ZOOM_SLIDER", showZoomSlider)
            putExtra("MIN_ZOOM", minZoom)
            putExtra("MAX_ZOOM", maxZoom)
            putExtra("SHADER", selectedShader)
        }
        activity.startService(captureIntent)
        
        isServiceRunning = true
        Toast.makeText(activity, "Permission granted, initializing...", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Completely exit the application
     */
    fun exitAppCompletely() {
        try {
            Log.d(TAG, "Starting complete app exit process...")
            
            // 1. Stop the overlay service if running
            if (isServiceRunning) {
                Log.d(TAG, "Stopping OverlayService...")
                val stopIntent = Intent(activity, OverlayService::class.java).apply {
                    action = "STOP_SERVICE"
                }
                activity.startService(stopIntent)
                isServiceRunning = false
            }
            
            // 2. Stop all services explicitly
            Log.d(TAG, "Stopping all services...")
            activity.stopService(Intent(activity, OverlayService::class.java))
            
            // 3. Clear any pending capture data
            OverlayService.setPendingCaptureData(Intent())
            
            // 4. Finish all activities in the task
            Log.d(TAG, "Finishing all activities...")
            activity.finishAffinity()
            
            // 5. Force garbage collection to clean up resources
            System.gc()
            
            // 6. Exit the process completely
            Log.d(TAG, "Terminating process...")
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during app exit", e)
            // Fallback: force exit even if cleanup fails
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }
}
