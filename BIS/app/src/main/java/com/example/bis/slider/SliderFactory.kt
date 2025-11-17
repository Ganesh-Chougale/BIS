package com.example.bis.slider

import android.content.Context
import android.view.WindowManager
import com.example.bis.slider.model.SliderConfig
import com.example.bis.slider.shape.CircleSlider
import com.example.bis.slider.shape.SquareSlider

object SliderFactory {
    fun createSlider(
        context: Context,
        config: SliderConfig,
        windowManager: WindowManager
    ): Slider {
        return when (config.shape) {
            SliderConfig.Shape.CIRCLE -> CircleSlider(context, config, windowManager)
            SliderConfig.Shape.SQUARE -> SquareSlider(context, config, windowManager)
        }
    }
    
    fun createSlider(
        context: Context,
        config: SliderConfig,
        windowManager: WindowManager,
        onZoomChanged: (Float) -> Unit
    ): Slider {
        return createSlider(context, config, windowManager).apply {
            setOnZoomChangeListener(onZoomChanged)
        }
    }
}