package com.example.bis.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Handles color filter processing for bitmaps
 */
object ColorFilterProcessor {
    
    /**
     * Apply color filter to bitmap based on mode
     */
    fun applyFilter(bitmap: Bitmap, filterMode: String): Bitmap {
        return when (filterMode) {
            "INVERSE" -> applyInverseFilter(bitmap)
            "MONOCHROME" -> applyMonochromeFilter(bitmap)
            else -> bitmap
        }
    }
    
    /**
     * Apply inverse color filter (negative)
     */
    private fun applyInverseFilter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val invertedBitmap = Bitmap.createBitmap(width, height, bitmap.config)
        
        val canvas = Canvas(invertedBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return invertedBitmap
    }
    
    /**
     * Apply monochrome (grayscale) filter
     */
    private fun applyMonochromeFilter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val monochromeBitmap = Bitmap.createBitmap(width, height, bitmap.config)
        
        val canvas = Canvas(monochromeBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)  // Remove all color saturation
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return monochromeBitmap
    }
}