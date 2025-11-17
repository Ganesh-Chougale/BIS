package com.example.bis.pkgs.main_activity_pkg.ui_components

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/**
 * UI component for crosshair toggles
 * Responsibility: Create and manage crosshair toggle switches
 */
class CrosshairToggles(private val context: Context) {
    
    lateinit var crosshairSwitch: Switch
    lateinit var outputCrosshairSwitch: Switch
    lateinit var outputCrosshairContainer: LinearLayout
    lateinit var widgetDraggableSwitch: Switch
    
    /**
     * Create crosshair toggle UI
     */
    fun createUI(
        parent: LinearLayout,
        onCrosshairChanged: (Boolean) -> Unit,
        onOutputCrosshairChanged: (Boolean) -> Unit,
        onWidgetDraggableChanged: (Boolean) -> Unit
    ) {
        // Container for output crosshair controls (shown only when input crosshair is enabled)
        outputCrosshairContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(32, 8, 0, 0)
        }
        
        val outputCrosshairLabel = TextView(context).apply {
            text = "Show Crosshair in Output:"
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }
        outputCrosshairContainer.addView(outputCrosshairLabel)
        
        outputCrosshairSwitch = Switch(context).apply {
            isChecked = false
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                text = if (isChecked) "ON" else "OFF"
                onOutputCrosshairChanged(isChecked)
            }
        }
        outputCrosshairContainer.addView(outputCrosshairSwitch)

        // Inline row: widget draggable & show crosshair in input
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 8)
        }

        val widgetContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        val widgetLabel = TextView(context).apply {
            text = "Widget Draggable:"
            textSize = 12f
        }
        widgetContainer.addView(widgetLabel)

        widgetDraggableSwitch = Switch(context).apply {
            isChecked = false
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                text = if (isChecked) "ON" else "OFF"
                onWidgetDraggableChanged(isChecked)
            }
        }
        widgetContainer.addView(widgetDraggableSwitch)

        val crosshairContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        val crosshairLabel = TextView(context).apply {
            text = "Show Crosshair in Input:"
            textSize = 12f
        }
        crosshairContainer.addView(crosshairLabel)

        crosshairSwitch = Switch(context).apply {
            isChecked = false
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                text = if (isChecked) "ON" else "OFF"
                outputCrosshairContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (!isChecked) {
                    outputCrosshairSwitch.isChecked = false
                    outputCrosshairSwitch.text = "OFF"
                    onOutputCrosshairChanged(false)
                }
                onCrosshairChanged(isChecked)
            }
        }
        crosshairContainer.addView(crosshairSwitch)

        row.addView(widgetContainer)
        row.addView(crosshairContainer)
        parent.addView(row)
        parent.addView(outputCrosshairContainer)
    }
}
