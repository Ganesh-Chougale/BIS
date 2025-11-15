package com.example.bis.renderer

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader

object ShaderHelper {

    private const val TAG = "ShaderHelper"

    /**
     * Loads, parses, and compiles a shader from the assets folder.
     * @param context The application context.
     * @param fileName The name of the .shader file in the 'assets/shaders' directory.
     * @return The OpenGL program ID, or 0 if loading failed.
     */
    fun loadShader(context: Context, fileName: String): Int {
        val shaderSources = parseShaderFile(context, "shaders/$fileName")
        val vertexShaderSource = shaderSources["vertex"]
        val fragmentShaderSource = shaderSources["fragment"]

        if (vertexShaderSource == null || fragmentShaderSource == null) {
            Log.e(TAG, "Failed to parse shader file: $fileName")
            return 0
        }

        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)

        if (vertexShader == 0 || fragmentShader == 0) {
            return 0
        }

        return linkProgram(vertexShader, fragmentShader)
    }

    private fun parseShaderFile(context: Context, filePath: String): Map<String, String> {
        val sources = mutableMapOf<String, String>()
        try {
            context.assets.open(filePath).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(reader)

                    var eventType = parser.eventType
                    var currentTag = ""
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                currentTag = parser.name
                            }
                            XmlPullParser.TEXT -> {
                                if (currentTag.equals("vertex", ignoreCase = true) || currentTag.equals("fragment", ignoreCase = true)) {
                                    if (!parser.text.trim().isEmpty()) {
                                        sources[currentTag] = parser.text
                                    }
                                }
                            }
                            XmlPullParser.CDSECT -> {
                                if (currentTag == "vertex" || currentTag == "fragment") {
                                    sources[currentTag] = parser.text
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing shader file: $filePath", e) // Log the full exception
        }
        return sources
    }

    private fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "Could not create new shader.")
            return 0
        }

        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }

    private fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
        val program = GLES20.glCreateProgram()
        if (program == 0) {
            Log.e(TAG, "Could not create new program.")
            return 0
        }

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking program: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            return 0
        }

        return program
    }
}
