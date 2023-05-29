package com.examples.flowerdig

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.icu.text.CaseMap.Title
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.core.content.ContentProviderCompat.requireContext
import java.io.*

fun saveImageLegacy(title: String, context: Context, bitmap: Bitmap, activity: Activity? = null) {

    try {

        MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, title, null)

        activity?.runOnUiThread {
            Toast.makeText(context, "Image Saved Successfully", Toast.LENGTH_LONG).show()
        }

    } catch (e: FileNotFoundException) {
        Log.d("TAG", "saveImage: ${e.message}")
    } catch (e: IOException) {
        Log.d("TAG", "saveImage: ${e.message}")
    }

}