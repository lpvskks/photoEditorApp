import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.exp

class UnsharpMaskFilter {
    fun gaussianBlur(image: Bitmap, radius: Int): Bitmap {
        val width = image.width
        val height = image.height
        val blurredImage = Bitmap.createBitmap(width, height, image.config)
        val kernel = createGaussianKernel(radius)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0.0
                var g = 0.0
                var b = 0.0
                for (ky in -radius..radius) {
                    for (kx in -radius..radius) {
                        val pixelX = (x + kx).coerceIn(0, width - 1)
                        val pixelY = (y + ky).coerceIn(0, height - 1)
                        val pixelColor = image.getPixel(pixelX, pixelY)
                        val weight = kernel[ky + radius][kx + radius]

                        r += Color.red(pixelColor) * weight
                        g += Color.green(pixelColor) * weight
                        b += Color.blue(pixelColor) * weight
                    }
                }
                blurredImage.setPixel(x, y, Color.rgb(r.toInt(), g.toInt(), b.toInt()))
            }
        }
        return blurredImage
    }

    private fun createGaussianKernel(radius: Int): Array<DoubleArray> {
        val size = radius * 2 + 1
        val kernel = Array(size) { DoubleArray(size) }
        val sigma = radius.toDouble() / 3
        var sum = 0.0

        for (y in -radius..radius) {
            for (x in -radius..radius) {
                val value = exp(-(x * x + y * y) / (2 * sigma * sigma)) / (2 * Math.PI * sigma * sigma)
                kernel[y + radius][x + radius] = value
                sum += value
            }
        }

        // Нормализация ядра
        for (y in kernel.indices) {
            for (x in kernel[y].indices) {
                kernel[y][x] /= sum
            }
        }
        return kernel
    }

    fun unsharpMask(original: Bitmap, blurred: Bitmap, threshold: Int): Bitmap {
        val width = original.width
        val height = original.height
        val sharpenedImage = Bitmap.createBitmap(width, height, original.config)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val originalColor = original.getPixel(x, y)
                val blurredColor = blurred.getPixel(x, y)

                val r = Color.red(originalColor) - Color.red(blurredColor)
                val g = Color.green(originalColor) - Color.green(blurredColor)
                val b = Color.blue(originalColor) - Color.blue(blurredColor)

                val newR = if (Math.abs(r) > threshold) Color.red(originalColor) + r else Color.red(originalColor)
                val newG = if (Math.abs(g) > threshold) Color.green(originalColor) + g else Color.green(originalColor)
                val newB = if (Math.abs(b) > threshold) Color.blue(originalColor) + b else Color.blue(originalColor)

                sharpenedImage.setPixel(x, y, Color.rgb(newR.coerceIn(0, 255), newG.coerceIn(0, 255), newB.coerceIn(0, 255)))
            }
        }
        return sharpenedImage
    }
}
