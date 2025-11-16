package com.example.bis.pkgs.main_activity_pkg.ui_components

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView

/**
 * UI component for color filter selection
 * Responsibility: Create and manage color filter toggle and options
 */
class ColorFilter(private val context: Context) {
    
    /**
     * Create color filter UI
     */
    fun createUI(
        parent: LinearLayout,
        onColorFilterChanged: (String) -> Unit
    ) {
        val colorFilterSwitch = Switch(context).apply {
            text = "Color Filter: OFF"
            textSize = 16f
            isChecked = false
        }
        parent.addView(colorFilterSwitch)
        
        val colorFilterOptionsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            id = View.generateViewId()
            setPadding(32, 8, 0, 0)
        }
        
        val filterTypeLabel = TextView(context).apply {
            text = "Select Filter Type:"
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }
        colorFilterOptionsContainer.addView(filterTypeLabel)
        
        val filterRadioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.VERTICAL
        }
        
        val inverseRadio = RadioButton(context).apply {
            id = View.generateViewId()
            text = "Inverse Colors"
            isChecked = true
        }
        filterRadioGroup.addView(inverseRadio)
        
        val monochromeRadio = RadioButton(context).apply {
            id = View.generateViewId()
            text = "Monochrome (Grayscale)"
        }
        filterRadioGroup.addView(monochromeRadio)
        
        filterRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                monochromeRadio.id -> "MONOCHROME"
                else -> "INVERSE"
            }
            onColorFilterChanged(mode)
        }
        
        colorFilterOptionsContainer.addView(filterRadioGroup)
        parent.addView(colorFilterOptionsContainer)
        
        colorFilterSwitch.setOnCheckedChangeListener { _, isChecked ->
            colorFilterSwitch.text = if (isChecked) "Color Filter: ON" else "Color Filter: OFF"
            colorFilterOptionsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            
            if (isChecked) {
                val mode = when (filterRadioGroup.checkedRadioButtonId) {
                    monochromeRadio.id -> "MONOCHROME"
                    else -> "INVERSE"
                }
                onColorFilterChanged(mode)
            } else {
                onColorFilterChanged("NORMAL")
            }
        }
    }
}
