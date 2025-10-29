package com.example.bis.slider.shape

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.bis.slider.Slider
import com.example.bis.slider.model.SliderConfig
import kotlin.math.*
import android.os.Handler
import android.os.Looper

class CircleSlider(
    private val context: Context,
    private var config: SliderConfig,
    private val windowManager: WindowManager
) : Slider {
    private val TAG = "CircleSlider"
    private lateinit var overlayView: View
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isSliderAttached = false
    private var zoomChangeListener: ((Float) -> Unit)? = null
    private var currentAngle = 0f // Will be initialized based on initialZoom
    private var feedbackAlpha = 0f  // Alpha for zoom feedback (0-1)
    private var feedbackHandler: Handler? = null
    private var feedbackRunnable: Runnable? = null
    private var isOutputOnLeft = false // Track which side output is on

    // Drawing constants
    private val strokeWidth = 12f
    private val thumbRadius = 24f
    private val padding = 8f

    override fun show() {
        if (isSliderAttached) return
        createView()
        updatePosition()
        windowManager.addView(overlayView, layoutParams)
        isSliderAttached = true
    }

    override fun hide() {
        if (!isSliderAttached) return
        overlayView.visibility = View.GONE
    }

    override fun remove() {
        if (!isSliderAttached) return
        windowManager.removeView(overlayView)
        isSliderAttached = false
    }

    override fun updatePosition() {
        if (!isSliderAttached) return

        val screenCenterX = config.screenWidth / 2
        val outputCenterX = config.windowX + (config.windowWidth / 2)
        isOutputOnLeft = outputCenterX < screenCenterX

        // Position slider on the side of the output window
        layoutParams.gravity = Gravity.START or Gravity.TOP

        // Use half the window width since it's a half-circle
        val sliderSize = (config.windowWidth / 2) + 40  // Half width + some padding for the arc
        val borderWidth = 5 // Output window border width

        if (isOutputOnLeft) {
            // Output on left -> slider on right side with gap
            layoutParams.x = config.windowX + config.windowWidth + 10
        } else {
            // Output on right -> slider on left side with gap
            layoutParams.x = config.windowX - sliderSize - 10
        }

        // Center vertically with output window
        layoutParams.y = config.windowY

        Log.d(TAG, "CircleSlider positioned at x=${layoutParams.x}, y=${layoutParams.y}, size=${sliderSize} (output at ${config.windowX},${config.windowY})")
        windowManager.updateViewLayout(overlayView, layoutParams)
        
        // Redraw to update arc direction
        if (::overlayView.isInitialized) {
            overlayView.invalidate()
        }
    }

    override fun setOnZoomChangeListener(listener: (Float) -> Unit) {
        zoomChangeListener = listener
    }

    override fun setVisibility(visible: Boolean) {
        if (isSliderAttached) {
            overlayView.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    override fun isAttached(): Boolean = isSliderAttached

    override fun updateConfig(newConfig: SliderConfig) {
        config = newConfig
        updatePosition()
    }

    private fun showZoomFeedback() {
        // Cancel any existing feedback timer
        feedbackRunnable?.let { feedbackHandler?.removeCallbacks(it) }

        // Show feedback
        feedbackAlpha = 1f
        overlayView.invalidate()

        // Initialize handler if needed
        if (feedbackHandler == null) {
            feedbackHandler = Handler(Looper.getMainLooper())
        }

        // Hide after 1 second
        feedbackRunnable = Runnable {
            // Fade out animation
            val fadeSteps = 10
            var step = 0
            val fadeHandler = Handler(Looper.getMainLooper())
            val fadeRunnable = object : Runnable {
                override fun run() {
                    step++
                    feedbackAlpha = 1f - (step.toFloat() / fadeSteps)
                    overlayView.invalidate()
                    if (step < fadeSteps) {
                        fadeHandler.postDelayed(this, 30)  // 300ms total fade
                    }
                }
            }
            fadeHandler.post(fadeRunnable)
        }
        feedbackHandler?.postDelayed(feedbackRunnable!!, 1000)
    }

    private fun createView() {
        // Initialize currentAngle based on initialZoom
        val initialProgress = (config.initialZoom - config.minZoom) / (config.maxZoom - config.minZoom)
        currentAngle = 90f + (initialProgress * 180f)
        
        overlayView = object : View(context) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = this@CircleSlider.strokeWidth
                color = Color.parseColor("#4CAF50")
            }

            private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.WHITE
                strokeWidth = 2f
            }

            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 36f
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)

                val centerX = width / 2f
                val centerY = height / 2f
                val radius = minOf(centerX, centerY) - padding - (strokeWidth / 2)

                // Draw arc track (vertical half-circle on the side)
                val rect = RectF(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius
                )

                // Determine arc direction based on output position
                val startAngle: Float
                val sweepAngle = 180f
                
                if (isOutputOnLeft) {
                    // Output on left -> draw right half-circle (270° to 90°)
                    startAngle = 270f
                } else {
                    // Output on right -> draw left half-circle (90° to 270°)
                    startAngle = 90f
                }

                // Draw background arc
                paint.color = Color.parseColor("#CCCCCC")
                canvas.drawArc(rect, startAngle, sweepAngle, false, paint)

                // Calculate progress from bottom to top
                val normalizedProgress = if (currentAngle >= 90f && currentAngle <= 270f) {
                    (currentAngle - 90f) / 180f  // 0.0 to 1.0
                } else {
                    0f
                }

                // Draw progress arc (cap at 179° to prevent full circle)
                paint.color = Color.parseColor("#4CAF50")
                val progressSweepAngle = (normalizedProgress * 180f).coerceAtMost(179f)
                canvas.drawArc(rect, 90f, progressSweepAngle, false, paint)

                // Draw thumb
                val angleRad = Math.toRadians(currentAngle.toDouble())
                val thumbX = centerX + (radius * cos(angleRad)).toFloat()
                val thumbY = centerY + (radius * sin(angleRad)).toFloat()

                // Thumb with border
                canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = Color.parseColor("#4CAF50")
                canvas.drawCircle(thumbX, thumbY, thumbRadius, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = this@CircleSlider.strokeWidth

                // Draw zoom level text near the center (always visible)
                val zoomLevel = config.minZoom + (normalizedProgress * (config.maxZoom - config.minZoom))
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#80FFFFFF")  // 50% opacity white
                    style = Paint.Style.FILL
                }
                val textBounds = android.graphics.Rect()
                val zoomText = "%.1fx".format(zoomLevel.coerceIn(config.minZoom, config.maxZoom))
                textPaint.getTextBounds(zoomText, 0, zoomText.length, textBounds)

                // Draw background for text
                val textBgRect = RectF(
                    centerX - textBounds.width() / 2 - 16f,
                    centerY - textBounds.height() / 2 - 16f,
                    centerX + textBounds.width() / 2 + 16f,
                    centerY + textBounds.height() / 2 + 16f
                )
                canvas.drawRoundRect(textBgRect, 8f, 8f, bgPaint)

                // Draw text
                canvas.drawText(
                    zoomText,
                    centerX,
                    centerY + (textPaint.textSize / 3),
                    textPaint
                )
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        val centerX = width / 2f
                        val centerY = height / 2f
                        val x = event.x - centerX
                        val y = event.y - centerY

                        // Calculate angle from center (0° = right, 90° = bottom, 180° = left, 270° = top)
                        var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                        if (angle < 0) angle += 360f

                        // Constrain angle based on which side the slider is on
                        currentAngle = if (isOutputOnLeft) {
                            // Right half-circle: 270° (top) to 90° (bottom)
                            when {
                                angle >= 270f -> angle  // Top half: 270° to 360°
                                angle <= 90f -> angle   // Bottom half: 0° to 90°
                                angle < 180f -> 90f     // Left side bottom -> snap to 90°
                                else -> 270f            // Left side top -> snap to 270°
                            }
                        } else {
                            // Left half-circle: 90° (bottom) to 270° (top)
                            when {
                                angle >= 90f && angle <= 270f -> angle  // Valid range
                                angle < 90f -> 90f      // Below range -> snap to 90°
                                else -> 270f            // Above range -> snap to 270°
                            }
                        }

                        // Calculate zoom level: 90° (bottom) = min zoom, 270° (top) = max zoom
                        val normalizedProgress = if (currentAngle >= 90f && currentAngle <= 270f) {
                            (currentAngle - 90f) / 180f
                        } else if (currentAngle > 270f) {
                            // Handle 270° to 360° range
                            (currentAngle - 90f) / 180f
                        } else {
                            // Handle 0° to 90° range
                            (currentAngle + 270f) / 180f
                        }

                        val zoomLevel = config.minZoom + (normalizedProgress.coerceIn(0f, 1f) * (config.maxZoom - config.minZoom))
                        zoomChangeListener?.invoke(zoomLevel)

                        showZoomFeedback()
                        invalidate()
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }
        }
        
        // Set size to match window height for proper half-circle
        val size = config.windowHeight
        layoutParams = WindowManager.LayoutParams(
            (config.windowWidth / 2) + 40,  // Half width + padding
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }
}