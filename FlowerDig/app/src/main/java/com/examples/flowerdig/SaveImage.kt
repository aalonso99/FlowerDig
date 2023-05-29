package com.examples.flowerdig

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.core.content.ContentProviderCompat.requireContext
import java.io.*

fun saveImage(title: String, context: Context, bitmap: Bitmap, activity: Activity? = null): Uri {
//        var outStream: FileOutputStream? = null
    var outStream: OutputStream? = null
    var uri: Uri? = null

    // Write to SD Card
    try {

        if (Build.VERSION.SDK_INT >= 30) {
            val content = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, title)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
//                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            context.contentResolver.also { resolver ->
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content)
                outStream = resolver.openOutputStream(uri!!)
            }
        }else {
            MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, title, null)
        }

        outStream.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream?.flush()
        }

        activity?.runOnUiThread {
            Toast.makeText(context, "Image Saved Successfully", Toast.LENGTH_LONG).show()
        }

    } catch (e: FileNotFoundException) {
        Log.d("TAG", "saveImage: ${e.message}")
    } catch (e: IOException) {
        Log.d("TAG", "saveImage: ${e.message}")
    }

    return uri!!
}