package com.example.bis.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import com.example.bis.config.MagnifierConfig
import com.example.bis.config.MagnifierShape
import com.example.bis.filter.ColorFilterProcessor
import com.example.bis.renderer.MagnifierSurfaceView

/**
 * Output window that displays the magnified view.
 * Supports both square and circle shapes. Draggable and shows real-time magnified content.
 */
class OutputWindowOverlay(
    private val context: Context,
    private val config: MagnifierConfig,
    private val windowManager: WindowManager,
    private val onPositionChanged: (() -> Unit)? = null,
    private val onTouched: (() -> Unit)? = null
) {
    private lateinit var overlayView: FrameLayout
    private lateinit var magnifierSurfaceView: MagnifierSurfaceView

    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isAttached = false
    private var crosshairView: TextView? = null
    
    // Touch handling
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    /**
     * Create and show the output window overlay
     */
    fun show() {
        try {
            if (isAttached) {
                // If already attached, update the existing view
                updateShape()
                android.util.Log.d("OutputWindowOverlay", "Updated existing view")
            } else {
                // Otherwise create and show new view
                android.util.Log.d("OutputWindowOverlay", "Creating view...")
                createView()
                android.util.Log.d("OutputWindowOverlay", "Adding view to window manager...")
                windowManager.addView(overlayView, layoutParams)
                isAttached = true
                android.util.Log.d("OutputWindowOverlay", "Successfully shown")
            }
        } catch (e: Exception) {
            android.util.Log.e("OutputWindowOverlay", "Failed to show/update overlay", e)
            throw e  // Re-throw to let caller handle
        }
    }
    
    /**
     * Update the shape of the output window
     */
    fun updateShape() {
        if (!isAttached) return
        
        // Update the background shape
        val drawable = GradientDrawable().apply {
            if (config.shape == MagnifierShape.CIRCLE) {
                // Circle: transparent fill with green ring only
                setColor(Color.TRANSPARENT)
                setStroke(8, Color.parseColor("#4CAF50"))
                cornerRadius = (config.outputSize / 2).toFloat()
            } else {
                // Square: semi-transparent dark background with green border
                setColor(Color.parseColor("#CC000000"))
                setStroke(8, Color.parseColor("#4CAF50"))
                cornerRadius = 0f
            }
        }
        overlayView.background = drawable
        
        // Update the GLSurfaceView clipping and inform renderer which shape to draw
        val isCircle = config.shape == MagnifierShape.CIRCLE
        if (isCircle) {
            magnifierSurfaceView.clipToOutline = true
            magnifierSurfaceView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        } else {
            magnifierSurfaceView.clipToOutline = false
            magnifierSurfaceView.outlineProvider = null
        }
        // Renderer uses circle geometry when isCircle=true, quad when false
        magnifierSurfaceView.setShape(isCircle)
        
        // Force redraw
        overlayView.invalidate()
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
        
        // Apply color filter using ColorFilterProcessor, then send to GL renderer
        val filteredBitmap = ColorFilterProcessor.applyFilter(bitmap, config.colorFilterMode)
        magnifierSurfaceView.updateBitmap(filteredBitmap)
    }

    /**
     * Update the active shader used by the GLSL renderer.
     */
    fun updateShader(shaderName: String) {
        if (!isAttached) return
        magnifierSurfaceView.setShader(shaderName)
    }
    
    /**
     * Update the size of the output window when config.outputSize changes.
     * Keeps the overlay constrained to a square of outputSize × outputSize.
     */
    fun updateSize() {
        if (!isAttached) return

        val newSize = config.outputSize

        // Update the container view's layout params
        overlayView.layoutParams = ViewGroup.LayoutParams(newSize, newSize)

        // Update the WindowManager layout params so the system respects the new size
        layoutParams.width = newSize
        layoutParams.height = newSize
        windowManager.updateViewLayout(overlayView, layoutParams)

        // Ensure the GL surface fills the overlay's bounds
        magnifierSurfaceView.layoutParams = FrameLayout.LayoutParams(newSize, newSize)
    }
    
    /**
     * Create the overlay view
     */
    private fun createView() {
        val outputSize = config.outputSize
        
        overlayView = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(outputSize, outputSize)
            
            // Semi-transparent dark background for square; circle uses transparent fill with ring
            val drawable = GradientDrawable().apply {
                if (config.shape == MagnifierShape.CIRCLE) {
                    setColor(Color.TRANSPARENT)
                    setStroke(8, Color.parseColor("#4CAF50"))
                    cornerRadius = (outputSize / 2).toFloat()
                } else {
                    setColor(Color.parseColor("#CC000000"))
                    setStroke(8, Color.parseColor("#4CAF50"))
                }
            }
            background = drawable
            
            // GLSurfaceView to display magnified content with shaders
            magnifierSurfaceView = MagnifierSurfaceView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                
                // Clip to circle and configure renderer shape if initial shape is CIRCLE
                if (config.shape == MagnifierShape.CIRCLE) {
                    clipToOutline = true
                    outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setOval(0, 0, view.width, view.height)
                        }
                    }
                    setShape(true)
                } else {
                    setShape(false)
                }
            }
            addView(magnifierSurfaceView)
            
            // Add crosshair overlay if enabled
            if (config.showOutputCrosshair) {
                addCrosshair(this)
            }
            
            // Only enable dragging if configured
            if (config.isOutputDraggable) {
                setOnTouchListener(TouchListener())
            }
        }
        
        // Window layout parameters (constrained to outputSize square)
        layoutParams = WindowManager.LayoutParams(
            outputSize,
            outputSize,
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
     * Add a crosshair in the center of the output window
     */
    private fun addCrosshair(parent: FrameLayout) {
        crosshairView = TextView(context).apply {
            text = "+"
            textSize = 24f
            setTextColor(config.crosshairColor)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            // Make crosshair non-interactive
            isClickable = false
            isFocusable = false
            // Set high elevation to ensure it appears above the image
            elevation = 10f
        }
        parent.addView(crosshairView)
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
                    // Notify that output window was touched (to show slider)
                    onTouched?.invoke()
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
                    // Also notify touch (to reset slider timeout)
                    onTouched?.invoke()
                    
                    return true
                }
            }
            return false
        }
    }
}