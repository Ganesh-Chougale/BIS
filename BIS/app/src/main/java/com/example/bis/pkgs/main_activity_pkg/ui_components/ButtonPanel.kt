package com.example.bis.pkgs.main_activity_pkg.ui_components

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout

/**
 * UI component for button panel (Toggle, About, Exit)
 * Responsibility: Create and manage action buttons
 */
class ButtonPanel(private val context: Context) {
    
    lateinit var toggleButton: Button
    
    /**
     * Create button panel UI
     */
    fun createUI(
        parent: LinearLayout,
        onToggleClicked: () -> Unit,
        onAboutClicked: () -> Unit,
        onExitClicked: () -> Unit
    ) {
        // Toggle button
        toggleButton = Button(context).apply {
            text = "Start Magnifier"
            textSize = 18f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 0)
            }
            setOnClickListener { onToggleClicked() }
        }
        parent.addView(toggleButton)
        
        // About button
        val aboutButton = Button(context).apply {
            text = "About"
            textSize = 16f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            setOnClickListener { onAboutClicked() }
        }
        parent.addView(aboutButton)
        
        // Exit button
        val exitButton = Button(context).apply {
            text = "Exit App"
            textSize = 16f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F44336"))
            setTextColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 32)
            }
            setOnClickListener { onExitClicked() }
        }
        parent.addView(exitButton)
    }
}
