package com.example.bis.pkgs.main_activity_pkg

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import com.example.bis.config.MagnifierShape
import com.example.bis.pkgs.main_activity_pkg.ui_components.*

/**
 * Orchestrates all UI components for MainActivity
 * Responsibility: Coordinate and assemble UI component classes
 */
class UI_MainActivity(private val context: Context) {
    
    // UI Component instances
    private lateinit var shapeSelector: ShapeSelector
    private lateinit var sizeSelector: SizeSelector
    private lateinit var draggableToggles: DraggableToggles
    private lateinit var crosshairToggles: CrosshairToggles
    private lateinit var zoomSlider: ZoomSlider
    private lateinit var colorFilter: ColorFilter
    private lateinit var shaderSelector: ShaderSelector
    private lateinit var buttonPanel: ButtonPanel
    
    // Expose toggle button for external access
    val toggleButton: android.widget.Button
        get() = buttonPanel.toggleButton
    
    /**
     * Creates the main UI layout by assembling all UI components
     */
    fun createMainLayout(
        onShapeChanged: (MagnifierShape) -> Unit,
        onSizeChanged: (Int) -> Unit,
        onOutputSizeChanged: (Int) -> Unit,
        onInputDraggableChanged: (Boolean) -> Unit,
        onOutputDraggableChanged: (Boolean) -> Unit,
        onWidgetDraggableChanged: (Boolean) -> Unit,
        onCrosshairChanged: (Boolean) -> Unit,
        onOutputCrosshairChanged: (Boolean) -> Unit,
        onZoomSliderChanged: (Boolean) -> Unit,
        onMinZoomChanged: (Float) -> Unit,
        onMaxZoomChanged: (Float) -> Unit,
        onColorFilterChanged: (String) -> Unit,
        onShaderChanged: (String) -> Unit,
        onToggleClicked: () -> Unit,
        onAboutClicked: () -> Unit,
        onExitClicked: () -> Unit
    ): ScrollView {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Initialize all UI components
        shapeSelector = ShapeSelector(context)
        sizeSelector = SizeSelector(context)
        draggableToggles = DraggableToggles(context)
        crosshairToggles = CrosshairToggles(context)
        zoomSlider = ZoomSlider(context)
        colorFilter = ColorFilter(context)
        shaderSelector = ShaderSelector(context)
        buttonPanel = ButtonPanel(context)
        
        // Assemble UI components in order
        shapeSelector.createUI(layout, onShapeChanged)
        sizeSelector.createInputSizeUI(layout, onSizeChanged)
        sizeSelector.createOutputSizeUI(layout, onOutputSizeChanged)
        draggableToggles.createUI(layout, onInputDraggableChanged, onOutputDraggableChanged, onWidgetDraggableChanged)
        crosshairToggles.createUI(layout, onCrosshairChanged, onOutputCrosshairChanged)
        zoomSlider.createUI(layout, onZoomSliderChanged, onMinZoomChanged, onMaxZoomChanged)
        colorFilter.createUI(layout, onColorFilterChanged)
        shaderSelector.createUI(layout, onShaderChanged)
        buttonPanel.createUI(layout, onToggleClicked, onAboutClicked, onExitClicked)
        
        return ScrollView(context).apply {
            addView(layout)
        }
    }
}
