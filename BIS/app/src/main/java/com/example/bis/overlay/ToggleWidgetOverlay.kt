package com.example.bis.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import com.example.bis.config.MagnifierConfig
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Floating toggle widget to control magnification on/off.
 * Draggable with click detection.
 */
class ToggleWidgetOverlay(
    private val context: Context,
    private val config: MagnifierConfig,
    private val windowManager: WindowManager,
    private val onToggleClick: () -> Unit
) {
    private lateinit var overlayView: FrameLayout
    private lateinit var iconView: TextView
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isAttached = false
    
    // Touch handling
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private val clickThreshold = 20
    private var isDragging = false
    
    /**
     * Create and show the toggle widget overlay
     */
    fun show() {
        if (isAttached) return
        
        createView()
        windowManager.addView(overlayView, layoutParams)
        isAttached = true
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
     * Update the icon based on magnification state
     */
    fun updateIcon(isMagnifying: Boolean) {
        if (!isAttached) return
        
        iconView.text = if (isMagnifying) "o" else "x"
    }
    
    /**
     * Create the overlay view
     */
    private fun createView() {
        overlayView = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(120, 120)
            
            // Semi-transparent circular background
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
//                setColor(Color.parseColor("#80FFFFFF")) // 50% opacity white
//                setStroke(0, Color.parseColor("#4CAF50"))
            }
            background = drawable
            
            // Icon/text
            iconView = TextView(context).apply {
                text = if (config.isMagnifying) "o" else "x"
                textSize = 32f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            addView(iconView)
            
            setOnTouchListener(TouchListener())
        }
        
        // Window layout parameters
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = config.togglePosition.x
            y = config.togglePosition.y
        }
    }
    
    /**
     * Touch listener for dragging and click detection
     */
    private inner class TouchListener : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    
                    if (abs(deltaX) > clickThreshold || abs(deltaY) > clickThreshold) {
                        isDragging = true
                        
                        val newX = initialX + deltaX
                        val newY = initialY + deltaY
                        
                        // Update config
                        config.togglePosition.x = newX
                        config.togglePosition.y = newY
                        
                        // Update view position
                        layoutParams.x = newX
                        layoutParams.y = newY
                        windowManager.updateViewLayout(overlayView, layoutParams)
                        return true
                    }
                    return false
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // It's a click
                        v?.performClick()
                        onToggleClick()
                    }
                    return true
                }
            }
            return false
        }
    }
}