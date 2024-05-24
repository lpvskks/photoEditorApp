package com.example.photoeditorapplication

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class CubeActivity : AppCompatActivity() {

    private lateinit var cubesButton: Button
    private lateinit var closeButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cube)

        val cubeView: CubeView = findViewById(R.id.cubeView)

        cubesButton = findViewById(R.id.cubesButton)
        closeButton = findViewById(R.id.closeButton)

        closeButton.setOnClickListener { finish() }

        cubesButton.setOnClickListener {
            if (cubeView.visibility == View.VISIBLE) {
                cubeView.visibility = View.GONE
            } else {
                cubeView.visibility = View.VISIBLE
            }
        }
    }
}
