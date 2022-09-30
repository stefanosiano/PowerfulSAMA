package com.stefanosiano.powerful_libraries.sama.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

@Suppress("UnusedPrivateClass")
private class AndroidExtensions

/**
 * Return a copy of the file retrieved through the uri using Android providers
 * into app's internal cache directory, using [fileName].
 */
fun Uri.toFileFromProviders(context: Context, fileName: String): File? =
    tryOrNull {
        val f = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(this)?.use { it.into(FileOutputStream(f)) }
        f
    } ?: tryOrNull { File(path) }

/**
 * Retrieves the activity this context is associated with.
 * If no activity is found (e.g. activity destroyed, service, etc.) returns null.
 */
fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
