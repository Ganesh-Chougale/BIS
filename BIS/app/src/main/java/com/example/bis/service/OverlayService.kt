package com.example.bis.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.example.bis.capture.ScreenCaptureManager
import com.example.bis.config.MagnifierConfig
import com.example.bis.config.MagnifierShape
import com.example.bis.overlay.InputSelectorOverlay
import com.example.bis.overlay.OutputWindowOverlay
import com.example.bis.overlay.ToggleWidgetOverlay
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

        fun updateShader(context: android.content.Context, shaderName: String) {
            Log.d(TAG, "updateShader called with: $shaderName")
            val intent = Intent(context, OverlayService::class.java).apply {
                action = "UPDATE_SHADER"
                putExtra("SHADER", shaderName)
            }
            context.startService(intent)
        }

        fun updateShape(context: android.content.Context, shape: String) {
            Log.d(TAG, "updateShape called with: $shape")
            val intent = Intent(context, OverlayService::class.java).apply {
                action = "UPDATE_SHAPE"
                putExtra("SHAPE", shape)
            }
            context.startService(intent)
        }
    }

    private val TAG = "OverlayService"

    private lateinit var config: MagnifierConfig
    private lateinit var windowManager: WindowManager
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private lateinit var inputSelectorOverlay: InputSelectorOverlay
    private lateinit var outputWindowOverlay: OutputWindowOverlay
    private lateinit var toggleWidgetOverlay: ToggleWidgetOverlay
    private lateinit var zoomSlider: Slider

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== OverlayService onCreate called ===")

        config = MagnifierConfig()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // SCREEN SIZE FIXED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val windowMetrics = wm.currentWindowMetrics

            val insets = windowMetrics.windowInsets.getInsets(
                WindowInsets.Type.navigationBars() or WindowInsets.Type.statusBars()
            )

            val bounds = windowMetrics.bounds

            config.screenWidth = bounds.width()
            config.screenHeight = bounds.height() - insets.top - insets.bottom
            config.screenDensity = resources.displayMetrics.densityDpi

        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)

            config.screenWidth = metrics.widthPixels
            config.screenHeight = metrics.heightPixels
            config.screenDensity = metrics.densityDpi
        }

        Log.d(TAG, "Screen: ${config.screenWidth}x${config.screenHeight}")

        screenCaptureManager = ScreenCaptureManager(
            context = this,
            config = config,
            onFrameAvailable = { bitmap ->
                if (::outputWindowOverlay.isInitialized) {
                    outputWindowOverlay.updateMagnifiedView(bitmap)
                }
            }
        )

        startForegroundService()
    }

    private fun createOverlays() {
        Log.d(TAG, "createOverlays called")

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "ERROR: No overlay permission!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            inputSelectorOverlay = InputSelectorOverlay(this, config, windowManager)

            outputWindowOverlay = OutputWindowOverlay(
                context = this,
                config = config,
                windowManager = windowManager,
                onPositionChanged = { onOutputPositionChanged() },
                onTouched = { onOutputWindowTouched() }
            )

            toggleWidgetOverlay = ToggleWidgetOverlay(
                context = this,
                config = config,
                windowManager = windowManager,
                onToggleClick = { toggleMagnification() }
            )

            val sliderConfig = createSliderConfig()
            zoomSlider = SliderFactory.createSlider(
                context = this,
                config = sliderConfig,
                windowManager = windowManager,
                onZoomChanged = { newZoom ->
                    config.setZoom(newZoom)
                    onZoomChanged()
                }
            )

            inputSelectorOverlay.show()
            outputWindowOverlay.show()
            toggleWidgetOverlay.show()
            zoomSlider.show()

        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR creating overlays: ${e.message}", e)
        }
    }

    private fun toggleMagnification() {
        config.isMagnifying = !config.isMagnifying

        if (config.isMagnifying) {
            inputSelectorOverlay.reveal()
            outputWindowOverlay.reveal()
            zoomSlider.setVisibility(false)
        } else {
            inputSelectorOverlay.hide()
            outputWindowOverlay.hide()
            zoomSlider.setVisibility(false)
        }

        toggleWidgetOverlay.updateIcon(config.isMagnifying)
    }

    private fun onZoomChanged() {
        // This might be needed if zoom affects shader parameters in the future
    }

    private fun onOutputPositionChanged() {
        if (::zoomSlider.isInitialized && zoomSlider.isAttached()) {
            zoomSlider.updateConfig(createSliderConfig())
        }
    }

    private fun onOutputWindowTouched() {
        if (config.isMagnifying && ::zoomSlider.isInitialized) {
            (zoomSlider as? SquareSlider)?.showWithTimeout()
        }
    }

    /**
     * Touch listener for dragging the output window
     */
    private inner class OutputTouchListener : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private lateinit var layoutParams: WindowManager.LayoutParams

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    layoutParams = v.layoutParams as WindowManager.LayoutParams
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    onOutputWindowTouched()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = initialX + (event.rawX - initialTouchX).toInt()
                    val newY = initialY + (event.rawY - initialTouchY).toInt()

                    config.outputPosition.x = newX
                    config.outputPosition.y = newY

                    layoutParams.x = newX
                    layoutParams.y = newY
                    windowManager.updateViewLayout(v, layoutParams)

                    onOutputPositionChanged()
                    onOutputWindowTouched()

                    return true
                }
            }
            return false
        }
    }

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
        when (intent?.action) {
            "STOP_SERVICE" -> {
                performCompleteCleanup()
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            "UPDATE_OUTPUT_SIZE" -> {
                if (::outputWindowOverlay.isInitialized) {
                    val newSize = intent.getIntExtra("OUTPUT_SIZE", config.outputSize)
                    config.outputSize = newSize
                    outputWindowOverlay.updateSize()
                }
                return START_NOT_STICKY
            }
            "UPDATE_SHADER" -> {
                val shaderName = intent.getStringExtra("SHADER") ?: ""
                Log.d(TAG, "UPDATE_SHADER received: $shaderName")
                if (::outputWindowOverlay.isInitialized) {
                    outputWindowOverlay.updateShader(shaderName)
                } else {
                    Log.d(TAG, "UPDATE_SHADER received but outputWindowOverlay is not initialized yet")
                }
                return START_NOT_STICKY
            }
            "UPDATE_SHAPE" -> {
                Log.d(TAG, "UPDATE_SHAPE action received")
                val shapeName = intent.getStringExtra("SHAPE") ?: MagnifierShape.SQUARE.name
                Log.d(TAG, "Updating shape to: $shapeName")
                config.shape = MagnifierShape.valueOf(shapeName)
                Log.d(TAG, "Config shape updated to: ${config.shape}")
                
                // Update shape in outputWindowOverlay
                if (::outputWindowOverlay.isInitialized) {
                    Log.d(TAG, "Updating outputWindowOverlay shape to: ${config.shape}")
                    outputWindowOverlay.updateShape()
                }
                
                // Update slider with new shape
                if (::zoomSlider.isInitialized) {
                    Log.d(TAG, "Removing old zoom slider")
                    zoomSlider.remove()
                }
                Log.d(TAG, "Creating new zoom slider with shape: ${config.shape}")
                zoomSlider = SliderFactory.createSlider(this, createSliderConfig(), windowManager) { zoom ->
                    config.setZoom(zoom)
                }
                zoomSlider.show()
                Log.d(TAG, "Zoom slider shown")
                
                return START_NOT_STICKY
            }
        }

        if (intent?.action == "START_CAPTURE") {

            val resultCode = intent.getIntExtra("RESULT_CODE", 0)
            val data = pendingCaptureData

            if (data != null) {
                val shapeName = intent.getStringExtra("SHAPE") ?: MagnifierShape.SQUARE.name
                val inputSize = intent.getIntExtra("INPUT_SIZE", 200)
                val outputSize = intent.getIntExtra("OUTPUT_SIZE", 500)
                val minZoom = intent.getFloatExtra("MIN_ZOOM", 1.5f)
                val maxZoom = intent.getFloatExtra("MAX_ZOOM", 6.0f)
                val isInputDraggable = intent.getBooleanExtra("INPUT_DRAGGABLE", false)
                val isOutputDraggable = intent.getBooleanExtra("OUTPUT_DRAGGABLE", true)
                val isWidgetDraggable = intent.getBooleanExtra("WIDGET_DRAGGABLE", false)
                val showCrosshair = intent.getBooleanExtra("SHOW_CROSSHAIR", false)
                val showOutputCrosshair = intent.getBooleanExtra("SHOW_OUTPUT_CROSSHAIR", false)
                val shaderName = intent.getStringExtra("SHADER") ?: ""

                config.shape = MagnifierShape.valueOf(shapeName)
                config.inputSize = inputSize
                config.outputSize = outputSize
                config.minZoom = minZoom
                config.maxZoom = maxZoom
                config.setZoom(minZoom)
                config.isInputDraggable = isInputDraggable
                config.isOutputDraggable = isOutputDraggable
                config.isWidgetDraggable = isWidgetDraggable
                config.showCrosshair = showCrosshair
                config.showOutputCrosshair = showOutputCrosshair

                if (config.inputX == 0 && config.inputY == 0) {
                    config.inputX = config.screenWidth / 2 - config.inputSize / 2
                    config.inputY = config.screenHeight / 2 - config.inputSize / 2
                }

                if (config.outputPosition.x == 0 && config.outputPosition.y == 0) {
                    config.outputPosition.x = 20
                    config.outputPosition.y = 20
                }

                if (!::inputSelectorOverlay.isInitialized) {
                    createOverlays()
                    onOutputPositionChanged()
                    zoomSlider.setVisibility(false)

                    config.isMagnifying = true
                    inputSelectorOverlay.reveal()
                    outputWindowOverlay.reveal()
                    toggleWidgetOverlay.updateIcon(true)
                }

                if (shaderName.isNotEmpty() && ::outputWindowOverlay.isInitialized) {
                    Log.d(TAG, "Applying initial shader from START_CAPTURE: $shaderName")
                    outputWindowOverlay.updateShader(shaderName)
                }

                screenCaptureManager.startCapture(resultCode, data)
            }
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
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setContentTitle("Screen Magnifier")
            .setContentText("Magnifier is active")
            .setSmallIcon(android.R.drawable.ic_menu_zoom)
            .build()

        startForeground(1, notification)
    }

    private fun performCompleteCleanup() {
        try {
            if (::screenCaptureManager.isInitialized)
                screenCaptureManager.stopCapture()

            if (::inputSelectorOverlay.isInitialized) {
                inputSelectorOverlay.hide()
                inputSelectorOverlay.remove()
            }
            if (::outputWindowOverlay.isInitialized) {
                outputWindowOverlay.hide()
                outputWindowOverlay.remove()
            }
            if (::toggleWidgetOverlay.isInitialized) {
                toggleWidgetOverlay.remove()
            }
            if (::zoomSlider.isInitialized) {
                zoomSlider.setVisibility(false)
                zoomSlider.remove()
            }

            pendingCaptureData = null
            config.isMagnifying = false

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    override fun onDestroy() {
        performCompleteCleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
