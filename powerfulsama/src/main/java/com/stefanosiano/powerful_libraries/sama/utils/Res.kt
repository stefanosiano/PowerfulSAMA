package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableInt
import com.stefanosiano.powerful_libraries.sama.tryOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/** Class that retrieves resources using current activity context or application context if no activity is available. */
object Res {

    /** Application context. */
    private lateinit var appContext: Context

    /** Set the application context, used as fallback for methods. */
    internal fun setApplicationContext(application: Application) { appContext = application.applicationContext }

    /** [ObservableInt] containing an user defined id of the current theme in use. Call [changeTheme] to update its value. */
    val theme = ObservableInt()

    /** Updates the value of [theme] with [themeId], to allow to catch theme changes on non-context classes. */
    fun changeTheme(themeId: Int) { theme.set(themeId) }

    /** Retrieve a color based on the current theme. NOTE: calling this method when there is no visible activity will use application context, losing theme information. */
    fun color(resourceId: Int, context: Context? = null): Int = (context ?: PowerfulSama.getCurrentActivity() ?: appContext).let {
        ContextCompat.getColor(
            it,
            resourceId
        )
    }

    /** Retrieve a string from resources using application context, passing [args]. Can be used anywhere. */
    fun string(resourceId: Int, vararg args: Any): String = appContext.getString(resourceId, *args)

    /** Retrieve a string from resources using application context. Can be used anywhere. */
    fun string(resourceId: Int): String = appContext.getString(resourceId)

    /** Retrieve a drawable from resources based on the current theme. NOTE: calling this method when there is no visible activity will use application context, losing theme information. */
    fun drawable(resourceId: Int, context: Context? = null): Drawable? = (context ?: PowerfulSama.getCurrentActivity() ?: appContext).let {
        AppCompatResources.getDrawable(
            it,
            resourceId
        )
    }

    /** Returns [dimenId] in dp. */
    fun dimensInDp(dimenId: Int, context: Context? = null) = (context ?: PowerfulSama.getCurrentActivity() ?: appContext).let {
        it.resources.getDimension(
            dimenId
        ) / it.resources.displayMetrics.density
    }

    /** Returns [dimenId] in px. */
    fun dimensInPx(dimenId: Int, context: Context? = null) = (context ?: PowerfulSama.getCurrentActivity() ?: appContext).resources.getDimension(
        dimenId
    )

    /** Returns the int bound to the [resourceId]. */
    fun integer(resourceId: Int) = appContext.resources.getInteger(resourceId)

    /** Returns an [InputStream] for the resource in the raw directory. */
    fun raw(id: Int): InputStream = appContext.resources.openRawResource(id)

    /** Writes the raw resource to a [File] in the cache directory with the name [outputName]. */
    fun rawToCache(id: Int, outputName: String): File? = appContext.resources.openRawResource(id).use { inputStream ->
        val f = File(appContext.cacheDir, outputName)
        if (f.exists()) f.delete()
        FileOutputStream(f).use { it.write(inputStream.readBytes()) }
        return f
    }

    /** Returns an [InputStream] for the resource [assetName] in the assets directory. */
    fun assets(assetName: String): InputStream = appContext.resources.assets.open(assetName)

    /** Writes the assets resource [assetName] to a [File] in the cache directory with the name [outputName]. */
    fun assetsToCache(assetName: String, outputName: String): File? = tryOrNull {
        appContext.resources.assets.open(
            assetName
        )
    }?.use { inputStream ->
        val f = File(appContext.cacheDir, outputName)
        if (f.exists()) f.delete()
        FileOutputStream(f).use { it.write(inputStream.readBytes()) }
        return f
    }
}
