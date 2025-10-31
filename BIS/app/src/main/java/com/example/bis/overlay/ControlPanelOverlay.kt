package com.example.bis.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.bis.config.MagnifierConfig
import kotlin.math.abs

/**
 * Expandable control panel with zoom and size controls.
 * Can be collapsed to a small button or expanded to show all controls.
 */
class ControlPanelOverlay(
    private val context: Context,
    private val config: MagnifierConfig,
    private val windowManager: WindowManager,
    private val onZoomChanged: () -> Unit,
    private val onInputSizeChanged: () -> Unit
) {
    private lateinit var overlayView: LinearLayout
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isAttached = false
    private var isExpanded = false
    
    // UI elements
    private lateinit var toggleButton: Button
    private lateinit var controlsContainer: LinearLayout
    private lateinit var zoomLabel: TextView
    private lateinit var sizeLabel: TextView
    
    // Touch handling
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private val clickThreshold = 20
    private var isDragging = false
    
    /**
     * Create and show the control panel
     */
    fun show() {
        if (isAttached) return
        
        createView()
        windowManager.addView(overlayView, layoutParams)
        isAttached = true
    }
    
    /**
     * Remove the control panel
     */
    fun remove() {
        if (!isAttached) return
        
        windowManager.removeView(overlayView)
        isAttached = false
    }
    
    /**
     * Create the control panel view
     */
    private fun createView() {
        overlayView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            
            // Background
            val drawable = GradientDrawable().apply {
                setColor(Color.parseColor("#E0FFFFFF")) // 88% opacity white
                cornerRadius = 16f
                setStroke(3, Color.parseColor("#4CAF50"))
            }
            background = drawable
            
            // Toggle button (always visible)
            toggleButton = Button(context).apply {
                text = "⚙️ Controls"
                textSize = 14f
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    toggleExpanded()
                }
            }
            addView(toggleButton)
            
            // Controls container (collapsible)
            controlsContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                
                // Zoom controls
                addView(createSectionLabel("Zoom Level"))
                addView(createZoomControls())
                
                // Spacer
                addView(createSpacer())
                
                // Input size controls
                addView(createSectionLabel("Input Area Size"))
                addView(createSizeControls())
            }
            addView(controlsContainer)
            
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
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 100
        }
    }
    
    /**
     * Create section label
     */
    private fun createSectionLabel(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.DKGRAY)
            setPadding(0, 8, 0, 4)
        }
    }
    
    /**
     * Create spacer
     */
    private fun createSpacer(): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                16
            )
        }
    }
    
    /**
     * Create zoom controls (+ and - buttons with label)
     */
    private fun createZoomControls(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            
            // Zoom out button
            val zoomOutBtn = Button(context).apply {
                text = "−"
                textSize = 20f
                setPadding(24, 8, 24, 8)
                setOnClickListener {
                    config.decreaseZoom(0.5f)
                    updateZoomLabel()
                    onZoomChanged()
                }
            }
            addView(zoomOutBtn)
            
            // Zoom level label
            zoomLabel = TextView(context).apply {
                text = String.format("%.1fx", config.zoomFactor)
                textSize = 16f
                setTextColor(Color.BLACK)
                setPadding(16, 0, 16, 0)
                minWidth = 80
                gravity = Gravity.CENTER
            }
            addView(zoomLabel)
            
            // Zoom in button
            val zoomInBtn = Button(context).apply {
                text = "+"
                textSize = 20f
                setPadding(24, 8, 24, 8)
                setOnClickListener {
                    config.increaseZoom(0.5f)
                    updateZoomLabel()
                    onZoomChanged()
                }
            }
            addView(zoomInBtn)
        }
    }
    
    /**
     * Create size controls for input area
     */
    private fun createSizeControls(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            
            // Decrease size button
            val decreaseBtn = Button(context).apply {
                text = "−"
                textSize = 20f
                setPadding(24, 8, 24, 8)
                setOnClickListener {
                    config.inputSize = (config.inputSize - 50).coerceAtLeast(100)
                    updateSizeLabel()
                    onInputSizeChanged()
                }
            }
            addView(decreaseBtn)
            
            // Size label
            sizeLabel = TextView(context).apply {
                text = "${config.inputSize}px"
                textSize = 16f
                setTextColor(Color.BLACK)
                setPadding(16, 0, 16, 0)
                minWidth = 80
                gravity = Gravity.CENTER
            }
            addView(sizeLabel)
            
            // Increase size button
            val increaseBtn = Button(context).apply {
                text = "+"
                textSize = 20f
                setPadding(24, 8, 24, 8)
                setOnClickListener {
                    config.inputSize = (config.inputSize + 50).coerceAtMost(500)
                    updateSizeLabel()
                    onInputSizeChanged()
                }
            }
            addView(increaseBtn)
        }
    }
    
    /**
     * Toggle expanded/collapsed state
     */
    private fun toggleExpanded() {
        isExpanded = !isExpanded
        controlsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        toggleButton.text = if (isExpanded) "▼ Controls" else "⚙️ Controls"
    }
    
    /**
     * Update zoom label
     */
    private fun updateZoomLabel() {
        zoomLabel.text = String.format("%.1fx", config.zoomFactor)
    }
    
    /**
     * Update size label
     */
    private fun updateSizeLabel() {
        sizeLabel.text = "${config.inputSize}px"
    }
    
    /**
     * Touch listener for dragging
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
                        layoutParams.x = initialX - deltaX // Subtract for RIGHT gravity
                        layoutParams.y = initialY + deltaY
                        windowManager.updateViewLayout(overlayView, layoutParams)
                        return true
                    }
                    return false
                }
                MotionEvent.ACTION_UP -> {
                    return isDragging
                }
            }
            return false
        }
    }
}