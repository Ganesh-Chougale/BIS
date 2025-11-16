package com.example.bis.pkgs.main_activity_pkg.ui_components

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.example.bis.config.MagnifierShape

/**
 * UI component for shape selection (Square/Circle)
 * Responsibility: Create and manage shape selector UI
 */
class ShapeSelector(private val context: Context) {
    
    lateinit var shapeRadioGroup: RadioGroup
    lateinit var squareRadio: RadioButton
    lateinit var circleRadio: RadioButton
    
    /**
     * Create the shape selector UI and add to parent layout
     */
    fun createUI(
        parent: LinearLayout,
        onShapeChanged: (MagnifierShape) -> Unit
    ) {
        val shapeLabel = TextView(context).apply {
            text = "Select Shape:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }
        parent.addView(shapeLabel)
        
        shapeRadioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.HORIZONTAL
            
            squareRadio = RadioButton(context).apply {
                id = View.generateViewId()
                text = "Square"
                isChecked = true
            }
            addView(squareRadio)
            
            circleRadio = RadioButton(context).apply {
                id = View.generateViewId()
                text = "Circle"
            }
            addView(circleRadio)
            
            setOnCheckedChangeListener { _, checkedId ->
                val shape = when (checkedId) {
                    circleRadio.id -> MagnifierShape.CIRCLE
                    else -> MagnifierShape.SQUARE
                }
                onShapeChanged(shape)
            }
        }
        parent.addView(shapeRadioGroup)
    }
}
