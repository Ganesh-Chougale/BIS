package com.example.bis

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.bis.config.MagnifierShape
import com.example.bis.service.OverlayService

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
    private lateinit var outputSizeSeekBar: SeekBar
    private lateinit var outputSizeLabel: TextView
    private lateinit var inputDraggableSwitch: Switch
    private lateinit var outputDraggableSwitch: Switch
    private lateinit var crosshairSwitch: Switch
    private lateinit var zoomSliderSwitch: Switch
    private lateinit var minZoomSeekBar: SeekBar
    private lateinit var minZoomLabel: TextView
    private lateinit var maxZoomSeekBar: SeekBar
    private lateinit var maxZoomLabel: TextView
    private lateinit var zoomRangeContainer: LinearLayout
    
    private var selectedShape = MagnifierShape.SQUARE
    private var selectedSize = 200
    private var selectedOutputSize = 500
    private var isInputDraggable = false
    private var isOutputDraggable = true
    private var showCrosshair = false
    private var showZoomSlider = false
    private var minZoom = 1.5f
    private var maxZoom = 6.0f
    private var crosshairColor = Color.BLACK
    private var colorFilterMode = "NORMAL"  // NORMAL, INVERSE, or MONOCHROME
    
    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        // Create main content layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Wrap in ScrollView for landscape mode
        val scrollView = ScrollView(this).apply {
            addView(layout)
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
        
        // Output size selector section
        outputSizeLabel = TextView(this).apply {
            text = "Output Size: 500px"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        layout.addView(outputSizeLabel)
        
        outputSizeSeekBar = SeekBar(this).apply {
            max = 700  // 300 to 1000 (we'll add 300 to the value)
            progress = 200  // Default 500px
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    selectedOutputSize = progress + 300
                    outputSizeLabel.text = "Output Size: ${selectedOutputSize}px"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        layout.addView(outputSizeSeekBar)
        
        // Input window draggable toggle
        val inputDraggableLabel = TextView(this).apply {
            text = "Input Window Draggable:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        layout.addView(inputDraggableLabel)
        
        inputDraggableSwitch = Switch(this).apply {
            isChecked = false  // Default OFF
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                isInputDraggable = isChecked
                text = if (isChecked) "ON" else "OFF"
            }
        }
        layout.addView(inputDraggableSwitch)
        
        // Output window draggable toggle
        val outputDraggableLabel = TextView(this).apply {
            text = "Output Window Draggable:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        layout.addView(outputDraggableLabel)
        
        outputDraggableSwitch = Switch(this).apply {
            isChecked = true  // Default ON
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                isOutputDraggable = isChecked
                text = if (isChecked) "ON" else "OFF"
            }
        }
        layout.addView(outputDraggableSwitch)
        
        // Crosshair toggle
        val crosshairLabel = TextView(this).apply {
            text = "Show Crosshair in Input:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        layout.addView(crosshairLabel)
        
        crosshairSwitch = Switch(this).apply {
            isChecked = false  // Default OFF
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                showCrosshair = isChecked
                text = if (isChecked) "ON" else "OFF"
            }
        }
        layout.addView(crosshairSwitch)
        
        // Zoom slider toggle
        val zoomSliderLabel = TextView(this).apply {
            text = "Show Zoom Slider:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        layout.addView(zoomSliderLabel)
        
        zoomSliderSwitch = Switch(this).apply {
            isChecked = false  // Default OFF
            text = if (isChecked) "ON" else "OFF"
            setOnCheckedChangeListener { _, isChecked ->
                showZoomSlider = isChecked
                text = if (isChecked) "ON" else "OFF"
                // Show/hide zoom range controls
                zoomRangeContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }
        layout.addView(zoomSliderSwitch)
        
        // Zoom range container (initially hidden)
        zoomRangeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE  // Hidden by default
        }
        
        // Minimum zoom selector
        minZoomLabel = TextView(this).apply {
            text = "Minimum Zoom: 1.5x"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        zoomRangeContainer.addView(minZoomLabel)
        
        minZoomSeekBar = SeekBar(this).apply {
            max = 20  // 1.0x to 3.0x in 0.1 steps (20 steps)
            progress = 5  // Default 1.5x (1.0 + 0.5 = 1.5)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    minZoom = 1.0f + (progress * 0.1f)
                    minZoomLabel.text = String.format("Minimum Zoom: %.1fx", minZoom)
                    // Ensure min is always less than max
                    if (minZoom >= maxZoom) {
                        maxZoom = minZoom + 0.5f
                        maxZoomSeekBar.progress = ((maxZoom - 3.0f) / 0.1f).toInt()
                        maxZoomLabel.text = String.format("Maximum Zoom: %.1fx", maxZoom)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        zoomRangeContainer.addView(minZoomSeekBar)
        
        // Maximum zoom selector
        maxZoomLabel = TextView(this).apply {
            text = "Maximum Zoom: 6.0x"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        zoomRangeContainer.addView(maxZoomLabel)
        
        maxZoomSeekBar = SeekBar(this).apply {
            max = 70  // 3.0x to 10.0x in 0.1 steps (70 steps)
            progress = 30  // Default 6.0x (3.0 + 3.0 = 6.0)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    maxZoom = 3.0f + (progress * 0.1f)
                    maxZoomLabel.text = String.format("Maximum Zoom: %.1fx", maxZoom)
                    // Ensure max is always greater than min
                    if (maxZoom <= minZoom) {
                        minZoom = maxZoom - 0.5f
                        minZoomSeekBar.progress = ((minZoom - 1.0f) / 0.1f).toInt()
                        minZoomLabel.text = String.format("Minimum Zoom: %.1fx", minZoom)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        zoomRangeContainer.addView(maxZoomSeekBar)
        
        // Add zoom range container to main layout
        layout.addView(zoomRangeContainer)
        
        // Color Filter Section
        val colorFilterLabel = TextView(this).apply {
            text = "Output Color Filter:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        layout.addView(colorFilterLabel)
        
        // Color filter enable/disable switch
        val colorFilterSwitch = Switch(this).apply {
            text = "Color Filter: OFF"
            isChecked = false
            setOnCheckedChangeListener { _, isChecked ->
                text = if (isChecked) "Color Filter: ON" else "Color Filter: OFF"
                // Show/hide filter options
                findViewById<LinearLayout>(View.generateViewId())?.apply {
                    visibility = if (isChecked) View.VISIBLE else View.GONE
                }
                // Reset to normal if disabled
                if (!isChecked) {
                    colorFilterMode = "NORMAL"
                }
            }
        }
        layout.addView(colorFilterSwitch)
        
        // Color filter options container (initially hidden)
        val colorFilterOptionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            id = View.generateViewId()
            setPadding(32, 8, 0, 0)
        }
        
        val filterTypeLabel = TextView(this).apply {
            text = "Select Filter Type:"
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }
        colorFilterOptionsContainer.addView(filterTypeLabel)
        
        // Radio group for filter types
        val filterRadioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        
        val inverseRadio = RadioButton(this).apply {
            id = View.generateViewId()
            text = "Inverse Colors"
            isChecked = true  // Default selection
        }
        filterRadioGroup.addView(inverseRadio)
        
        val monochromeRadio = RadioButton(this).apply {
            id = View.generateViewId()
            text = "Monochrome (Grayscale)"
        }
        filterRadioGroup.addView(monochromeRadio)
        
        // Handle radio button selection
        filterRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            colorFilterMode = when (checkedId) {
                monochromeRadio.id -> "MONOCHROME"
                else -> "INVERSE"
            }
        }
        
        colorFilterOptionsContainer.addView(filterRadioGroup)
        layout.addView(colorFilterOptionsContainer)
        
        // Update switch listener to show/hide options
        colorFilterSwitch.setOnCheckedChangeListener { _, isChecked ->
            colorFilterSwitch.text = if (isChecked) "Color Filter: ON" else "Color Filter: OFF"
            colorFilterOptionsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            
            if (isChecked) {
                // Set to selected filter type
                colorFilterMode = when (filterRadioGroup.checkedRadioButtonId) {
                    monochromeRadio.id -> "MONOCHROME"
                    else -> "INVERSE"
                }
            } else {
                // Reset to normal
                colorFilterMode = "NORMAL"
            }
        }
        
        // Toggle button
        toggleButton = Button(this).apply {
            text = "Start Magnifier"
            textSize = 18f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#4CAF50")) // Green
            setTextColor(Color.WHITE)
            setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 0)
            }
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    toggleService()
                } else {
                    requestOverlayPermission()
                }
            }
        }
        layout.addView(toggleButton)
        
        // About button
        val aboutButton = Button(this).apply {
            text = "About"
            textSize = 16f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#2196F3")) // Blue
            setTextColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            setOnClickListener {
                startActivity(Intent(this@MainActivity, AboutActivity::class.java))
            }
        }
        layout.addView(aboutButton)
        
        // Exit App button
        val exitButton = Button(this).apply {
            text = "Exit App"
            textSize = 16f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F44336")) // Red
            setTextColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 32)
            }
            setOnClickListener {
                // Stop the service if running
                if (isServiceRunning) {
                    stopService(Intent(this@MainActivity, OverlayService::class.java))
                }
                // Close the app
                finishAffinity()
            }
        }
        layout.addView(exitButton)
        
        setContentView(scrollView)
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
                    putExtra("OUTPUT_SIZE", selectedOutputSize)
                    putExtra("INPUT_DRAGGABLE", isInputDraggable)
                    putExtra("OUTPUT_DRAGGABLE", isOutputDraggable)
                    putExtra("SHOW_CROSSHAIR", showCrosshair)
                    putExtra("CROSSHAIR_COLOR", crosshairColor)
                    putExtra("COLOR_FILTER_MODE", colorFilterMode)
                    putExtra("SHOW_ZOOM_SLIDER", showZoomSlider)
                    putExtra("MIN_ZOOM", minZoom)
                    putExtra("MAX_ZOOM", maxZoom)
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
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
