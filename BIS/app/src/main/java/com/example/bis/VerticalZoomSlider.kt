package com.example.bis

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * Vertical zoom slider that appears on the opposite side of the output window.
 * Only visible when magnification is ON.
 */
class VerticalZoomSlider(
    private val context: Context,
    private val config: MagnifierConfig,
    private val windowManager: WindowManager,
    private val onZoomChanged: () -> Unit
) {
    private val TAG = "VerticalZoomSlider"
    
    private lateinit var overlayView: LinearLayout
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var zoomSeekBar: SeekBar
    private lateinit var zoomLabel: TextView
    private var isAttached = false
    
    /**
     * Show the zoom slider
     */
    fun show() {
        if (isAttached) return
        
        createView()
        updatePosition()
        windowManager.addView(overlayView, layoutParams)
        isAttached = true
    }
    
    /**
     * Hide the slider
     */
    fun hide() {
        if (!isAttached) return
        overlayView.visibility = View.GONE
    }
    
    /**
     * Reveal the slider
     */
    fun reveal() {
        if (!isAttached) return
        overlayView.visibility = View.VISIBLE
        updatePosition()  // Update position in case output window moved
    }
    
    /**
     * Remove the slider
     */
    fun remove() {
        if (!isAttached) return
        windowManager.removeView(overlayView)
        isAttached = false
    }
    
    /**
     * Update slider position based on output window location
     */
    fun updatePosition() {
        if (!isAttached) return
        
        val fixedOutputSize = 500  // Fixed output window size
        val screenCenterX = config.screenWidth / 2
        val outputCenterX = config.outputPosition.x + (fixedOutputSize / 2)
        
        // Determine which side of screen the output window is on
        val isOutputOnLeft = outputCenterX < screenCenterX
        
        if (isOutputOnLeft) {
            // Output on left side -> slider on right side of output
            layoutParams.gravity = Gravity.START or Gravity.TOP
            layoutParams.x = config.outputPosition.x + fixedOutputSize + 10  // 10px gap
        } else {
            // Output on right side -> slider on left side of output
            layoutParams.gravity = Gravity.START or Gravity.TOP
            // Get slider width (approximately 100px for the rotated seekbar)
            val sliderWidth = 100
            layoutParams.x = config.outputPosition.x - sliderWidth - 10  // 10px gap on left
        }
        
        layoutParams.y = config.outputPosition.y  // Align with output window vertically
        
        Log.d(TAG, "Slider positioned at x=${layoutParams.x}, y=${layoutParams.y} (output on ${if (isOutputOnLeft) "LEFT" else "RIGHT"})")
        
        windowManager.updateViewLayout(overlayView, layoutParams)
    }
    
    /**
     * Create the slider view
     */
    private fun createView() {
        overlayView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 16, 12, 16)
            
            // Background
            val drawable = GradientDrawable().apply {
                setColor(Color.parseColor("#D0FFFFFF"))  // 82% opacity white
                cornerRadius = 16f
                setStroke(2, Color.parseColor("#4CAF50"))
            }
            background = drawable
            
            // Zoom label
            zoomLabel = TextView(context).apply {
                text = String.format("%.1fx", config.zoomFactor)
                textSize = 14f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 8)
            }
            addView(zoomLabel)
            
            // Vertical SeekBar
            zoomSeekBar = SeekBar(context).apply {
                // Map zoom range (minZoom to maxZoom) with 0.1 step precision
                max = ((config.maxZoom - config.minZoom) * 10).toInt()
                progress = ((config.zoomFactor - config.minZoom) * 10).toInt()
                
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    400  // Height of slider
                )
                
                // Rotate to make it vertical
                rotation = 270f
                
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            val newZoom = config.minZoom + (progress / 10f)
                            config.setZoom(newZoom)
                            zoomLabel.text = String.format("%.1fx", config.zoomFactor)
                            // Update seekbar to reflect actual zoom (in case it was clamped)
                            val actualProgress = ((config.zoomFactor - config.minZoom) * 10).toInt()
                            if (actualProgress != progress) {
                                seekBar?.progress = actualProgress
                            }
                            onZoomChanged()
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
            addView(zoomSeekBar)
        }
        
        // Window layout parameters
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }
}