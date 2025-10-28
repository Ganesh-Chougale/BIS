package com.example.bis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * Output window that displays the magnified view.
 * Supports both square and circle shapes. Draggable and shows real-time magnified content.
 */
class OutputWindowOverlay(
    private val context: Context,
    private val config: MagnifierConfig,
    private val windowManager: WindowManager,
    private val onPositionChanged: (() -> Unit)? = null
) {
    private lateinit var overlayView: FrameLayout
    private lateinit var magnifierImageView: ImageView
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isAttached = false
    
    // Touch handling
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    /**
     * Create and show the output window overlay
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
     * Update the magnified bitmap
     */
    fun updateMagnifiedView(bitmap: Bitmap) {
        if (!isAttached) return
        
        magnifierImageView.setImageBitmap(bitmap)
    }
    
    /**
     * Update the size of the output window based on zoom
     * NOTE: Output window size is now fixed at 500px. Zoom changes the magnification, not window size.
     */
    fun updateSize() {
        // No-op: Output window size is fixed
        // Zoom is handled by ScreenCaptureManager scaling the cropped bitmap
    }
    
    /**
     * Create the overlay view
     */
    private fun createView() {
        val outputSize = 500  // Fixed output window size
        
        overlayView = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(outputSize, outputSize)
            
            // Black background with green border
            val drawable = GradientDrawable().apply {
                setColor(Color.BLACK)
                setStroke(5, Color.parseColor("#4CAF50"))
                
                // Make it circular if shape is CIRCLE
                if (config.shape == MagnifierShape.CIRCLE) {
                    cornerRadius = (outputSize / 2).toFloat()
                }
            }
            background = drawable
            
            // ImageView to display magnified content
            magnifierImageView = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_XY
                
                // Clip to circle if shape is CIRCLE
                if (config.shape == MagnifierShape.CIRCLE) {
                    clipToOutline = true
                    outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setOval(0, 0, view.width, view.height)
                        }
                    }
                }
            }
            addView(magnifierImageView)
            
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
            gravity = Gravity.TOP or Gravity.START
            x = config.outputPosition.x
            y = config.outputPosition.y
        }
    }
    
    /**
     * Touch listener for dragging the output window
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
                    
                    // Update config
                    config.outputPosition.x = newX
                    config.outputPosition.y = newY
                    
                    // Update view position
                    layoutParams.x = newX
                    layoutParams.y = newY
                    windowManager.updateViewLayout(overlayView, layoutParams)
                    
                    // Notify that position changed (for slider to follow)
                    onPositionChanged?.invoke()
                    
                    return true
                }
            }
            return false
        }
    }
}