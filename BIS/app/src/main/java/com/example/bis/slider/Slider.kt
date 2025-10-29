package com.example.bis.slider

import android.view.WindowManager
import com.example.bis.slider.model.SliderConfig

interface Slider {
    fun show()
    fun hide()
    fun remove()
    fun updatePosition()
    fun updateConfig(newConfig: SliderConfig)
    fun setOnZoomChangeListener(listener: (Float) -> Unit)
    fun setVisibility(visible: Boolean)
    fun isAttached(): Boolean
    
    companion object {
        const val DEFAULT_MIN_ZOOM = 1.0f
        const val DEFAULT_MAX_ZOOM = 5.0f
        const val DEFAULT_INITIAL_ZOOM = 1.0f
    }
}