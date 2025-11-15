package com.example.bis.renderer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MagnifierRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private val TAG = "MagnifierRenderer"

    // Geometry of a simple square
    private val vertices = floatArrayOf(
        -1.0f, -1.0f,  // Bottom Left
         1.0f, -1.0f,  // Bottom Right
        -1.0f,  1.0f,  // Top Left
         1.0f,  1.0f   // Top Right
    )

    private val textureCoords = floatArrayOf(
        0.0f, 1.0f,   // Bottom Left (flipped Y)
        1.0f, 1.0f,   // Bottom Right (flipped Y)
        0.0f, 0.0f,   // Top Left (flipped Y)
        1.0f, 0.0f    // Top Right (flipped Y)
    )

    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer

    private var program: Int = 0
    private var textureId: Int = 0
    private var currentBitmap: Bitmap? = null
    @Volatile private var bitmapToUpdate: Bitmap? = null
    private val lock = Any()
    private var shaderToUpdate: String? = null

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        textureBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        textureBuffer.put(textureCoords).position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated called")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f) // Transparent background
        setupTexture()
        Log.d(TAG, "Texture setup complete. Texture ID: $textureId")
        // Load a default shader to ensure program is never 0
        try {
            program = ShaderHelper.loadShader(context, "2xbr.shader")
            Log.d(TAG, "Default shader (2xbr) loaded. Program ID: $program")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load default shader", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width x $height")
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Handle shader update
        shaderToUpdate?.let {
            Log.d(TAG, "Loading shader: $it")
            program = ShaderHelper.loadShader(context, it)
            Log.d(TAG, "Shader loaded. Program ID: $program")
            shaderToUpdate = null
        }

        // Handle bitmap update safely on the GL thread
        synchronized(lock) {
            bitmapToUpdate?.let {
                updateTexture(it)
                bitmapToUpdate = null
            }
        }

        if (program == 0 || currentBitmap == null) {
            // Clear the screen if we have nothing to draw
            Log.w(TAG, "Cannot draw: program=$program, bitmap=${currentBitmap != null}")
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            return
        }
        Log.d(TAG, "Drawing frame: program=$program, bitmap=${currentBitmap?.width}x${currentBitmap?.height}")

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // Get handle to vertex shader's vPosition member
        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        // Get handle to texture coordinate member
        val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)

        // Get handle to fragment shader's rubyTexture member
        val textureHandle = GLES20.glGetUniformLocation(program, "rubyTexture")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)
        Log.d(TAG, "Drawing frame with program $program and texture ID $textureId")

        // Pass texture size to the shader
        val textureSizeHandle = GLES20.glGetUniformLocation(program, "rubyTextureSize")
        currentBitmap?.let {
            GLES20.glUniform2f(textureSizeHandle, it.width.toFloat(), it.height.toFloat())
        }

        // Draw the square
        Log.d(TAG, "Calling glDrawArrays")
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        // Check for OpenGL errors
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL error after draw: $error")
        }

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun setupTexture() {
        Log.d(TAG, "Setting up texture")
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        Log.d(TAG, "Generated texture ID: $textureId")
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        Log.d(TAG, "Texture setup complete")
    }

    private fun updateTexture(bitmap: Bitmap) {
        if (bitmap.isRecycled) {
            Log.w(TAG, "Attempted to update texture with a recycled bitmap.")
            return
        }
        Log.d(TAG, "Updating texture with bitmap: ${bitmap.width}x${bitmap.height}")
        currentBitmap = bitmap
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL error during texture update: $error")
        } else {
            Log.d(TAG, "Texture updated successfully")
        }
    }

    fun setBitmap(bitmap: Bitmap) {
        synchronized(lock) {
            bitmapToUpdate?.recycle()
            bitmapToUpdate = bitmap
        }
    }

    fun setShader(shaderName: String) {
        Log.d(TAG, "setShader called with: $shaderName")
        if (shaderName.isEmpty()) {
            // Handle 'None' case by cleaning up the program
            Log.d(TAG, "Clearing shader")
            GLES20.glDeleteProgram(program)
            program = 0
        } else {
            Log.d(TAG, "Queuing shader for loading: $shaderName")
            shaderToUpdate = shaderName
        }
    }
}
