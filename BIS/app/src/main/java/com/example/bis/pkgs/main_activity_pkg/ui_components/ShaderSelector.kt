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
        
        val shaderFiles = try { context.assets.list("shaders") ?: arrayOf() } catch (e: Exception) { arrayOf() }
        val shaders = arrayOf("Default 2xbr") + shaderFiles
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, shaders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        val shaderSpinner = Spinner(context).apply {
            this.adapter = adapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val shader = if (position == 0) "" else shaders[position]
                    onShaderChanged(shader)
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    onShaderChanged("")
                }
            }
        }
        parent.addView(shaderSpinner)
    }
}
