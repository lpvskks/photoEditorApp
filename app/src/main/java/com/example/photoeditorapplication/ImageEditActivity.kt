package com.example.photoeditorapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
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
class ImageEditActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var originalBitmap: Bitmap
    private lateinit var filteredBitmap: Bitmap
    private lateinit var scaledBitmap: Bitmap
    private lateinit var filtersScrollView: HorizontalScrollView
    private lateinit var rotationSeekBar: SeekBar
    private lateinit var scalingSeekBar: SeekBar
    private lateinit var filtersButton: ImageButton
    private lateinit var filtersContainer: LinearLayout
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

        val closeButton: ImageView = findViewById(R.id.closeButton)
        val saveButton: ImageView = findViewById(R.id.saveButton)
        val rotateButton: ImageView = findViewById(R.id.photoRotationButton)
        val scaleButton: ImageView = findViewById(R.id.scallingButton)

        val imageUri: Uri? = intent.getParcelableExtra("imageUri")
            if (imageUri != null) {
                imageView.setImageURI(imageUri)
                imageView.drawable?.let {
                    originalBitmap = (it as BitmapDrawable).bitmap
                    scaledBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                }
            }

        filtersButton.setOnClickListener {
            toggleFiltersVisibility()
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
            applyHighContrastFilter()
        }



        closeButton.setOnClickListener { finish() }
        saveButton.setOnClickListener { requestStoragePermission() }
        rotateButton.setOnClickListener { toggleSeekBarVisibility(rotationSeekBar) }
        scaleButton.setOnClickListener { toggleSeekBarVisibility(scalingSeekBar) }

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
            originalBitmap.let { bitmap ->
                val mat = Mat()
                Utils.bitmapToMat(bitmap, mat)
                val faceBitmap = faceDetection(mat, this)
                val processedBitmap = Bitmap.createBitmap(
                    faceBitmap.cols(),
                    faceBitmap.rows(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(faceBitmap, processedBitmap)
                imageView.setImageBitmap(processedBitmap)
                originalBitmap = processedBitmap
            }
        }

    }
    private fun toggleSeekBarVisibility(seekBar: SeekBar) {
        if (seekBar.visibility == View.GONE) {
            rotationSeekBar.visibility = View.GONE
            scalingSeekBar.visibility = View.GONE
            seekBar.visibility = View.VISIBLE
        } else {
            seekBar.visibility = View.GONE
        }
    }
    private fun toggleFiltersVisibility() {
        filtersContainer.visibility = if (filtersContainer.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
    companion object {
        private const val REQUEST_STORAGE_PERMISSION_CODE = 101
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
        filteredBitmap = applyRedFilter(originalBitmap, Color.RED)
        imageView.setImageBitmap(filteredBitmap)
    }

    private fun applyRedFilter(source: Bitmap, color: Int): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = Color.red(color)
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

    private fun scaleImage(scalePercent: Int) {
        val newWidth = originalBitmap.width * scalePercent / 100
        val newHeight = originalBitmap.height * scalePercent / 100
        scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        for (x in 0 until newWidth) {
            for (y in 0 until newHeight) {
                val srcX = (x * originalBitmap.width / newWidth).toInt()
                val srcY = (y * originalBitmap.height / newHeight).toInt()
                scaledBitmap.setPixel(x, y, originalBitmap.getPixel(srcX, srcY))
            }
        }
        imageView.setImageBitmap(scaledBitmap)
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val radians = Math.toRadians(degrees.toDouble())
        val cos = cos(radians)
        val sin = sin(radians)

        val srcWidth = source.width
        val srcHeight = source.height
        val newWidth = (srcWidth * kotlin.math.abs(cos) + srcHeight * kotlin.math.abs(sin)).roundToInt()
        val newHeight = (srcWidth * kotlin.math.abs(sin) + srcHeight * kotlin.math.abs(cos)).roundToInt()

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

    fun faceDetection(input: Mat, context: Context): Mat {
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
