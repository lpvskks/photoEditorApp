package com.example.photoeditorapplication

import UnsharpMaskFilter
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.FileOutputStream
import org.opencv.imgproc.Imgproc
import org.opencv.core.Scalar
import java.util.Random
import kotlin.math.abs
import kotlin.math.min

class ImageEditActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var originalBitmap: Bitmap
    private lateinit var filteredBitmap: Bitmap
    private lateinit var retouchBitmap: Bitmap
    private lateinit var scaledBitmap: Bitmap
    private lateinit var filtersScrollView: HorizontalScrollView
    private lateinit var rotationSeekBar: SeekBar
    private lateinit var scalingSeekBar: SeekBar
    private lateinit var filtersButton: ImageButton
    private lateinit var filtersContainer: LinearLayout
    private lateinit var brushSizeSeekBar: SeekBar
    private lateinit var retouchStrengthSeekBar: SeekBar
    private lateinit var retouchButton: ImageButton
    private lateinit var cubeButton: ImageButton
    private lateinit var currentBitmap: Bitmap
    private lateinit var unsharpMaskFilter: UnsharpMaskFilter

    private lateinit var blurRadiusSeekBar: SeekBar
    private lateinit var unsharpMaskThresholdSeekBar: SeekBar
    private lateinit var maskingSlidersLayout: View

    private var rotationSeekBarVisible = false
    private var scalingSeekBarVisible = false
    private var filtersVisible = false
    private var isFaceDetectionApplied = false
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_edit)

        imageView = findViewById(R.id.selectedImageView)
        filtersScrollView = findViewById(R.id.filtersScrollView)
        rotationSeekBar = findViewById(R.id.rotationSeekBar)
        scalingSeekBar = findViewById(R.id.scalingSeekBar)
        filtersButton = findViewById(R.id.filtersButton)
        filtersContainer = findViewById(R.id.filtersContainer)
        brushSizeSeekBar = findViewById(R.id.brushSizeSeekBar)
        retouchStrengthSeekBar = findViewById(R.id.retouchStrengthSeekBar)
        retouchButton = findViewById(R.id.retouchButton)
        cubeButton = findViewById(R.id.cubeButton)
        unsharpMaskFilter = UnsharpMaskFilter()

        blurRadiusSeekBar = findViewById(R.id.blurRadiusSeekBar)
        unsharpMaskThresholdSeekBar = findViewById(R.id.unsharpMaskThresholdSeekBar)
        maskingSlidersLayout = findViewById(R.id.maskingSlidersLayout)

        val closeButton: ImageView = findViewById(R.id.closeButton)
        val saveButton: ImageView = findViewById(R.id.saveButton)
        val rotateButton: ImageView = findViewById(R.id.photoRotationButton)
        val scaleButton: ImageView = findViewById(R.id.scallingButton)
        val maskingButton = findViewById<ImageButton>(R.id.maskingButton)
        maskingButton.setOnClickListener {
            toggleMaskingSeekBars()
        }

        cubeButton.setOnClickListener {
            val intent = Intent(this, CubeActivity::class.java)
            startActivity(intent)
        }

        val imageUri: Uri? = intent.getParcelableExtra("imageUri")
        if (imageUri != null) {
            imageView.setImageURI(imageUri)
            imageView.drawable?.let {
                originalBitmap = (it as BitmapDrawable).bitmap
                scaledBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                currentBitmap = originalBitmap.copy(originalBitmap.config, true)
            }
        }

        blurRadiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyUnsharpMask()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        unsharpMaskThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyUnsharpMask()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


    rotationSeekBar.visibility = View.GONE
        scalingSeekBar.visibility = View.GONE

        filtersButton.setOnClickListener {
            filtersVisible = !filtersVisible
            rotationSeekBarVisible = false
            scalingSeekBarVisible = false
            updateUIVisibility()
        }


        findViewById<ImageButton>(R.id.filter1Button).setOnClickListener {
            applyRedFilter()
        }

        findViewById<ImageButton>(R.id.filter2Button).setOnClickListener {
            applyNegativeFilter()
        }

        findViewById<ImageButton>(R.id.filter3Button).setOnClickListener {
            applyMonochromeFilter()
        }

        findViewById<ImageButton>(R.id.filter4Button).setOnClickListener {
            applyNoiseBlurFilter()
        }

        findViewById<ImageButton>(R.id.filter5Button).setOnClickListener {
            applySeaFilter()
        }

        findViewById<ImageButton>(R.id.filter6Button).setOnClickListener {
            applySepiaToneEffect()
        }

        findViewById<ImageButton>(R.id.filter7Button).setOnClickListener {
            applyHighContrastFilter()
        }

        findViewById<ImageButton>(R.id.filter8Button).setOnClickListener {
            applyPsychedelicFilter()
        }

        rotateButton.setOnClickListener {
            rotationSeekBarVisible = !rotationSeekBarVisible
            scalingSeekBarVisible = false
            filtersVisible = false
            updateUIVisibility()
        }

        scaleButton.setOnClickListener {
            scalingSeekBarVisible = !scalingSeekBarVisible
            rotationSeekBarVisible = false
            filtersVisible = false
            updateUIVisibility()
        }

        closeButton.setOnClickListener { finish() }
        saveButton.setOnClickListener { requestStoragePermission() }


        rotationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val rotatedBitmap = rotateBitmap(scaledBitmap, progress.toFloat())
                imageView.setImageBitmap(rotatedBitmap)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onStopTrackingTouch(seekBar: SeekBar) { }
        })

        scalingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                scaleImage(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val buttonFaceDetection: ImageView = findViewById(R.id.faceRecognitionButton)
        buttonFaceDetection.setOnClickListener {
            rotationSeekBarVisible = false
            scalingSeekBarVisible = false
            filtersVisible = false
            updateUIVisibility()

            if (isFaceDetectionApplied) {
                currentBitmap = originalBitmap.copy(originalBitmap.config, true)
                imageView.setImageBitmap(currentBitmap)
                isFaceDetectionApplied = false
            } else {
                val mat = Mat()
                Utils.bitmapToMat(currentBitmap, mat)
                val faceBitmap = faceDetection(mat, this)
                val processedBitmap = Bitmap.createBitmap(
                    faceBitmap.cols(),
                    faceBitmap.rows(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(faceBitmap, processedBitmap)
                imageView.setImageBitmap(processedBitmap)
                currentBitmap = processedBitmap
                isFaceDetectionApplied = true
            }
        }
    }
    private fun updateUIVisibility() {
        rotationSeekBar.visibility = if (rotationSeekBarVisible) View.VISIBLE else View.GONE
        scalingSeekBar.visibility = if (scalingSeekBarVisible) View.VISIBLE else View.GONE
        filtersContainer.visibility = if (filtersVisible) View.VISIBLE else View.GONE
    }

    companion object {
        private const val REQUEST_STORAGE_PERMISSION_CODE = 101
    }

    private fun toggleMaskingSeekBars() {
        if (maskingSlidersLayout.visibility == View.GONE) {
            maskingSlidersLayout.visibility = View.VISIBLE
        } else {
            maskingSlidersLayout.visibility = View.GONE
        }
    }


    private fun applyUnsharpMask() {
        val blurRadius = blurRadiusSeekBar.progress
        val threshold = unsharpMaskThresholdSeekBar.progress

        // Проверка значений
        println("Applying Unsharp Mask with blurRadius: $blurRadius, threshold: $threshold")

        val blurredBitmap = unsharpMaskFilter.gaussianBlur(currentBitmap, blurRadius)
        val unsharpMaskBitmap = unsharpMaskFilter.unsharpMask(currentBitmap, blurredBitmap, threshold)

        // Проверка выполнения
        println("Unsharp Mask applied, updating ImageView")

        imageView.setImageBitmap(unsharpMaskBitmap)

    }

    private fun applyNegativeFilter() {
        filteredBitmap = applyNegativeFilter(originalBitmap)
        imageView.setImageBitmap(filteredBitmap)
    }

    private fun applyNegativeFilter(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = 255 - Color.red(pixel)
            val green = 255 - Color.green(pixel)
            val blue = 255 - Color.blue(pixel)
            pixels[i] = Color.rgb(red, green, blue)
        }

        val result = Bitmap.createBitmap(width, height, source.config)
        result.setPixels(pixels, 0, width, 0, 0, width, height)

        return result
    }

    private fun applyRedFilter() {
        filteredBitmap = Color.RED.applyRedFilter(originalBitmap)
        imageView.setImageBitmap(filteredBitmap)
    }

    private fun Int.applyRedFilter(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = Color.red(this)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            pixels[i] = Color.rgb(red, green, blue)
        }

        val result = Bitmap.createBitmap(width, height, source.config)
        result.setPixels(pixels, 0, width, 0, 0, width, height)

        return result
    }

    private fun applyMonochromeFilter() {
        filteredBitmap = applyMonochromeFilter(originalBitmap)
        imageView.setImageBitmap(filteredBitmap)
    }

    private fun applyMonochromeFilter(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val luminance = (red * 0.299 + green * 0.587 + blue * 0.114).toInt()
            pixels[i] = Color.rgb(luminance, luminance, luminance)
        }

        val result = Bitmap.createBitmap(width, height, source.config)
        result.setPixels(pixels, 0, width, 0, 0, width, height)

        return result
    }

    private fun applyHighContrastFilter() {
        filteredBitmap = applyHighContrastFilter(originalBitmap)
        imageView.setImageBitmap(filteredBitmap)
    }

    private fun applyHighContrastFilter(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = Array(width) { IntArray(height) }

        for (x in 0 until width) {
            for (y in 0 until height) {
                pixels[x][y] = source.getPixel(x, y)
            }
        }

        val newPixels = Array(width) { IntArray(height) }
        for (x in 0 until width) {
            for (y in 0 until height) {
                val neighbors = getNeighbors(pixels, x, y)
                var totalR = 0.0
                var totalG = 0.0
                var totalB = 0.0

                for (pixel in neighbors) {
                    val currentR = Color.red(pixel).toDouble()
                    val currentG = Color.green(pixel).toDouble()
                    val currentB = Color.blue(pixel).toDouble()
                    for (otherPixel in neighbors) {
                        val otherR = Color.red(otherPixel).toDouble()
                        val otherG = Color.green(otherPixel).toDouble()
                        val otherB = Color.blue(otherPixel).toDouble()

                        totalR += Math.abs(otherR - currentR)
                        totalG += Math.abs(otherG - currentG)
                        totalB += Math.abs(otherB - currentB)
                    }
                }

                val n = neighbors.size
                totalR /= (n * n)
                totalG /= (n * n)
                totalB /= (n * n)

                newPixels[x][y] = Color.rgb(settle(totalR), settle(totalG), settle(totalB))
            }
        }

        val result = Bitmap.createBitmap(width, height, source.config)
        for (x in 0 until width) {
            for (y in 0 until height) {
                result.setPixel(x, y, newPixels[x][y])
            }
        }

        return result
    }


    private fun getNeighbors(pixels: Array<IntArray>, x: Int, y: Int): List<Int> {
        val neighbors = mutableListOf<Int>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until pixels.size && ny in 0 until pixels[0].size) {
                    neighbors.add(pixels[nx][ny])
                }
            }
        }
        return neighbors
    }

    private fun settle(n: Double): Int {
        return Math.max(0, Math.min(255, n.toInt()))
    }

    private fun applyNoiseBlurFilter() {
        filteredBitmap = applyNoiseBlurFilter(originalBitmap, 70f)
        imageView.setImageBitmap(filteredBitmap)
    }
    private fun applyNoiseBlurFilter(source: Bitmap, magnitude: Float): Bitmap {
        val width = source.width
        val height = source.height
        val noisyBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val random = Random()

        for (x in 0 until width) {
            for (y in 0 until height) {
                val offset = random.nextFloat() * magnitude
                val newX = (x + offset).toInt().coerceIn(0, width - 1)
                val newY = (y + offset).toInt().coerceIn(0, height - 1)
                val pixel = source.getPixel(newX, newY)
                noisyBitmap.setPixel(x, y, pixel)
            }
        }

        return noisyBitmap
    }

    private fun applySeaFilter() {
        filteredBitmap = applySeaFilter(originalBitmap)
        imageView.setImageBitmap(filteredBitmap)
    }

    private fun applySeaFilter(source: Bitmap): Bitmap {
        val resultBitmap = Bitmap.createBitmap(source.width, source.height, source.config)
        val amplitude = 20

        for (x in 0 until source.width) {
            for (y in 0 until source.height) {
                val pixel = source.getPixel(x, y)

                val alpha = Color.alpha(pixel)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                val newRed = min(255, (red * 1.2).toInt())
                val newGreen = min(255, (green * 1.2).toInt())
                val newBlue = min(255, (blue * 1.2).toInt() + 30) // Добавляем оттенок синего

                val waveOffset = (amplitude * Math.sin(2 * Math.PI * y / 64)).toInt()
                val newX = (x + waveOffset).coerceIn(0, source.width - 1)

                val newPixel = Color.argb(alpha, newRed, newGreen, newBlue)
                resultBitmap.setPixel(newX, y, newPixel)
            }
        }

        return resultBitmap
    }

    private fun applyPsychedelicFilter() {
        filteredBitmap = applyPsychedelicFilter(originalBitmap)
        imageView.setImageBitmap(filteredBitmap)
    }

    private fun applyPsychedelicFilter(source: Bitmap): Bitmap {
        val resultBitmap = Bitmap.createBitmap(source.width, source.height, source.config)

        for (x in 0 until source.width) {
            for (y in 0 until source.height) {
                val pixel = source.getPixel(x, y)

                val alpha = Color.alpha(pixel)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                val newRed = (Math.sin(0.1 * red + 2.0) * 255).toInt().coerceIn(0, 255)
                val newGreen = (Math.sin(0.1 * green + 4.0) * 255).toInt().coerceIn(0, 255)
                val newBlue = (Math.sin(0.1 * blue + 6.0) * 255).toInt().coerceIn(0, 255)

                val adjustedRed = (newRed * 1.2).toInt().coerceIn(0, 255)
                val adjustedGreen = (newGreen * 1.2).toInt().coerceIn(0, 255)
                val adjustedBlue = (newBlue * 1.2).toInt().coerceIn(0, 255)

                val newPixel = Color.argb(alpha, adjustedRed, adjustedGreen, adjustedBlue)
                resultBitmap.setPixel(x, y, newPixel)
            }
        }
        return resultBitmap
    }


    private fun applySepiaToneEffect() {
        filteredBitmap = applySepiaToneEffect(originalBitmap)
        imageView.setImageBitmap(filteredBitmap)
    }
    private fun applySepiaToneEffect(source: Bitmap) : Bitmap {
        val sepiaBitmap = source.copy(source.config, true)

        for (x in 0 until source.width) {
            for (y in 0 until source.height) {
                val pixel = source.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val outputR = (r * 0.393 + g * 0.769 + b * 0.189).toInt()
                val outputG = (r * 0.349 + g * 0.686 + b * 0.168).toInt()
                val outputB = (r * 0.272 + g * 0.534 + b * 0.131).toInt()

                sepiaBitmap.setPixel(x, y, Color.rgb(
                    if (outputR > 255) 255 else outputR,
                    if (outputG > 255) 255 else outputG,
                    if (outputB > 255) 255 else outputB
                ))
            }
        }

        return sepiaBitmap
    }

    private fun scaleImage(scalePercent: Int) {
        val newWidth = originalBitmap.width * scalePercent / 100
        val newHeight = originalBitmap.height * scalePercent / 100
        val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)

        for (x in 0 until newWidth) {
            for (y in 0 until newHeight) {
                val srcX = x.toFloat() / newWidth * (originalBitmap.width - 1)
                val srcY = y.toFloat() / newHeight * (originalBitmap.height - 1)
                val x0 = srcX.toInt()
                val y0 = srcY.toInt()
                val x1 = Math.min(x0 + 1, originalBitmap.width - 1)
                val y1 = Math.min(y0 + 1, originalBitmap.height - 1)

                val p00 = originalBitmap.getPixel(x0, y0)
                val p01 = originalBitmap.getPixel(x0, y1)
                val p10 = originalBitmap.getPixel(x1, y0)
                val p11 = originalBitmap.getPixel(x1, y1)

                val a = srcX - x0
                val b = srcY - y0

                val color = interpolateColors(p00, p01, p10, p11, a, b)

                scaledBitmap.setPixel(x, y, color)
            }
        }

        imageView.setImageBitmap(scaledBitmap)
    }

    private fun interpolateColors(p00: Int, p01: Int, p10: Int, p11: Int, a: Float, b: Float): Int {
        val red = (1 - a) * (1 - b) * (p00.red()) +
                a * (1 - b) * (p10.red()) +
                (1 - a) * b * (p01.red()) +
                a * b * (p11.red())

        val green = (1 - a) * (1 - b) * (p00.green()) +
                a * (1 - b) * (p10.green()) +
                (1 - a) * b * (p01.green()) +
                a * b * (p11.green())

        val blue = (1 - a) * (1 - b) * (p00.blue()) +
                a * (1 - b) * (p10.blue()) +
                (1 - a) * b * (p01.blue()) +
                a * b * (p11.blue())

        val alpha = (1 - a) * (1 - b) * (p00.alpha()) +
                a * (1 - b) * (p10.alpha()) +
                (1 - a) * b * (p01.alpha()) +
                a * b * (p11.alpha())

        return Color.argb(alpha.toInt(), red.toInt(), green.toInt(), blue.toInt())
    }

    private fun Int.red() = (this shr 16) and 0xFF
    private fun Int.green() = (this shr 8) and 0xFF
    private fun Int.blue() = this and 0xFF
    private fun Int.alpha() = (this shr 24) and 0xFF
    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val radians = Math.toRadians(degrees.toDouble())
        val cos = cos(radians)
        val sin = sin(radians)

        val srcWidth = source.width
        val srcHeight = source.height
        val newWidth = (srcWidth * abs(cos) + srcHeight * abs(sin)).roundToInt()
        val newHeight = (srcWidth * abs(sin) + srcHeight * abs(cos)).roundToInt()

        val result = Bitmap.createBitmap(newWidth, newHeight, source.config)
        val centerX = srcWidth / 2
        val centerY = srcHeight / 2
        val newCenterX = newWidth / 2
        val newCenterY = newHeight / 2

        for (x in 0 until newWidth) {
            for (y in 0 until newHeight) {
                val deltaX = x - newCenterX
                val deltaY = y - newCenterY
                val originalX = (deltaX * cos + deltaY * sin + centerX).roundToInt()
                val originalY = (-deltaX * sin + deltaY * cos + centerY).roundToInt()

                if (originalX in 0 until srcWidth && originalY in 0 until srcHeight) {
                    val pixel = source.getPixel(originalX, originalY)
                    result.setPixel(x, y, pixel)
                }
            }
        }

        return result
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION_CODE)
        } else {
            saveImage()
        }
    }

    private fun saveImage() {
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "edited_image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    Toast.makeText(this, "Image saved successfully", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun faceDetection(input: Mat, context: Context): Mat {
        val cascadeFile =
            File(context.getExternalFilesDir(null), "haarcascade_frontalface_alt2.xml")
        if (!cascadeFile.exists()) {
            val inputStream: InputStream =
                context.resources.openRawResource(R.raw.haarcascade_frontalface_alt2)
            val outputStream: OutputStream = FileOutputStream(cascadeFile)

            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            outputStream.close()
        }

        val faceCascade = CascadeClassifier(cascadeFile.absolutePath)
        if (faceCascade.empty()) {
            println("Error loading cascade: ${cascadeFile.absolutePath}")
        } else {
            val faces = MatOfRect()
            faceCascade.detectMultiScale(input, faces)
            for (rect: Rect in faces.toArray()) {
                Imgproc.rectangle(input, rect.tl(), rect.br(), Scalar(154.0, 254.0, 4.0), 2)
            }
        }

        return input
    }
}
