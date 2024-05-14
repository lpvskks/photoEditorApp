package com.example.photoeditorapplication

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ImageEditActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var filtersScrollView: HorizontalScrollView

    companion object {
        private const val REQUEST_STORAGE_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_edit)

        imageView = findViewById(R.id.selectedImageView)
        filtersScrollView = findViewById(R.id.filtersScrollView)

        val closeButton = findViewById<ImageView>(R.id.closeButton)
        val saveButton = findViewById<ImageView>(R.id.saveButton)
        val rotateButton = findViewById<ImageView>(R.id.photoRotationButton)
        val filtersButton = findViewById<ImageView>(R.id.filtersButton)
        val imageUri: Uri? = intent.getParcelableExtra("imageUri")

        imageView.setImageURI(imageUri)

        closeButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            requestStoragePermission()
        }

        rotateButton.setOnClickListener {
            rotateImage()
        }

        filtersButton.setOnClickListener {
            toggleFiltersVisibility()
        }
    }

    private fun rotateImage() {
        val currentBitmap = (imageView.drawable as BitmapDrawable).bitmap
        val rotatedBitmap = rotateBitmap(currentBitmap, 90f)
        imageView.setImageBitmap(rotatedBitmap)
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun toggleFiltersVisibility() {
        if (filtersScrollView.visibility == View.GONE) {
            filtersScrollView.visibility = View.VISIBLE
        } else {
            filtersScrollView.visibility = View.GONE
        }
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION_CODE)
        } else {
            saveImage()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveImage()
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
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
                }
                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
