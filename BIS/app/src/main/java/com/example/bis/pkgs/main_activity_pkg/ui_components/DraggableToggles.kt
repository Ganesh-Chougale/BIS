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
        // Output draggable
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 8)
        }
        
        val inputContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        val inputLabel = TextView(context).apply {
            text = "Input Window Draggable:"
            textSize = 12f
        }
        inputContainer.addView(inputLabel)
        
        inputDraggableSwitch = Switch(context).apply {
            isChecked = false
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                text = if (isChecked) "ON" else "OFF"
                onInputDraggableChanged(isChecked)
            }
        }
        inputContainer.addView(inputDraggableSwitch)
        
        val outputContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        val outputLabel = TextView(context).apply {
            text = "Output Window Draggable:"
            textSize = 12f
        }
        outputContainer.addView(outputLabel)
        
        outputDraggableSwitch = Switch(context).apply {
            isChecked = true
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                text = if (isChecked) "ON" else "OFF"
                onOutputDraggableChanged(isChecked)
            }
        }
        outputContainer.addView(outputDraggableSwitch)
        
        row.addView(inputContainer)
        row.addView(outputContainer)
        parent.addView(row)
        
        // Widget draggable
    }
}
