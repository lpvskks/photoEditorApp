package com.example.photoeditorapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class CubeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var angleX = 0f
    private var angleY = 0f
    private var angleZ = 0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        strokeWidth = 5f
        textSize = 40f
        isAntiAlias = true
    }

    private val vertices = arrayOf(
        floatArrayOf(-1f, -1f, -1f),
        floatArrayOf(1f, -1f, -1f),
        floatArrayOf(1f, 1f, -1f),
        floatArrayOf(-1f, 1f, -1f),
        floatArrayOf(-1f, -1f, 1f),
        floatArrayOf(1f, -1f, 1f),
        floatArrayOf(1f, 1f, 1f),
        floatArrayOf(-1f, 1f, 1f)
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val widthMid = width / 2f
        val heightMid = height / 2f
        val scale = width.coerceAtLeast(height) / 8f

        val transformedVertices = vertices.map { rotate(it) }

        val path = android.graphics.Path()
        fun moveToVertex(i: Int) {
            val v = transformedVertices[i]
            path.moveTo(widthMid + v[0] * scale, heightMid + v[1] * scale)
        }

        fun lineToVertex(i: Int) {
            val v = transformedVertices[i]
            path.lineTo(widthMid + v[0] * scale, heightMid + v[1] * scale)
        }

        moveToVertex(0)
        lineToVertex(1)
        lineToVertex(2)
        lineToVertex(3)
        lineToVertex(0)

        moveToVertex(4)
        lineToVertex(5)
        lineToVertex(6)
        lineToVertex(7)
        lineToVertex(4)

        moveToVertex(0)
        lineToVertex(4)

        moveToVertex(1)
        lineToVertex(5)

        moveToVertex(2)
        lineToVertex(6)

        moveToVertex(3)
        lineToVertex(7)

        canvas.drawPath(path, paint)

        val faceCenters = listOf(
            floatArrayOf(0f, 0f, -1f),
            floatArrayOf(0f, 0f, 1f),
            floatArrayOf(-1f, 0f, 0f),
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, -1f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val faceNumbers = listOf("1", "2", "3", "4", "5", "6")

        faceCenters.forEachIndexed { index, center ->
            val transformedCenter = rotate(center)
            canvas.drawText(
                faceNumbers[index],
                widthMid + transformedCenter[0] * scale - 10,
                heightMid + transformedCenter[1] * scale + 10,
                textPaint
            )
        }
    }

    private fun rotate(vertex: FloatArray): FloatArray {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)

        var x = vertex[0]
        var y = vertex[1]
        var z = vertex[2]

        var newY = y * cosX - z * sinX
        var newZ = y * sinX + z * cosX
        y = newY
        z = newZ

        var newX = x * cosY + z * sinY
        newZ = -x * sinY + z * cosY
        x = newX
        z = newZ

        newX = x * cosZ - y * sinZ
        newY = x * sinZ + y * cosZ
        x = newX
        y = newY

        return floatArrayOf(x, y, z)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                angleX += dy / 100
                angleY += dx / 100

                lastTouchX = event.x
                lastTouchY = event.y

                invalidate()
            }
        }
        return true
    }

}
