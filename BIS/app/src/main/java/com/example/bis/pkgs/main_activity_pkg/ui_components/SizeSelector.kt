package com.example.bis.pkgs.main_activity_pkg.ui_components

import android.content.Context
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * UI component for input and output size selection
 * Responsibility: Create and manage size selector UI
 */
class SizeSelector(private val context: Context) {
    
    lateinit var sizeLabel: TextView
    lateinit var sizeSeekBar: SeekBar
    lateinit var outputSizeLabel: TextView
    lateinit var outputSizeSeekBar: SeekBar
    
    /**
     * Create input size selector
     */
    fun createInputSizeUI(
        parent: LinearLayout,
        onSizeChanged: (Int) -> Unit
    ) {
        sizeLabel = TextView(context).apply {
            text = "Input Size: 200px"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        parent.addView(sizeLabel)
        
        sizeSeekBar = SeekBar(context).apply {
            max = 400
            progress = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val size = progress + 100
                    sizeLabel.text = "Input Size: ${size}px"
                    onSizeChanged(size)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        parent.addView(sizeSeekBar)
    }
    
    /**
     * Create output size selector
     */
    fun createOutputSizeUI(
        parent: LinearLayout,
        onOutputSizeChanged: (Int) -> Unit
    ) {
        outputSizeLabel = TextView(context).apply {
            text = "Output Size: 500px"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        parent.addView(outputSizeLabel)
        
        outputSizeSeekBar = SeekBar(context).apply {
            max = 700
            progress = 200
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val size = progress + 300
                    outputSizeLabel.text = "Output Size: ${size}px"
                    onOutputSizeChanged(size)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        parent.addView(outputSizeSeekBar)
    }
}
