package com.example.bis.pkgs.main_activity_pkg.ui_components

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * UI component for zoom slider controls
 * Responsibility: Create and manage zoom range selector UI
 */
class ZoomSlider(private val context: Context) {
    
    lateinit var minZoomLabel: TextView
    lateinit var minZoomSeekBar: SeekBar
    lateinit var maxZoomLabel: TextView
    lateinit var maxZoomSeekBar: SeekBar
    lateinit var zoomRangeContainer: LinearLayout
    
    /**
     * Create zoom slider UI
     */
    fun createUI(
        parent: LinearLayout,
        onMinZoomChanged: (Float) -> Unit,
        onMaxZoomChanged: (Float) -> Unit
    ) {
        zoomRangeContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.VISIBLE
        }
        
        // Min zoom
        minZoomLabel = TextView(context).apply {
            text = "Minimum Zoom: 1.5x"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        zoomRangeContainer.addView(minZoomLabel)
        
        minZoomSeekBar = SeekBar(context).apply {
            max = 20
            progress = 5
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val minZoom = 1.0f + (progress * 0.1f)
                    minZoomLabel.text = String.format("Minimum Zoom: %.1fx", minZoom)
                    onMinZoomChanged(minZoom)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        zoomRangeContainer.addView(minZoomSeekBar)
        
        // Max zoom
        maxZoomLabel = TextView(context).apply {
            text = "Maximum Zoom: 6.0x"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        zoomRangeContainer.addView(maxZoomLabel)
        
        maxZoomSeekBar = SeekBar(context).apply {
            max = 70
            progress = 30
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val maxZoom = 3.0f + (progress * 0.1f)
                    maxZoomLabel.text = String.format("Maximum Zoom: %.1fx", maxZoom)
                    onMaxZoomChanged(maxZoom)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        zoomRangeContainer.addView(maxZoomSeekBar)
        
        parent.addView(zoomRangeContainer)
    }
}
