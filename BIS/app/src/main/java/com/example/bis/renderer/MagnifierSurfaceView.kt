package com.example.bis.renderer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class MagnifierSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer: MagnifierRenderer

    init {
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2)

        renderer = MagnifierRenderer(context)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        // Render the view only when there is a change in the drawing data
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    /**
     * Passes a new bitmap to the renderer to be displayed.
     */
    fun updateBitmap(bitmap: Bitmap) {
        renderer.setBitmap(bitmap)
        requestRender() // Request a redraw
    }

    /**
     * Updates the shader program used by the renderer.
     */
    fun setShader(shaderName: String) {
        renderer.setShader(shaderName)
        requestRender()
    }
}
