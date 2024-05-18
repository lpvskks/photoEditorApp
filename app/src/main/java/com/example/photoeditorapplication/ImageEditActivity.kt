package com.example.photoeditorapplication

import android.Manifest
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
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
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
    private lateinit var scaledBitmap: Bitmap
    private lateinit var filtersScrollView: HorizontalScrollView
    private lateinit var rotationSeekBar: SeekBar
    private lateinit var scalingSeekBar: SeekBar
    private lateinit var brushSizeSeekBar: SeekBar
    private lateinit var retouchStrengthSeekBar: SeekBar
    private var brushSize: Int = 0
    private var retouchStrength: Double = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_edit)

        imageView = findViewById(R.id.selectedImageView)
        filtersScrollView = findViewById(R.id.filtersScrollView)
        rotationSeekBar = findViewById(R.id.rotationSeekBar)
        scalingSeekBar = findViewById(R.id.scalingSeekBar)
        retouchStrengthSeekBar = findViewById(R.id.retouchStrengthSeekBar)
        brushSizeSeekBar = findViewById(R.id.brushSizeSeekBar)

        val closeButton: ImageView = findViewById(R.id.closeButton)
        val saveButton: ImageView = findViewById(R.id.saveButton)
        val rotateButton: ImageView = findViewById(R.id.photoRotationButton)
        val scaleButton: ImageView = findViewById(R.id.scallingButton)
        val retouchButton: ImageView = findViewById(R.id.retouchButton)

        val imageUri: Uri? = intent.getParcelableExtra("imageUri")
            if (imageUri != null) {
                imageView.setImageURI(imageUri)
                imageView.drawable?.let {
                    originalBitmap = (it as BitmapDrawable).bitmap
                    scaledBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                }
            }

        val buttonFaceDetection: ImageView = findViewById(R.id.faceRecognitionButton)
        buttonFaceDetection.setOnClickListener {
            originalBitmap?.let { bitmap ->
                val mat = Mat()
                Utils.bitmapToMat(bitmap, mat)
                val faceBitmap = faceDetection(mat, this) // Передаем this как context
                val processedBitmap = Bitmap.createBitmap(
                    faceBitmap.cols(),
                    faceBitmap.rows(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(faceBitmap, processedBitmap)
                imageView.setImageBitmap(processedBitmap)
                originalBitmap = processedBitmap
            } ?: Toast.makeText(this, "Нет изображения для обработки", Toast.LENGTH_SHORT).show()
        }

        closeButton.setOnClickListener { finish() }
        saveButton.setOnClickListener { requestStoragePermission() }
        rotateButton.setOnClickListener { toggleSeekBarVisibility(rotationSeekBar) }
        scaleButton.setOnClickListener { toggleSeekBarVisibility(scalingSeekBar) }
        retouchButton.setOnClickListener {
            toggleSeekBarVisibility(retouchStrengthSeekBar)
            toggleSeekBarVisibility(brushSizeSeekBar)
        }

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

        brushSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                brushSize = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        retouchStrengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                retouchStrength = progress.toDouble() / 100
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        imageView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    applyRetouch(event.x.toInt(), event.y.toInt())
                }
            }
            true
        }

    }
    private fun applyRetouch(x: Int, y: Int) {
        val centerX = x
        val centerY = y
        val radius = brushSize / 2

        for (i in -radius..radius) {
            for (j in -radius..radius) {
                val pixelX = centerX + i
                val pixelY = centerY + j
                if (pixelX in 0 until scaledBitmap.width && pixelY in 0 until scaledBitmap.height) {
                    val distance = Math.sqrt((i * i + j * j).toDouble())
                    if (distance <= radius) {
                        val strength = (1 - distance / radius) * retouchStrength
                        applyRetouchToPixel(pixelX, pixelY, strength)
                    }
                }
            }
        }

        imageView.setImageBitmap(scaledBitmap)
    }

    private fun applyRetouchToPixel(x: Int, y: Int, strength: Double) {
        val pixel = scaledBitmap.getPixel(x, y)
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)

        val newRed = (red + (255 - red) * strength * retouchStrength).toInt().coerceIn(0..255)
        val newGreen = (green + (255 - green) * strength * retouchStrength).toInt().coerceIn(0..255)
        val newBlue = (blue + (255 - blue) * strength * retouchStrength).toInt().coerceIn(0..255)

        val newPixel = Color.rgb(newRed, newGreen, newBlue)
        scaledBitmap.setPixel(x, y, newPixel)
    }



    companion object {
        private const val REQUEST_STORAGE_PERMISSION_CODE = 101
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

    private fun toggleSeekBarVisibility(seekBar: SeekBar) {
        if (seekBar.visibility == View.GONE) {
            rotationSeekBar.visibility = View.GONE
            scalingSeekBar.visibility = View.GONE
            seekBar.visibility = View.VISIBLE
        } else {
            seekBar.visibility = View.GONE
        }
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
