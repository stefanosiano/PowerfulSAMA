package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableInt
import java.lang.ref.WeakReference


/** Class that retrieves resources using current activity context or application context if no activity is available */
object Res {

    /** Weak reference to the current activity */
    private var currentActivity : WeakReference<Activity>? = null

    /** Application context */
    private lateinit var appContext: Context

    /** Set the application context, used as fallback for methods */
    internal fun setApplicationContext(application: Application) { appContext = application.applicationContext }

    /** Set [activity] as the current activity (as a weak reference) */
    internal fun setCurrentActivity(activity: Activity) { currentActivity?.clear(); currentActivity = WeakReference(activity) }

    /** [ObservableInt] containing an user defined id of the current theme in use. Call [changeTheme] to update its value */
    val theme = ObservableInt()

    /** Updates the value of [theme] with [themeId], to allow to catch theme changes on non-context classes */
    fun changeTheme(themeId: Int) { theme.set(themeId) }

    /** Retrieve a color based on the current theme. NOTE: calling this method when there is no visible activity will use application context, losing theme information */
    fun color(resourceId: Int, context: Context? = null): Int = (context ?: currentActivity?.get() ?: appContext).let { ContextCompat.getColor(it, resourceId) }

    /** Retrieve a string from resources using application context. Can be used anywhere */
    fun string(resourceId: Int, vararg args: Any): String = appContext.getString(resourceId, args)

    /** Retrieve a drawable from resources based on the current theme. NOTE: calling this method when there is no visible activity will use application context, losing theme information */
    fun drawable(resourceId: Int, context: Context? = null): Drawable? = (context ?: currentActivity?.get() ?: appContext).let { AppCompatResources.getDrawable(it, resourceId) }

}