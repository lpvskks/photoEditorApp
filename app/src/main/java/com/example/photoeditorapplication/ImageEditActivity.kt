package com.example.photoeditorapplication

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ImageEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_edit)

        val imageView = findViewById<ImageView>(R.id.selectedImageView)
        val closeButton = findViewById<ImageView>(R.id.closeButton)
        val imageUri: Uri? = intent.getParcelableExtra("imageUri")
        imageView.setImageURI(imageUri)

        closeButton.setOnClickListener {
            finish()
        }
    }
}
