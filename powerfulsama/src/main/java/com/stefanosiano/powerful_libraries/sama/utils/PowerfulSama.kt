package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.view.SamaIntent
import java.lang.Exception

object PowerfulSama {

    internal var onExceptionWorkarounded: ((clazz: Class<*>, e: Exception) -> Unit)? = null
    /** Initializes the SAMA library
     *
     * [application] needed to initialize the library
     * [defaultMessagesTheme] is used as theme for all messages
     * [defaultMessageCustomization] is used as customization function called after the message has been shown. Note: It will be called on UI thread
     * [defaultYeslabel] Default "Yes" text
     * [defaultNolabel] Default "No" text
     */
    fun init(application: Application, defaultMessagesTheme: Int? = null, defaultMessageCustomization: ((Any) -> Unit)? = null, defaultYeslabel: Int = android.R.string.yes, defaultNolabel: Int = android.R.string.no) {

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {}
            override fun onActivityResumed(activity: Activity?) { setMsgActivity(activity); setResActivity(activity) }
            override fun onActivityStarted(activity: Activity?) { setMsgActivity(activity); setResActivity(activity) }
            override fun onActivityDestroyed(activity: Activity?) { clearIntent(activity) }
            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
            override fun onActivityStopped(activity: Activity?) {}
            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) { setMsgActivity(activity); setResActivity(activity) }
        })

        Res.setApplicationContext(application)
        Msg.defaultTheme = defaultMessagesTheme
        Msg.defaultCustomization = defaultMessageCustomization
        Msg.defaultYes = defaultYeslabel
        Msg.defaultNo = defaultNolabel
    }

    /** Clears the intent used to start an activity */
    private fun clearIntent(activity: Activity?) = activity?.let { if(it is SamaActivity) SamaIntent.clear("${it.samaIntent.uid} ") }

    /** Sets the current activity on which to show the messages */
    private fun setMsgActivity(activity: Activity?) = activity?.let { Msg.setCurrentActivity(it) }

    /** Sets the current activity on which to show the messages */
    private fun setResActivity(activity: Activity?) = activity?.let { Res.setCurrentActivity(it) }

    fun onExceptionWorkarounded(f: (clazz: Class<*>, e: Exception) -> Unit) { onExceptionWorkarounded = f }
}