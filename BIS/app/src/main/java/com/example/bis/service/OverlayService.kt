package com.example.bis.service

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
import com.example.bis.config.MagnifierConfig
import com.example.bis.config.MagnifierShape
import com.example.bis.overlay.InputSelectorOverlay
import com.example.bis.overlay.OutputWindowOverlay
import com.example.bis.overlay.ToggleWidgetOverlay
import com.example.bis.overlay.ControlPanelOverlay
import com.example.bis.capture.ScreenCaptureManager
import com.example.bis.slider.Slider
import com.example.bis.slider.SliderFactory
import com.example.bis.slider.model.SliderConfig
import com.example.bis.slider.shape.SquareSlider

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
    private lateinit var zoomSlider: Slider

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
            val errorMsg = "ERROR: No overlay permission!"
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            Log.e(TAG, errorMsg)
            return
        }
        
        Log.d(TAG, "Overlay permission granted, creating overlays...")
        
        try {
            // Initialize input selector overlay
            Log.d(TAG, "Creating InputSelectorOverlay...")
            inputSelectorOverlay = InputSelectorOverlay(this, config, windowManager)
            
            Log.d(TAG, "Creating OutputWindowOverlay...")
            outputWindowOverlay = OutputWindowOverlay(
                context = this,
                config = config,
                windowManager = windowManager,
                onPositionChanged = { onOutputPositionChanged() },
                onTouched = { onOutputWindowTouched() }
            )
            
            Log.d(TAG, "Creating ToggleWidgetOverlay...")
            toggleWidgetOverlay = ToggleWidgetOverlay(
                context = this,
                config = config,
                windowManager = windowManager,
                onToggleClick = { toggleMagnification() }
            )
            
            // Create slider configuration
            Log.d(TAG, "Creating slider configuration...")
            val sliderConfig = createSliderConfig()
            
            Log.d(TAG, "Creating ZoomSlider...")
            zoomSlider = SliderFactory.createSlider(
                context = this,
                config = sliderConfig,
                windowManager = windowManager,
                onZoomChanged = { newZoom ->
                    config.setZoom(newZoom)
                    onZoomChanged()
                }
            )
            
            // Show all overlays with individual error handling
            var overlaysShown = 0
            
            try {
                Log.d(TAG, "Showing InputSelectorOverlay...")
                inputSelectorOverlay.show()
                overlaysShown++
                Log.d(TAG, "InputSelectorOverlay shown successfully")
            } catch (e: Exception) {
                Log.e(TAG, "FAILED to show InputSelectorOverlay", e)
                Toast.makeText(this, "ERROR showing input selector: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            try {
                Log.d(TAG, "Showing OutputWindowOverlay...")
                outputWindowOverlay.show()
                overlaysShown++
                Log.d(TAG, "OutputWindowOverlay shown successfully")
            } catch (e: Exception) {
                Log.e(TAG, "FAILED to show OutputWindowOverlay", e)
                Toast.makeText(this, "ERROR showing output window: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            try {
                Log.d(TAG, "Showing ToggleWidgetOverlay...")
                toggleWidgetOverlay.show()
                overlaysShown++
                Log.d(TAG, "ToggleWidgetOverlay shown successfully")
            } catch (e: Exception) {
                Log.e(TAG, "FAILED to show ToggleWidgetOverlay", e)
                Toast.makeText(this, "ERROR showing toggle widget: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            try {
                Log.d(TAG, "Showing ZoomSlider...")
                zoomSlider.show()
                overlaysShown++
                Log.d(TAG, "ZoomSlider shown successfully")
            } catch (e: Exception) {
                Log.e(TAG, "FAILED to show ZoomSlider", e)
                Toast.makeText(this, "ERROR showing zoom slider: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            Log.d(TAG, "Overlay creation complete: $overlaysShown/4 overlays shown")
            
            if (overlaysShown == 4) {
                Toast.makeText(this, "Magnifier ready!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "WARNING: Only $overlaysShown/4 overlays shown. Check logs.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR creating overlays", e)
            Toast.makeText(this, "CRITICAL ERROR: ${e.message}\n${e.stackTraceToString()}", Toast.LENGTH_LONG).show()
        }
    }
    
    
    private fun toggleMagnification() {
        config.isMagnifying = !config.isMagnifying
        
        if (config.isMagnifying) {
            inputSelectorOverlay.reveal()
            outputWindowOverlay.reveal()
            // Don't show slider automatically - wait for user to touch output window
            zoomSlider.setVisibility(false)
        } else {
            inputSelectorOverlay.hide()
            outputWindowOverlay.hide()
            zoomSlider.setVisibility(false)
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
        if (::zoomSlider.isInitialized && zoomSlider.isAttached()) {
            Log.d(TAG, "Output position changed to: (${config.outputPosition.x}, ${config.outputPosition.y})")
            zoomSlider.updateConfig(createSliderConfig())
        }
    }
    
    /**
     * Called when output window is touched
     */
    private fun onOutputWindowTouched() {
        if (config.isMagnifying && ::zoomSlider.isInitialized) {
            // Show slider with auto-hide timeout
            (zoomSlider as? SquareSlider)?.showWithTimeout()
            Log.d(TAG, "Output window touched - showing slider with timeout")
        }
    }
    
    /**
     * Helper method to create SliderConfig from current MagnifierConfig
     */
    private fun createSliderConfig(): SliderConfig {
        return SliderConfig(
            windowWidth = config.outputSize,
            windowHeight = config.outputSize,
            screenWidth = config.screenWidth,
            screenHeight = config.screenHeight,
            windowX = config.outputPosition.x,
            windowY = config.outputPosition.y,
            minZoom = config.minZoom,
            maxZoom = config.maxZoom,
            initialZoom = config.zoomFactor,
            shape = when (config.shape) {
                MagnifierShape.CIRCLE -> SliderConfig.Shape.CIRCLE
                MagnifierShape.SQUARE -> SliderConfig.Shape.SQUARE
            }
        )
    }
    
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "=== onStartCommand called ===")
        Log.d(TAG, "Intent: $intent")
        Log.d(TAG, "Action: ${intent?.action}")
        Log.d(TAG, "Flags: $flags, StartId: $startId")
        
        // Handle STOP_SERVICE action
        if (intent?.action == "STOP_SERVICE") {
            Log.d(TAG, "Received STOP_SERVICE action, stopping service...")
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        
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
                val outputSize = intent.getIntExtra("OUTPUT_SIZE", 500)
                val minZoom = intent.getFloatExtra("MIN_ZOOM", 1.5f)
                val maxZoom = intent.getFloatExtra("MAX_ZOOM", 6.0f)
                val isInputDraggable = intent.getBooleanExtra("INPUT_DRAGGABLE", false)
                val isOutputDraggable = intent.getBooleanExtra("OUTPUT_DRAGGABLE", true)
                val isWidgetDraggable = intent.getBooleanExtra("WIDGET_DRAGGABLE", false)
                val showCrosshair = intent.getBooleanExtra("SHOW_CROSSHAIR", false)
                
                Log.d(TAG, "Configuration received - isWidgetDraggable: $isWidgetDraggable")
                val crosshairColor = intent.getIntExtra("CROSSHAIR_COLOR", android.graphics.Color.BLACK)
                val colorFilterMode = intent.getStringExtra("COLOR_FILTER_MODE") ?: "NORMAL"
                val showZoomSlider = intent.getBooleanExtra("SHOW_ZOOM_SLIDER", false)
                
                // Apply configuration
                config.shape = MagnifierShape.valueOf(shapeName)
                config.inputSize = inputSize
                config.outputSize = outputSize
                config.setZoom(minZoom)  // Set initial zoom to min
                // Update min/max zoom range (need to modify MagnifierConfig to allow setting minZoom)
                config.maxZoom = maxZoom
                config.minZoom = minZoom
                config.isInputDraggable = isInputDraggable
                config.isOutputDraggable = isOutputDraggable
                config.isWidgetDraggable = isWidgetDraggable
                config.showCrosshair = showCrosshair
                
                Log.d(TAG, "Configuration applied - config.isWidgetDraggable: ${config.isWidgetDraggable}")
                config.crosshairColor = crosshairColor
                config.colorFilterMode = colorFilterMode
                
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
                Toast.makeText(this, "Starting magnifier... Widget Draggable: ${config.isWidgetDraggable}", Toast.LENGTH_LONG).show()
                
                // Create overlays if not already created
                if (!::inputSelectorOverlay.isInitialized) {
                    Log.d(TAG, "Creating overlays...")
                    createOverlays()
                    // Update slider position after overlays are created
                    onOutputPositionChanged()
                    // Start with slider hidden - it will show when user touches output window
                    zoomSlider.setVisibility(false)
                    
                    // IMPORTANT: Automatically enable magnification so overlays are visible
                    // Without this, overlays are created but hidden until user clicks toggle
                    Log.d(TAG, "Auto-enabling magnification...")
                    config.isMagnifying = true
                    inputSelectorOverlay.reveal()
                    outputWindowOverlay.reveal()
                    toggleWidgetOverlay.updateIcon(true)
                    Log.d(TAG, "Magnification auto-enabled, overlays should now be visible")
                } else {
                    Log.d(TAG, "Overlays already exist")
                }
                
                // Start screen capture
                Log.d(TAG, "Starting screen capture...")
                screenCaptureManager.startCapture(resultCode, data)
                Log.d(TAG, "Screen capture started")
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
        if (::zoomSlider.isInitialized) {
            zoomSlider.remove()
        }
        
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
