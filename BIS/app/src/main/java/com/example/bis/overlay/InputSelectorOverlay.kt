package com.example.bis.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import com.example.bis.config.MagnifierConfig
import com.example.bis.config.MagnifierShape
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
    private var crosshairView: TextView? = null
    
    // Touch handling
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    /**
     * Create and show the input selector overlay
     */
    fun show() {
        if (isAttached) {
            android.util.Log.d("InputSelectorOverlay", "Already attached, skipping show()")
            return
        }
        
        try {
            android.util.Log.d("InputSelectorOverlay", "Creating view...")
            createView()
            android.util.Log.d("InputSelectorOverlay", "Adding view to window manager...")
            windowManager.addView(overlayView, layoutParams)
            isAttached = true
            android.util.Log.d("InputSelectorOverlay", "Successfully shown")
        } catch (e: WindowManager.BadTokenException) {
            android.util.Log.e("InputSelectorOverlay", "Failed to show overlay: BadTokenException", e)
            throw e  // Re-throw to let caller handle
        } catch (e: Exception) {
            android.util.Log.e("InputSelectorOverlay", "Failed to show overlay", e)
            throw e  // Re-throw to let caller handle
        }
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

        // Circle support
        val drawable = GradientDrawable().apply {
            if (config.shape == MagnifierShape.CIRCLE) {
                cornerRadius = (config.inputSize / 2f)
            }
            setColor(Color.TRANSPARENT) // no border
        }
        background = drawable

        if (config.showCrosshair) addCrosshair(this)

        if (config.isInputDraggable) {
            setOnTouchListener(TouchListener())
        }
    }

    // IMPORTANT FIX: use exact size so gravity CENTER works
    layoutParams = WindowManager.LayoutParams(
        config.inputSize,
        config.inputSize,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {

if (config.inputX == 0 && config.inputY == 0) {
    // compute real center manually
    val cx = (config.screenWidth - config.inputSize) / 2
    val cy = (config.screenHeight - config.inputSize) / 2

    config.inputX = cx
    config.inputY = cy

    gravity = Gravity.TOP or Gravity.START
    x = cx
    y = cy
} else {
    gravity = Gravity.TOP or Gravity.START
    x = config.inputX
    y = config.inputY
}

    }
}
    /**
     * Add a crosshair in the center for better targeting
     */
    private fun addCrosshair(parent: FrameLayout) {
        crosshairView = TextView(context).apply {
            text = "+"
            textSize = 36f  // Larger text size
            setTextColor(config.crosshairColor)
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            
            // Use MATCH_PARENT to fill the entire input selector area
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
            }
            
            // Make crosshair non-interactive
            isClickable = false
            isFocusable = false
            
            // Add some styling for better visibility
            setBackgroundColor(Color.TRANSPARENT)
            elevation = 10f
            
            // Add text shadow for better visibility
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        parent.addView(crosshairView)
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