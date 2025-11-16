package com.example.bis

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bis.config.MagnifierShape
import com.example.bis.pkgs.main_activity_pkg.ButtonHandle_MainActivity
import com.example.bis.pkgs.main_activity_pkg.Permission_MainActivity
import com.example.bis.pkgs.main_activity_pkg.Service_MainActivity
import com.example.bis.pkgs.main_activity_pkg.UI_MainActivity
import com.example.bis.service.OverlayService

class MainActivity : AppCompatActivity() {
    
    // Helper classes for SRP
    private lateinit var uiManager: UI_MainActivity
    private lateinit var permissionManager: Permission_MainActivity
    private lateinit var serviceManager: Service_MainActivity
    private lateinit var buttonHandler: ButtonHandle_MainActivity
    
    // Configuration state
    private var selectedShape = MagnifierShape.SQUARE
    private var selectedSize = 200
    private var selectedOutputSize = 500
    private var isInputDraggable = false
    private var isOutputDraggable = true
    private var isWidgetDraggable = false
    private var showCrosshair = false
    private var showOutputCrosshair = false
    private var showZoomSlider = false
    private var minZoom = 1.5f
    private var maxZoom = 6.0f
    private var crosshairColor = Color.BLACK
    private var colorFilterMode = "NORMAL"
    private var selectedShader = ""
    
    // For handling screen capture result
    private var pendingCaptureData: Intent? = null
    
    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize helper classes
        uiManager = UI_MainActivity(this)
        permissionManager = Permission_MainActivity(this)
        serviceManager = Service_MainActivity(this)
        buttonHandler = ButtonHandle_MainActivity(this)
        
        // Create UI with callbacks
        val scrollView = uiManager.createMainLayout(
            onShapeChanged = { selectedShape = it },
            onSizeChanged = { selectedSize = it },
            onOutputSizeChanged = { selectedOutputSize = it },
            onInputDraggableChanged = { isInputDraggable = it },
            onOutputDraggableChanged = { isOutputDraggable = it },
            onWidgetDraggableChanged = { isWidgetDraggable = it },
            onCrosshairChanged = { showCrosshair = it },
            onOutputCrosshairChanged = { showOutputCrosshair = it },
            onZoomSliderChanged = { showZoomSlider = it },
            onMinZoomChanged = { minZoom = it },
            onMaxZoomChanged = { maxZoom = it },
            onColorFilterChanged = { colorFilterMode = it },
            onShaderChanged = { selectedShader = it },
            onToggleClicked = { handleToggleButtonClick() },
            onAboutClicked = { buttonHandler.handleAboutButtonClick() },
            onExitClicked = { buttonHandler.handleExitButtonClick(serviceManager) }
        )
        
        setContentView(scrollView)
    }
    
    private fun handleToggleButtonClick() {
        if (permissionManager.canDrawOverlays()) {
            if (serviceManager.isRunning()) {
                serviceManager.stopService()
                uiManager.toggleButton.text = "Start Magnifier"
            } else {
                serviceManager.startScreenCapture()
            }
        } else {
            permissionManager.requestOverlayPermission()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (!permissionManager.canDrawOverlays()) {
            uiManager.toggleButton.text = "Grant Permission First"
            serviceManager.setRunning(false)
        } else {
            if (!serviceManager.isRunning()) {
                uiManager.toggleButton.text = "Start Magnifier"
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Log.d(TAG, "Got permission: resultCode=$resultCode (${Activity.RESULT_OK})")
                
                // Start magnifier with current configuration
                serviceManager.startMagnifier(
                    selectedShape = selectedShape,
                    selectedSize = selectedSize,
                    selectedOutputSize = selectedOutputSize,
                    isInputDraggable = isInputDraggable,
                    isOutputDraggable = isOutputDraggable,
                    isWidgetDraggable = isWidgetDraggable,
                    showCrosshair = showCrosshair,
                    crosshairColor = crosshairColor,
                    colorFilterMode = colorFilterMode,
                    showZoomSlider = showZoomSlider,
                    minZoom = minZoom,
                    maxZoom = maxZoom,
                    selectedShader = selectedShader,
                    captureData = data
                )
                
                uiManager.toggleButton.text = "Stop Magnifier"
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                uiManager.toggleButton.text = "Start Magnifier"
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
