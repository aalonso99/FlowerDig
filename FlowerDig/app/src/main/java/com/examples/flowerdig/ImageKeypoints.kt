package com.examples.flowerdig

import android.util.Log

class ImageKeypoints(val keypoints: List<Point>, val imageWidth: Int = 0, val imageHeight: Int = 0) {
    fun resize(newWidth: Int, newHeight: Int): ImageKeypoints {

        if (imageHeight==0 || imageWidth==0){
            Log.w("Not resizing keypoints", "Original width or height undefined!")

            return this

        }else

            return ImageKeypoints(keypoints.map { point ->
                                                Point(
                                                    (point.x * newWidth).div(imageWidth),
                                                    (point.y * newHeight).div(imageHeight)
                                                )
                                            },
                              newWidth, newHeight)
    }
}

data class Point(val x: Float, val y: Float)