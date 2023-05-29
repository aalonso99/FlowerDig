/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.examples.flowerdig

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import arrow.core.Either
import com.examples.flowerdig.ml.ModelV1
import com.examples.flowerdig.ml.ModelV2
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.DataType
import org.tensorflow.lite.TensorFlowLite.init
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.gms.vision.TfLiteVision

typealias Model = ModelV1

class KeypointDetector(
  var currentModel: Int = 0,
  var currentDelegate: Int = 0,
  val context: Context,
  val activity: Activity? = null,
  val keypointDetectorListener: KeypointDetectorListener,
  private val imageWidth: Int = 96,
  private val imageHeight: Int = 96,
  private val centerCropX: Int = 600,
  private val centerCropY: Int = 400,
  private val numKeypoints: Int = 0,
  private var rawImageHeight: Int = 0,
  private var rawImageWidth: Int = 0,
) {

    private val TAG = "KeypointDetector"
    private var outputs: FloatArray = FloatArray(0)

    //// DEBUGGING
    private var numSavedImages = 0
    private var numRuns = 0
    private var totalRunningTime = 0f
    ////

    // For this example this needs to be a var so it can be reset on changes. If the ObjectDetector
    // will not change, a lazy val would be preferable.
    private var keypointDetectionModelV1: ModelV1? = null
    private var keypointDetectionModelV2: ModelV2? = null


    init {

        TfLiteGpu.isGpuDelegateAvailable(context).onSuccessTask { gpuAvailable: Boolean ->
            val optionsBuilder =
                TfLiteInitializationOptions.builder()
            if (gpuAvailable) {
                optionsBuilder.setEnableGpuDelegateSupport(true)
            }
            TfLiteVision.initialize(context, optionsBuilder.build())
        }.addOnSuccessListener {
            keypointDetectorListener.onInitialized()
        }.addOnFailureListener{
            keypointDetectorListener.onError("TfLiteVision failed to initialize: "
                    + it.message)
        }
    }

    fun clearKeypointDetector() {
        keypointDetectionModelV1 = null
        keypointDetectionModelV2 = null
    }

    // Initialize the object detector using current settings on the
    // thread that is using it. CPU and NNAPI delegates can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the detector
    fun setupKeypointDetector() {
        if (!TfLiteVision.isInitialized()) {
            Log.e(TAG, "setupKeypointDetector: TfLiteVision is not initialized yet")
            return
        }

        try {
            when (currentModel) {
                MODEL_V1 -> keypointDetectionModelV1 = ModelV1.newInstance(context)
                MODEL_V2 -> keypointDetectionModelV2 = ModelV2.newInstance(context)
            }
        } catch (e: Exception) {
            keypointDetectorListener.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    fun detect(image: Bitmap, imageRotation: Int, correctionMatrix: Matrix = Matrix()) {
        if (!TfLiteVision.isInitialized()) {
            Log.e(TAG, "detect: TfLiteVision is not initialized yet")
            return
        }

        if (keypointDetectionModelV1 == null && keypointDetectionModelV2 == null) {
            setupKeypointDetector()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        val imageProcessor = ImageProcessor.Builder()
                                           .add(Rot90Op(-imageRotation / 90))
                                           .add(ResizeWithCropOrPadOp(centerCropY,centerCropX))
                                           .add(ResizeOp(imageHeight, imageWidth, ResizeOp.ResizeMethod.BILINEAR))
                                           .build()

        // Preprocess the image
        val tensorImage = TensorImage.fromBitmap(image)
        rawImageHeight = tensorImage.height
        rawImageWidth = tensorImage.width
//        println("width=$rawImageWidth height=$rawImageHeight")
        imageProcessor.process(tensorImage)

        //// DEBUGGING
//        if (numSavedImages < 1){
//            saveImage("flor", context, tensorImage.bitmap, activity)
//            numSavedImages += 1
//        }
        ////

        // Loads the processed image into a TensorBuffer
        val input = TensorBuffer.createFixedSize(intArrayOf(1, imageHeight, imageWidth, 3), DataType.FLOAT32) // Create tensor buffer
        val pixelsArray = FloatArray(imageHeight * imageWidth * 3)

        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val px = tensorImage.bitmap.getPixel(x, y)

                val pos = y*imageWidth+x
                pixelsArray[pos] = Color.red(px).toFloat()
                pixelsArray[pos+1] = Color.green(px).toFloat()
                pixelsArray[pos+2] = Color.blue(px).toFloat()
            }
        }

        input.loadArray(pixelsArray)

        when (currentModel) {
            MODEL_V1 -> outputs = keypointDetectionModelV1!!.process(input).
                                    outputFeature0AsTensorBuffer.floatArray
            MODEL_V2 -> outputs = keypointDetectionModelV2!!.process(input).
                                    outputFeature0AsTensorBuffer.floatArray
        }

        // DEBUG
//        printOutputs(flag="1")
        ////

        for ((i,o) in outputs.withIndex()){
            if (i%2==0){ outputs[i] = o/imageWidth*centerCropX + (rawImageWidth-centerCropX)/2 }
            else { outputs[i] = o/imageHeight*centerCropY + (rawImageHeight-centerCropY)/2 }
        }

        // DEBUG
//        printOutputs(flag="2")
        ////

        if (outputs.isNotEmpty()) {
            // Correct the point coordinates to the camera view
//            correctionMatrix.mapPoints(outputs)
//            println("Correction Matrix: $correctionMatrix")
//            println("3 -> ${outputs[0]}")
            // Create the keypoints
            val keypointList = List(numKeypoints) {
                Point(x=outputs[2 * it], y=outputs[2 * it + 1])
            }

            val results = ImageKeypoints( keypointList )
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

            // DEBUGGING
//            totalRunningTime += inferenceTime
//            numRuns += 1
//            println("Number of runs: $numRuns")
//            println("Average running time (ms): ${totalRunningTime/numRuns}")
            ////

            keypointDetectorListener.onResults(
                results,
                inferenceTime,
                rawImageHeight,
                rawImageWidth
            )
        }

    }

    private fun printOutputs(flag: String = ""){
        for ((i,o) in outputs.withIndex()){
            if (i%2==0){
                println("output $flag x=$o")
            }
            else {
                println("output $flag y=$o")}
        }
    }

    interface KeypointDetectorListener {
        fun onInitialized()
        fun onError(error: String)
        fun onResults(
          results: ImageKeypoints?,
          inferenceTime: Long,
          imageHeight: Int,
          imageWidth: Int
        )
    }

    companion object {
        const val MODEL_V1 = 0
        const val MODEL_V2 = 1
    }
}
