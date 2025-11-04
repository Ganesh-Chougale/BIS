package com.example.bis.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import com.example.bis.config.MagnifierConfig

/**
 * Manages screen capture using MediaProjection API.
 * Handles VirtualDisplay, ImageReader, and bitmap processing.
 */
class ScreenCaptureManager(
    private val context: Context,
    private val config: MagnifierConfig,
    private val onFrameAvailable: (Bitmap) -> Unit
) {
    private val TAG = "ScreenCaptureManager"
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private var isCapturing = false
    private var frameCount = 0
    
    /**
     * Start screen capture with the given permission result
     */
    fun startCapture(resultCode: Int, data: Intent) {
        if (isCapturing) {
            Log.w(TAG, "Already capturing")
            return
        }
        
        try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            // Register callback (required for Android 14+)
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection stopped")
                    cleanup()
                }
            }, handler)
            
            // Create ImageReader to capture screen
            imageReader = ImageReader.newInstance(
                config.screenWidth,
                config.screenHeight,
                PixelFormat.RGBA_8888,
                2
            )
            
            imageReader?.setOnImageAvailableListener({ reader ->
                processImage(reader)
            }, handler)
            
            // Create VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                config.screenWidth,
                config.screenHeight,
                config.screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
            
            isCapturing = true
            Log.d(TAG, "Screen capture started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen capture", e)
            cleanup()
        }
    }
    
    /**
     * Process captured image and extract magnified region
     */
    private fun processImage(reader: ImageReader) {
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image != null) {
                // Convert Image to Bitmap
                val fullBitmap = imageToBitmap(image)
                
                // Calculate the actual crop size based on zoom factor
                // Higher zoom = smaller crop area (more magnification)
                // The crop size is: outputSize / zoomFactor
                val actualCropSize = (config.outputSize / config.zoomFactor).toInt()
                
                // Center the crop within the input selector area
                val centerX = config.inputX + (config.inputSize / 2)
                val centerY = config.inputY + (config.inputSize / 2)
                
                // Calculate crop bounds (centered)
                val cropLeft = (centerX - actualCropSize / 2).coerceAtLeast(0)
                val cropTop = (centerY - actualCropSize / 2).coerceAtLeast(0)
                val cropRight = (cropLeft + actualCropSize).coerceAtMost(fullBitmap.width)
                val cropBottom = (cropTop + actualCropSize).coerceAtMost(fullBitmap.height)
                
                val cropWidth = cropRight - cropLeft
                val cropHeight = cropBottom - cropTop
                
                // Log every 60 frames to verify capture is running
                frameCount++
                if (frameCount % 60 == 0) {
                    Log.d(TAG, "Frame $frameCount - Zoom: ${config.zoomFactor}x, Crop: ${cropWidth}x${cropHeight} at ($cropLeft, $cropTop)")
                }
                
                // Crop the selected area
                val croppedBitmap = Bitmap.createBitmap(
                    fullBitmap,
                    cropLeft,
                    cropTop,
                    cropWidth,
                    cropHeight
                )
                
                // Scale the cropped bitmap to fill the output window
                // This creates the magnification effect
                val magnifiedBitmap = Bitmap.createScaledBitmap(
                    croppedBitmap,
                    config.outputSize,
                    config.outputSize,
                    true
                )
                
                // Send to callback
                onFrameAvailable(magnifiedBitmap)
                
                // Cleanup temporary bitmaps
                croppedBitmap.recycle()
                fullBitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
        } finally {
            image?.close()
        }
    }
    
    /**
     * Convert Image to Bitmap
     */
    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * config.screenWidth
        
        val bitmap = Bitmap.createBitmap(
            config.screenWidth + rowPadding / pixelStride,
            config.screenHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }
    
    /**
     * Stop screen capture and release resources
     */
    fun stopCapture() {
        cleanup()
        isCapturing = false
        Log.d(TAG, "Screen capture stopped")
    }
    
    /**
     * Clean up MediaProjection resources
     */
    private fun cleanup() {
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.close()
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
    }
    
    /**
     * Check if currently capturing
     */
    fun isCapturing(): Boolean = isCapturing
}