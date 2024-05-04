package com.example.photoeditorapplication
import android.graphics.Bitmap

fun rotateImage(source: Bitmap, clockwise: Boolean): Bitmap {
    val width = source.width
    val height = source.height
    val newWidth = height
    val newHeight = width

    val result = Bitmap.createBitmap(newWidth, newHeight, source.config)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = source.getPixel(x, y)
            if (clockwise) {
                result.setPixel(y, newHeight - x - 1, pixel)
            } else {
                result.setPixel(newWidth - y - 1, x, pixel)
            }
        }
    }

    return result
}
