package com.example.bis.config

import android.graphics.Point
import android.graphics.Color

/**
 * Shape type for input and output areas
 */
enum class MagnifierShape {
    SQUARE,
    CIRCLE
}

/**
 * Centralized configuration for the magnifier app.
 * Holds all settings for input area, output window, zoom, and state.
 */
data class MagnifierConfig(
    // Shape configuration
    var shape: MagnifierShape = MagnifierShape.SQUARE,
    
    // Input selector (area to magnify)
    var inputSize: Int = 200,  // Size of the input selection square/circle diameter
    var inputX: Int = 0,  // Will be set to screen center in OverlayService
    var inputY: Int = 0,  // Will be set to screen center in OverlayService
    
    // Output window (magnified view)
    var outputSize: Int = 500,
    var outputPosition: Point = Point(0, 0),  // Will be set in OverlayService
    
    // Zoom settings (default 2.5x, min 1.5x, max 10.0x)
    var zoomFactor: Float = 2.5f,  // Default zoom, must be >= minZoom (1.5x)
    var minZoom: Float = 1.5f,
    var maxZoom: Float = 10.0f,
    
    // Toggle widget position
    var togglePosition: Point = Point(50, 50),
    
    // State
    var isMagnifying: Boolean = true,
    
    // Draggable flags
    var isInputDraggable: Boolean = false,
    var isOutputDraggable: Boolean = true,
    
    // Display flags
    var showCrosshair: Boolean = false,
    var crosshairColor: Int = Color.BLACK,  // Default black
    var colorFilterMode: String = "NORMAL",  // NORMAL, INVERSE, or MONOCHROME
    
    // Screen properties (set at runtime)
    var screenWidth: Int = 0,
    var screenHeight: Int = 0,
    var screenDensity: Int = 0
) {
    // Backward compatibility property
    var inputPosition: Point
        get() = Point(inputX, inputY)
        set(value) {
            inputX = value.x
            inputY = value.y
        }

    /**
     * Calculate the actual output size based on input size and zoom factor
     */
    fun getCalculatedOutputSize(): Int {
        return (inputSize * zoomFactor).toInt()
    }
    
    /**
     * Update zoom factor within allowed range
     */
    fun setZoom(newZoom: Float) {
        zoomFactor = newZoom.coerceIn(minZoom, maxZoom)
    }
    
    /**
     * Increase zoom by step
     */
    fun increaseZoom(step: Float = 0.5f) {
        setZoom(zoomFactor + step)
    }
    
    /**
     * Decrease zoom by step
     */
    fun decreaseZoom(step: Float = 0.5f) {
        setZoom(zoomFactor - step)
    }
    
    /**
     * Reset to default values
     */
    fun reset() {
        inputSize = 200
        outputSize = 500
        zoomFactor = 2.5f
        isMagnifying = true
    }
}