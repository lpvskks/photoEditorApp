package com.example.photoeditorapplication

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

class ImageEditActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var originalBitmap: Bitmap
    private lateinit var filtersScrollView: HorizontalScrollView
    private lateinit var rotationSeekBar: SeekBar

    companion object {
        private const val REQUEST_STORAGE_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_edit)

        imageView = findViewById(R.id.selectedImageView)
        filtersScrollView = findViewById(R.id.filtersScrollView)
        rotationSeekBar = findViewById(R.id.rotationSeekBar)
        rotationSeekBar.visibility = View.GONE

        val closeButton: ImageView = findViewById(R.id.closeButton)
        val saveButton: ImageView = findViewById(R.id.saveButton)
        val rotateButton: ImageView = findViewById(R.id.photoRotationButton)
        val filtersButton: ImageView = findViewById(R.id.filtersButton)
        val imageUri: Uri? = intent.getParcelableExtra("imageUri")

        if (imageUri != null) {
            imageView.setImageURI(imageUri)
            imageView.drawable?.let {
                originalBitmap = (it as BitmapDrawable).bitmap
            }
        }

        closeButton.setOnClickListener { finish() }
        saveButton.setOnClickListener { requestStoragePermission() }
        rotateButton.setOnClickListener { toggleSeekBarVisibility() }
        filtersButton.setOnClickListener { toggleFiltersVisibility() }

        rotationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val rotatedBitmap = rotateBitmap(originalBitmap, progress.toFloat())
                imageView.setImageBitmap(rotatedBitmap)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onStopTrackingTouch(seekBar: SeekBar) { }
        })
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

    private fun toggleFiltersVisibility() {
        filtersScrollView.visibility = if (filtersScrollView.visibility == View.GONE) View.VISIBLE else View.GONE
    }

    private fun toggleSeekBarVisibility() {
        rotationSeekBar.visibility = if (rotationSeekBar.visibility == View.GONE) {
            updateImageViewLayout(true)
            View.VISIBLE
        } else {
            updateImageViewLayout(false)
            View.GONE
        }
    }

    private fun updateImageViewLayout(isSeekBarVisible: Boolean) {
        val params = imageView.layoutParams as RelativeLayout.LayoutParams
        if (isSeekBarVisible) {
            params.addRule(RelativeLayout.ABOVE, R.id.rotationSeekBar)
        } else {
            params.addRule(RelativeLayout.ABOVE, R.id.toolsScrollView)
        }
        imageView.layoutParams = params
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
}
