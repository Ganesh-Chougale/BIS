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
    
    // Shape rendering
    private var isCircular = false
    
    // Geometry of a simple square
    private val squareVertices = floatArrayOf(
        -1.0f, -1.0f,  // Bottom Left
         1.0f, -1.0f,  // Bottom Right
        -1.0f,  1.0f,  // Top Left
         1.0f,  1.0f   // Top Right
    )
    
    // Geometry for a circle (approximated with triangles)
    private val circleVertices: FloatArray by lazy {
        generateCircleVertices(64)
    }
    
    private val textureCoords = floatArrayOf(
        0.0f, 1.0f,   // Bottom Left (flipped Y)
        1.0f, 1.0f,   // Bottom Right (flipped Y)
        0.0f, 0.0f,   // Top Left (flipped Y)
        1.0f, 0.0f    // Top Right (flipped Y)
    )
    
    // Texture coordinates for circle (radial mapping)
    private val circleTextureCoords: FloatArray by lazy {
        generateCircleTextureCoords(64)
    }

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var textureBuffer: FloatBuffer

    private var program: Int = 0
    private var textureId: Int = 0
    private var currentBitmap: Bitmap? = null
    @Volatile private var bitmapToUpdate: Bitmap? = null
    private val lock = Any()
    private var shaderToUpdate: String? = null

    init {
        updateGeometry()
    }
    
    /**
     * Generate vertices for a circle approximation
     */
    private fun generateCircleVertices(segments: Int): FloatArray {
        val vertices = mutableListOf<Float>()
        // Center point
        vertices.add(0.0f)
        vertices.add(0.0f)
        
        // Generate points around the circle
        for (i in 0..segments) {
            val angle = 2 * Math.PI * i / segments
            vertices.add(Math.cos(angle).toFloat())
            vertices.add(Math.sin(angle).toFloat())
        }
        
        return vertices.toFloatArray()
    }
    
    /**
     * Generate texture coordinates for circular mapping
     */
    private fun generateCircleTextureCoords(segments: Int): FloatArray {
        val coords = mutableListOf<Float>()
        // Center texture coordinate
        coords.add(0.5f)
        coords.add(0.5f)
        
        // Generate texture coordinates around the circle
        for (i in 0..segments) {
            val angle = 2 * Math.PI * i / segments
            // Map circle to texture coordinates (0,0 to 1,1)
            val u = (Math.cos(angle) * 0.5f + 0.5f).toFloat()
            val v = (Math.sin(angle) * 0.5f + 0.5f).toFloat()
            coords.add(u)
            coords.add(v)
        }
        
        return coords.toFloatArray()
    }
    
    /**
     * Update vertex and texture buffers based on current shape
     */
    private fun updateGeometry() {
        val vertices = if (isCircular) circleVertices else squareVertices
        val texCoords = if (isCircular) circleTextureCoords else textureCoords
        
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        textureBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        textureBuffer.put(texCoords).position(0)
    }
    
    /**
     * Set the shape for rendering
     */
    fun setShape(isCircular: Boolean) {
        this.isCircular = isCircular
        updateGeometry()
    }
    
    /**
     * Get the current shape
     */
    fun isCircular(): Boolean = isCircular

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated called")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f) // Transparent background
        setupTexture()
        Log.d(TAG, "Texture setup complete. Texture ID: $textureId")
        // Load a default shader to ensure program is never 0
        try {
            program = ShaderHelper.loadShader(context, "2xbr.shader")
            if (program == 0) {
                Log.w(TAG, "Default shader failed, using fallback shader")
                program = createFallbackProgram()
            } else {
                Log.d(TAG, "Default shader (2xbr) loaded. Program ID: $program")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load default shader, using fallback", e)
            program = createFallbackProgram()
        }

        // Upload a small placeholder bitmap so we always draw something
        try {
            val placeholder = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
            placeholder.eraseColor(0xFFFF00FF.toInt()) // Magenta placeholder
            updateTexture(placeholder)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create placeholder bitmap", e)
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
            val newProgram = if (it == "__FALLBACK__") {
                createFallbackProgram()
            } else {
                ShaderHelper.loadShader(context, it)
            }

            if (newProgram != 0) {
                if (program != 0) {
                    GLES20.glDeleteProgram(program)
                }
                program = newProgram
                Log.d(TAG, "Shader loaded. Program ID: $program")
            } else {
                Log.e(TAG, "Failed to load shader '$it', keeping existing program $program")
            }
            shaderToUpdate = null
        }

        // Handle bitmap update safely on the GL thread
        synchronized(lock) {
            bitmapToUpdate?.let {
                updateTexture(it)
                bitmapToUpdate = null
            }
        }

        if (program == 0) {
            // Clear the screen if we have no valid GL program
            Log.w(TAG, "Cannot draw: program=$program")
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            return
        }
        Log.d(TAG, "Drawing frame: program=$program, bitmapPresent=${currentBitmap != null}")

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

        // Pass texture size and input/output sizes to the shader
        val textureSizeHandle = GLES20.glGetUniformLocation(program, "rubyTextureSize")
        val inputSizeHandle = GLES20.glGetUniformLocation(program, "rubyInputSize")
        val outputSizeHandle = GLES20.glGetUniformLocation(program, "rubyOutputSize")
        
        currentBitmap?.let {
            GLES20.glUniform2f(textureSizeHandle, it.width.toFloat(), it.height.toFloat())
            GLES20.glUniform2f(inputSizeHandle, it.width.toFloat(), it.height.toFloat())
            GLES20.glUniform2f(outputSizeHandle, it.width.toFloat(), it.height.toFloat())
        }

        // Draw the shape
        Log.d(TAG, "Calling glDrawArrays - isCircular: $isCircular")
        if (isCircular) {
            // Draw circle as triangle fan
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, circleVertices.size / 2)
        } else {
            // Draw square as triangle strip
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }
        
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
            // Handle 'None' case by reverting to fallback shader
            Log.d(TAG, "Clearing shader (using fallback)")
            shaderToUpdate = "__FALLBACK__"
        } else {
            Log.d(TAG, "Queuing shader for loading: $shaderName")
            shaderToUpdate = shaderName
        }
    }

    /**
     * Create a simple fallback shader program that just draws the texture.
     */
    private fun createFallbackProgram(): Int {
        val vertexShaderSource = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;

            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        val fragmentShaderSource = """
            precision mediump float;
            varying vec2 vTexCoord;

            void main() {
                // Solid magenta fallback so we can visually debug rendering
                gl_FragColor = vec4(1.0, 0.0, 1.0, 1.0);
            }
        """.trimIndent()

        fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            if (shader == 0) {
                Log.e(TAG, "Could not create shader of type $type")
                return 0
            }
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader $type: ${GLES20.glGetShaderInfoLog(shader)}")
                GLES20.glDeleteShader(shader)
                return 0
            }
            return shader
        }

        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
        if (vertexShader == 0 || fragmentShader == 0) {
            return 0
        }

        val programId = GLES20.glCreateProgram()
        if (programId == 0) {
            Log.e(TAG, "Could not create GL program")
            return 0
        }

        GLES20.glAttachShader(programId, vertexShader)
        GLES20.glAttachShader(programId, fragmentShader)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Could not link program: ${GLES20.glGetProgramInfoLog(programId)}")
            GLES20.glDeleteProgram(programId)
            return 0
        }

        return programId
    }
}
