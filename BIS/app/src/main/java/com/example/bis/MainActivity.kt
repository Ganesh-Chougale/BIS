package com.example.bis

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var isServiceRunning = false
    private lateinit var toggleButton: Button
    private lateinit var mediaProjectionManager: MediaProjectionManager
    
    // Configuration UI
    private lateinit var shapeRadioGroup: RadioGroup
    private lateinit var squareRadio: RadioButton
    private lateinit var circleRadio: RadioButton
    private lateinit var sizeSeekBar: SeekBar
    private lateinit var sizeLabel: TextView
    
    private var selectedShape = MagnifierShape.SQUARE
    private var selectedSize = 200
    
    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Shape selector section
        val shapeLabel = TextView(this).apply {
            text = "Select Shape:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }
        layout.addView(shapeLabel)
        
        shapeRadioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            
            squareRadio = RadioButton(this@MainActivity).apply {
                id = View.generateViewId()
                text = "Square"
                isChecked = true
            }
            addView(squareRadio)
            
            circleRadio = RadioButton(this@MainActivity).apply {
                id = View.generateViewId()
                text = "Circle"
            }
            addView(circleRadio)
            
            setOnCheckedChangeListener { _, checkedId ->
                selectedShape = when (checkedId) {
                    circleRadio.id -> MagnifierShape.CIRCLE
                    else -> MagnifierShape.SQUARE
                }
            }
        }
        layout.addView(shapeRadioGroup)
        
        // Size selector section
        sizeLabel = TextView(this).apply {
            text = "Input Size: 200px"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        layout.addView(sizeLabel)
        
        sizeSeekBar = SeekBar(this).apply {
            max = 400  // 100 to 500 (we'll add 100 to the value)
            progress = 100  // Default 200px
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    selectedSize = progress + 100
                    sizeLabel.text = "Input Size: ${selectedSize}px"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        layout.addView(sizeSeekBar)
        
        // Toggle button
        toggleButton = Button(this).apply {
            text = "Start Magnifier"
            textSize = 18f
            setPadding(0, 32, 0, 0)
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    toggleService()
                } else {
                    requestOverlayPermission()
                }
            }
        }
        layout.addView(toggleButton)
        
        setContentView(layout)
    }
    
    private fun toggleService() {
        if (isServiceRunning) {
            stopService(Intent(this, OverlayService::class.java))
            toggleButton.text = "Start Magnifier"
            isServiceRunning = false
        } else {
            // Request screen capture permission
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            )
        }
    }
    
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(this)) {
            toggleButton.text = "Grant Permission First"
            isServiceRunning = false
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Log.d("MainActivity", "Got permission: resultCode=$resultCode (${Activity.RESULT_OK})")
                Toast.makeText(this, "Permission granted, initializing...", Toast.LENGTH_SHORT).show()
                
                // Store the capture data in service companion object
                OverlayService.setPendingCaptureData(data)
                
                // Send the capture command to the service with configuration
                val captureIntent = Intent(this, OverlayService::class.java).apply {
                    action = "START_CAPTURE"
                    // Pass Activity.RESULT_OK as the MediaProjection result code
                    putExtra("RESULT_CODE", Activity.RESULT_OK)
                    // Pass shape and size configuration
                    putExtra("SHAPE", selectedShape.name)
                    putExtra("INPUT_SIZE", selectedSize)
                }
                startService(captureIntent)
                
                isServiceRunning = true
                toggleButton.text = "Stop Magnifier"
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                toggleButton.text = "Start Magnifier"
            }
        }
    }
}
