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
    
    /**
     * Create crosshair toggle UI
     */
    fun createUI(
        parent: LinearLayout,
        onCrosshairChanged: (Boolean) -> Unit,
        onOutputCrosshairChanged: (Boolean) -> Unit
    ) {
        val crosshairLabel = TextView(context).apply {
            text = "Show Crosshair in Input:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        parent.addView(crosshairLabel)
        
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
        parent.addView(crosshairSwitch)
        parent.addView(outputCrosshairContainer)
    }
}
