package com.example.bis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast

class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService.Companion"
        
        @Volatile
        private var pendingCaptureData: Intent? = null
        
        fun setPendingCaptureData(data: Intent) {
            Log.d(TAG, "setPendingCaptureData called with: $data")
            pendingCaptureData = data
            Log.d(TAG, "pendingCaptureData stored: $pendingCaptureData")
        }
    }

    private val TAG = "OverlayService"
    
    // Configuration
    private lateinit var config: MagnifierConfig
    
    // Managers and overlays
    private lateinit var windowManager: WindowManager
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private lateinit var inputSelectorOverlay: InputSelectorOverlay
    private lateinit var outputWindowOverlay: OutputWindowOverlay
    private lateinit var toggleWidgetOverlay: ToggleWidgetOverlay
    private lateinit var verticalZoomSlider: VerticalZoomSlider

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== OverlayService onCreate called ===")
        
        // Initialize configuration
        config = MagnifierConfig()
        
        // Get window manager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Get screen dimensions
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        config.screenWidth = metrics.widthPixels
        config.screenHeight = metrics.heightPixels
        config.screenDensity = metrics.densityDpi
        
        Log.d(TAG, "Screen: ${config.screenWidth}x${config.screenHeight}")
        
        // Initialize screen capture manager
        screenCaptureManager = ScreenCaptureManager(
            context = this,
            config = config,
            onFrameAvailable = { bitmap ->
                outputWindowOverlay.updateMagnifiedView(bitmap)
            }
        )
        
        // Start foreground service
        startForegroundService()
    }
    
    private fun createOverlays() {
        Log.d(TAG, "createOverlays called")
        
        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "ERROR: No overlay permission!", Toast.LENGTH_LONG).show()
            Log.e(TAG, "No overlay permission!")
            return
        }
        
        try {
            // Initialize overlays
            inputSelectorOverlay = InputSelectorOverlay(this, config, windowManager)
            outputWindowOverlay = OutputWindowOverlay(
                context = this,
                config = config,
                windowManager = windowManager,
                onPositionChanged = { onOutputPositionChanged() }
            )
            toggleWidgetOverlay = ToggleWidgetOverlay(
                context = this,
                config = config,
                windowManager = windowManager,
                onToggleClick = { toggleMagnification() }
            )
            verticalZoomSlider = VerticalZoomSlider(
                context = this,
                config = config,
                windowManager = windowManager,
                onZoomChanged = { onZoomChanged() }
            )
            
            // Show all overlays
            inputSelectorOverlay.show()
            outputWindowOverlay.show()
            toggleWidgetOverlay.show()
            verticalZoomSlider.show()
            
            Log.d(TAG, "All overlays created successfully")
            Toast.makeText(this, "Magnifier ready!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating overlays", e)
            Toast.makeText(this, "ERROR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    
    private fun toggleMagnification() {
        config.isMagnifying = !config.isMagnifying
        
        if (config.isMagnifying) {
            inputSelectorOverlay.reveal()
            outputWindowOverlay.reveal()
            verticalZoomSlider.reveal()
        } else {
            inputSelectorOverlay.hide()
            outputWindowOverlay.hide()
            verticalZoomSlider.hide()
        }
        
        toggleWidgetOverlay.updateIcon(config.isMagnifying)
        Log.d(TAG, "Magnification toggled: ${config.isMagnifying}")
    }
    
    /**
     * Called when zoom level changes from the slider
     */
    private fun onZoomChanged() {
        Log.d(TAG, "Zoom changed to: ${config.zoomFactor}x")
        // Update output window size based on new zoom
        outputWindowOverlay.updateSize()
    }
    
    /**
     * Called when output window position changes (dragged)
     */
    private fun onOutputPositionChanged() {
        // Update slider position to follow output window
        if (::verticalZoomSlider.isInitialized) {
            verticalZoomSlider.updatePosition()
        }
    }
    
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "=== onStartCommand called ===")
        Log.d(TAG, "Intent: $intent")
        Log.d(TAG, "Action: ${intent?.action}")
        Log.d(TAG, "Flags: $flags, StartId: $startId")
        
        // Only process if we have a START_CAPTURE action
        if (intent?.action == "START_CAPTURE") {
            // Get data from companion object (since Intent can't be passed as extra)
            val resultCode = intent.getIntExtra("RESULT_CODE", 0)
            val data = pendingCaptureData
            
            Log.d(TAG, "Processing START_CAPTURE action")
            Log.d(TAG, "resultCode: $resultCode (Activity.RESULT_OK = -1)")
            Log.d(TAG, "data from companion: $data")
            
            if (data == null) {
                Log.e(TAG, "ERROR: data is null")
                Toast.makeText(this, "ERROR: MediaProjection data is null", Toast.LENGTH_LONG).show()
            } else {
                Log.d(TAG, "SUCCESS: Both resultCode and data are valid!")
                
                // Get shape and size configuration from intent
                val shapeName = intent.getStringExtra("SHAPE") ?: MagnifierShape.SQUARE.name
                val inputSize = intent.getIntExtra("INPUT_SIZE", 200)
                val maxZoom = intent.getFloatExtra("MAX_ZOOM", 10.0f)
                
                // Apply configuration
                config.shape = MagnifierShape.valueOf(shapeName)
                config.inputSize = inputSize
                config.maxZoom = maxZoom
                
                // Set initial positions BEFORE creating overlays
                if (config.inputX == 0 && config.inputY == 0) {
                    // Place input selector at screen center
                    config.inputX = config.screenWidth / 2 - config.inputSize / 2
                    config.inputY = config.screenHeight / 2 - config.inputSize / 2
                }
                
                if (config.outputPosition.x == 0 && config.outputPosition.y == 0) {
                    // Place output window at top-left corner
                    config.outputPosition.x = 20  // Small margin from edge
                    config.outputPosition.y = 20  // Small margin from edge
                }
                
                Log.d(TAG, "Configuration: shape=${config.shape}, size=${config.inputSize}, maxZoom=${config.maxZoom}")
                Log.d(TAG, "Initial positions: input=(${config.inputX},${config.inputY}), output=(${config.outputPosition.x},${config.outputPosition.y})")
                Toast.makeText(this, "Starting magnifier...", Toast.LENGTH_SHORT).show()
                
                // Create overlays if not already created
                if (!::inputSelectorOverlay.isInitialized) {
                    Log.d(TAG, "Creating overlays...")
                    createOverlays()
                    // Update slider position after overlays are created
                    verticalZoomSlider.updatePosition()
                } else {
                    Log.d(TAG, "Overlays already exist")
                }
                
                // Start screen capture
                screenCaptureManager.startCapture(resultCode, data)
            }
        } else {
            Log.d(TAG, "Service started without START_CAPTURE action, waiting for capture intent...")
        }
        
        return START_NOT_STICKY
    }
    
    private fun startForegroundService() {
        val channelId = "magnifier_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Magnifier Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }
            .setContentTitle("Screen Magnifier")
            .setContentText("Magnifier is active")
            .setSmallIcon(android.R.drawable.ic_menu_zoom)
            .build()
        
        startForeground(1, notification)
    }
    

    override fun onDestroy() {
        super.onDestroy()
        
        // Stop screen capture
        if (::screenCaptureManager.isInitialized) {
            screenCaptureManager.stopCapture()
        }
        
        // Remove all overlays
        if (::inputSelectorOverlay.isInitialized) {
            inputSelectorOverlay.remove()
        }
        if (::outputWindowOverlay.isInitialized) {
            outputWindowOverlay.remove()
        }
        if (::toggleWidgetOverlay.isInitialized) {
            toggleWidgetOverlay.remove()
        }
        if (::verticalZoomSlider.isInitialized) {
            verticalZoomSlider.remove()
        }
        
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
