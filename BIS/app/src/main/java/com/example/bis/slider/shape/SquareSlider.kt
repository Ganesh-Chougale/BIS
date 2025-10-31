package com.example.bis.slider.shape

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.example.bis.slider.Slider
import com.example.bis.slider.model.SliderConfig

class SquareSlider(
    private val context: Context,
    private var config: SliderConfig,
    private val windowManager: WindowManager
) : Slider {

    private val TAG = "SquareSlider"
    private lateinit var overlayView: LinearLayout
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var zoomSeekBar: SeekBar
    private lateinit var zoomLabel: TextView
    private var isSliderAttached = false
    private var zoomChangeListener: ((Float) -> Unit)? = null
    
    // Auto-hide functionality
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { setVisibility(false) }
    private val AUTO_HIDE_DELAY_MS = 3000L // 3 seconds

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
        cancelAutoHideTimer()
        windowManager.removeView(overlayView)
        isSliderAttached = false
    }

    override fun updatePosition() {
        if (!isSliderAttached) return

        val screenCenterX = config.screenWidth / 2
        val outputCenterX = config.windowX + (config.windowWidth / 2)
        val isOutputOnLeft = outputCenterX < screenCenterX

        layoutParams.gravity = Gravity.START or Gravity.TOP

        val density = context.resources.displayMetrics.density
        val sliderWidthPx = (80 * density).toInt()
        val borderWidth = 5 // Output window border width

        if (isOutputOnLeft) {
            // Position right next to the border on the right side
            layoutParams.x = config.windowX + config.windowWidth - (borderWidth + 90)
        } else {
            // Position right next to the border on the left side
            layoutParams.x = config.windowX - sliderWidthPx + (borderWidth + 90)
        }

        layoutParams.y = config.windowY

        Log.d(
            TAG,
            "SquareSlider positioned at x=${layoutParams.x}, y=${layoutParams.y} (output at ${config.windowX},${config.windowY})"
        )
        windowManager.updateViewLayout(overlayView, layoutParams)
    }

    override fun setOnZoomChangeListener(listener: (Float) -> Unit) {
        zoomChangeListener = listener
    }

    override fun setVisibility(visible: Boolean) {
        if (isSliderAttached) {
            overlayView.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                resetAutoHideTimer()
            } else {
                cancelAutoHideTimer()
            }
        }
    }
    
    /**
     * Show the slider and start auto-hide timer
     */
    fun showWithTimeout() {
        setVisibility(true)
    }
    
    /**
     * Reset the auto-hide timer (called when user interacts)
     */
    fun resetAutoHideTimer() {
        cancelAutoHideTimer()
        hideHandler.postDelayed(hideRunnable, AUTO_HIDE_DELAY_MS)
    }
    
    /**
     * Cancel the auto-hide timer
     */
    private fun cancelAutoHideTimer() {
        hideHandler.removeCallbacks(hideRunnable)
    }

    override fun isAttached(): Boolean = isSliderAttached

    override fun updateConfig(newConfig: SliderConfig) {
        config = newConfig
        updatePosition()
    }

    /**
     * Show zoom level feedback with fade animation
     */
    private fun showZoomFeedback(zoom: Float) {
        zoomLabel.text = String.format("%.1fx", zoom)
        // Cancel any pending animations
        zoomLabel.animate().cancel()
        zoomLabel.removeCallbacks(null)
        
        // Show with fade in
        zoomLabel.alpha = 0f
        zoomLabel.visibility = View.VISIBLE
        zoomLabel.animate()
            .alpha(1f)
            .setDuration(150)
            .withEndAction {
                // Keep visible for 1.5 seconds, then fade out
                zoomLabel.postDelayed({
                    zoomLabel.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction {
                            zoomLabel.visibility = View.GONE
                        }
                        .start()
                }, 1500)
            }
            .start()
    }

    private fun createView() {
        overlayView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            clipChildren = false
            clipToPadding = false
            // No background - just the slider itself

            // Zoom level feedback label
            zoomLabel = TextView(context).apply {
                text = String.format("%.1fx", config.initialZoom)
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                visibility = View.GONE
                setPadding(24, 12, 24, 12)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#E0000000")) // 88% opacity black
                    cornerRadius = 12f
                }
                elevation = 8f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setMargins(0, 0, 0, 16)
                }
            }
            addView(zoomLabel)

            // Slider container - wraps the rotated SeekBar
            val seekBarContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)

                val density = context.resources.displayMetrics.density

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 0)
                }

                zoomSeekBar = SeekBar(context).apply {
                    max = ((config.maxZoom - config.minZoom) * 10).toInt()
                    progress = ((config.initialZoom - config.minZoom) * 10).toInt()
                    rotation = 270f
                    // Width = track thickness, Height = slider length (becomes visual height when rotated)
                    layoutParams = LinearLayout.LayoutParams(
                        (80 * density).toInt(),  // Small width for track thickness
                        config.windowHeight      // Height becomes visual length when rotated
                    ).apply { gravity = Gravity.CENTER }
                    setPadding(0, 0, 0, 0)
                    minimumHeight = 0
                    minimumWidth = 0
                    maxHeight = config.windowHeight  // Force inner track to take full height
                    thumbOffset = 0  // Remove thumb offset to maximize track length

                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                val newZoom = config.minZoom + (progress / 10f)
                                showZoomFeedback(newZoom)
                                zoomChangeListener?.invoke(newZoom)
                                resetAutoHideTimer() // Reset timer on interaction
                            }
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            resetAutoHideTimer() // Reset timer when user starts touching
                        }
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })
                }
                addView(zoomSeekBar)

                // Allow touch anywhere on container
                setOnTouchListener { v, e ->
                    if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_MOVE) {
                        val relativeY = e.y.coerceIn(0f, v.height.toFloat())
                        val progressRatio = 1f - (relativeY / v.height)
                        val newProgress = (progressRatio * zoomSeekBar.max)
                            .toInt().coerceIn(0, zoomSeekBar.max)
                        zoomSeekBar.progress = newProgress

                        val newZoom = config.minZoom + (newProgress / 10f)
                        showZoomFeedback(newZoom)
                        zoomChangeListener?.invoke(newZoom)
                        resetAutoHideTimer() // Reset timer on touch
                        true
                    } else false
                }
            }
            addView(seekBarContainer)
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }
}
