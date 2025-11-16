package com.example.bis.pkgs.main_activity_pkg.ui_components

import android.content.Context
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/**
 * UI component for draggable window toggles
 * Responsibility: Create and manage draggable toggle switches
 */
class DraggableToggles(private val context: Context) {
    
    lateinit var inputDraggableSwitch: Switch
    lateinit var outputDraggableSwitch: Switch
    lateinit var widgetDraggableSwitch: Switch
    
    /**
     * Create all draggable toggle switches
     */
    fun createUI(
        parent: LinearLayout,
        onInputDraggableChanged: (Boolean) -> Unit,
        onOutputDraggableChanged: (Boolean) -> Unit,
        onWidgetDraggableChanged: (Boolean) -> Unit
    ) {
        // Input draggable
        val inputLabel = TextView(context).apply {
            text = "Input Window Draggable:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        parent.addView(inputLabel)
        
        inputDraggableSwitch = Switch(context).apply {
            isChecked = false
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                text = if (isChecked) "ON" else "OFF"
                onInputDraggableChanged(isChecked)
            }
        }
        parent.addView(inputDraggableSwitch)
        
        // Output draggable
        val outputLabel = TextView(context).apply {
            text = "Output Window Draggable:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        parent.addView(outputLabel)
        
        outputDraggableSwitch = Switch(context).apply {
            isChecked = true
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                text = if (isChecked) "ON" else "OFF"
                onOutputDraggableChanged(isChecked)
            }
        }
        parent.addView(outputDraggableSwitch)
        
        // Widget draggable
        val widgetLabel = TextView(context).apply {
            text = "Widget Draggable:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        parent.addView(widgetLabel)
        
        widgetDraggableSwitch = Switch(context).apply {
            isChecked = false
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                text = if (isChecked) "ON" else "OFF"
                onWidgetDraggableChanged(isChecked)
            }
        }
        parent.addView(widgetDraggableSwitch)
    }
}
