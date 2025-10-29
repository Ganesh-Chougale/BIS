package com.example.bis.slider.shape

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
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
        }
    }

    override fun isAttached(): Boolean = isSliderAttached

    override fun updateConfig(newConfig: SliderConfig) {
        config = newConfig
        updatePosition()
    }

    // --- single clean version ---
    private fun showZoomFeedback(zoom: Float) {
        zoomLabel.text = String.format("%.1fx", zoom)
        zoomLabel.alpha = 0.5f // 50% opacity
        zoomLabel.animate()
            .alpha(0.5f)
            .setDuration(100)
            .withEndAction {
                zoomLabel.postDelayed({
                    zoomLabel.animate().alpha(0f).setDuration(300).start()
                }, 1000) // visible for 1s
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

            // Zoom label - create but don't add to view hierarchy (used for feedback overlay)
            zoomLabel = TextView(context).apply {
                text = String.format("%.1fx", config.initialZoom)
                textSize = 14f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                alpha = 0f
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#80FFFFFF"))
                    cornerRadius = 8f
                }
            }
            // Note: zoomLabel is NOT added to view hierarchy to save space

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
                            }
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
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
