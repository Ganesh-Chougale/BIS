package com.example.bis

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Input selector overlay that allows user to select the area to magnify.
 * Supports both square and circle shapes. Draggable.
 */
class InputSelectorOverlay(
    private val context: Context,
    private val config: MagnifierConfig,
    private val windowManager: WindowManager
) {
    private lateinit var overlayView: FrameLayout
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isAttached = false
    
    // Touch handling
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    /**
     * Create and show the input selector overlay
     */
    fun show() {
        if (isAttached) return
        
        createView()
        windowManager.addView(overlayView, layoutParams)
        isAttached = true
    }
    
    /**
     * Hide the overlay
     */
    fun hide() {
        if (!isAttached) return
        
        overlayView.visibility = View.GONE
    }
    
    /**
     * Show the overlay (if already created)
     */
    fun reveal() {
        if (!isAttached) return
        
        overlayView.visibility = View.VISIBLE
    }
    
    /**
     * Remove the overlay from window
     */
    fun remove() {
        if (!isAttached) return
        
        windowManager.removeView(overlayView)
        isAttached = false
    }
    
    /**
     * Update the size of the input selector
     */
    fun updateSize(newSize: Int) {
        config.inputSize = newSize
        overlayView.layoutParams = ViewGroup.LayoutParams(newSize, newSize)
        windowManager.updateViewLayout(overlayView, layoutParams)
    }
    
    /**
     * Create the overlay view
     */
    private fun createView() {
        overlayView = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(config.inputSize, config.inputSize)
            
            // Green border with semi-transparent fill
            val drawable = GradientDrawable().apply {
                setStroke(8, Color.parseColor("#4CAF50"))
                setColor(Color.parseColor("#204CAF50")) // 20% opacity green
                
                // Make it circular if shape is CIRCLE
                if (config.shape == MagnifierShape.CIRCLE) {
                    cornerRadius = (config.inputSize / 2).toFloat()
                }
            }
            background = drawable
            
            // Add crosshair in center (for better targeting) if enabled
            if (config.showCrosshair) {
                addCrosshair(this)
            }
            
            // Only enable dragging if configured
            if (config.isInputDraggable) {
                setOnTouchListener(TouchListener())
            }
        }
        
        // Window layout parameters
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            
            // Set initial position from config or center of screen
            if (config.inputX == 0 && config.inputY == 0) {
                config.inputX = config.screenWidth / 2 - config.inputSize / 2
                config.inputY = config.screenHeight / 2 - config.inputSize / 2
            }
            
            x = config.inputX
            y = config.inputY
        }
    }
    
    /**
     * Add a crosshair in the center for better targeting
     */
    private fun addCrosshair(parent: FrameLayout) {
        val crosshair = TextView(context).apply {
            text = "+"
            textSize = 24f
            setTextColor(Color.parseColor("#4CAF50"))
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        parent.addView(crosshair)
    }
    
    /**
     * Touch listener for dragging the input selector
     */
    private inner class TouchListener : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = initialX + (event.rawX - initialTouchX).toInt()
                    val newY = initialY + (event.rawY - initialTouchY).toInt()
                    
                    // Update config directly
                    config.inputX = newX
                    config.inputY = newY
                    
                    // Update view position
                    layoutParams.x = newX
                    layoutParams.y = newY
                    windowManager.updateViewLayout(overlayView, layoutParams)
                    return true
                }
            }
            return false
        }
    }
}