package com.example.bis.pkgs.main_activity_pkg.ui_components

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

/**
 * UI component for shader selection
 * Responsibility: Create and manage shader selector UI
 */
class ShaderSelector(private val context: Context) {
    
    /**
     * Create shader selector UI
     */
    fun createUI(
        parent: LinearLayout,
        onShaderChanged: (String) -> Unit
    ) {
        val shaderLabel = TextView(context).apply {
            text = "Select Shader:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        parent.addView(shaderLabel)
        
        // Load shader files from assets, but hide the raw 2xbr.shader entry
        // so we only expose a single user-friendly "Default 2xbr" option.
        val shaderFiles = try {
            (context.assets.list("shaders") ?: arrayOf())
                .filterNot { it.equals("2xbr.shader", ignoreCase = true) }
                .toTypedArray()
        } catch (e: Exception) {
            arrayOf()
        }
        val shaders = arrayOf("Default 2xbr") + shaderFiles
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, shaders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        val shaderSpinner = Spinner(context).apply {
            this.adapter = adapter
            val density = context.resources.displayMetrics.density
            dropDownVerticalOffset = (8 * density).toInt()

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // Position 0 is the friendly "Default 2xbr" entry that
                    // explicitly maps to the 2xbr.shader file. Other entries
                    // map directly to their corresponding asset filenames.
                    val shader = if (position == 0) "2xbr.shader" else shaders[position]
                    onShaderChanged(shader)
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Fallback to the default 2xbr shader
                    onShaderChanged("2xbr.shader")
                }
            }
        }
        parent.addView(shaderSpinner)
    }
}
