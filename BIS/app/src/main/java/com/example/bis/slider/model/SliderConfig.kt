package com.example.bis.slider.model

data class SliderConfig(
    val windowWidth: Int,
    val windowHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val windowX: Int,
    val windowY: Int,
    val minZoom: Float = 1.0f,
    val maxZoom: Float = 5.0f,
    val initialZoom: Float = 1.0f,
    val shape: Shape = Shape.SQUARE
) {
    enum class Shape {
        SQUARE,
        CIRCLE
    }
    
    companion object {
        const val DEFAULT_MIN_ZOOM = 1.0f
        const val DEFAULT_MAX_ZOOM = 5.0f
        const val DEFAULT_INITIAL_ZOOM = 1.0f
    }
}