/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.examples.flowerdig

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var result = ImageKeypoints(emptyList<Point>(), 0, 0)
    private var scaleFactorX: Float = 1f
    private var scaleFactorY: Float = 1f
    private var pointPaint = Paint()
    private var textPaint = Paint()

    init {
        initPaints()
        setWillNotDraw(false)
    }

    fun clear() {
        textPaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {

        textPaint.color = Color.BLACK
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        pointPaint.color = Color.RED
        pointPaint.strokeWidth = 8F
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for ((i,point) in result.keypoints.withIndex()) {

            val x = point.x * scaleFactorX
            val y = point.y * scaleFactorY

//            println("$x $y")

            canvas.drawCircle(x, y, 3F, pointPaint)

            // Draw text with keypoint label
            canvas.drawText(i.toString(), x, y + 3F, textPaint)
        }
    }

    fun setResults(
      detectionResults: ImageKeypoints,
      imageHeight: Int,
      imageWidth: Int,
    ) {
        result = detectionResults

        scaleFactorX = width * 1f / imageWidth
        scaleFactorY = height * 1f / imageHeight
    }

}
