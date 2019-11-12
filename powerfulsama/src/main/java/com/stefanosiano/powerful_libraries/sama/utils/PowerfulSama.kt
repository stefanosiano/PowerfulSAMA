package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.Signature
import android.os.Bundle
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.view.SamaIntent

object PowerfulSama {
//todo add annotation to add scripts
    internal var logger: PowerfulSamaLogger? = null
    internal var isAppDebug: Boolean = false
    internal lateinit var applicationContext: Context

    /** Initializes the SAMA library
     *
     * [application] needed to initialize the library
     * [defaultMessagesTheme] is used as theme for all messages
     * [defaultMessageCustomization] is used as customization function called after the message has been shown. Note: It will be called on UI thread
     * [defaultYeslabel] Default "Yes" text
     * [defaultNolabel] Default "No" text
     * [logger] Logger used internally for base Sama classes
     * [checkSignatureFunction] Function to check whether the signatures of the app are correct
     * [onSignatureChackFailed] Function to run if the signatures of the app are NOT correct (will be run by [SamaSignature.checkSignatures])
     */
    fun init(
        application: Application,
        isDebug: Boolean,
        defaultMessagesTheme: Int? = null,
        defaultMessageCustomization: ((Any) -> Unit)? = null,
        defaultYeslabel: Int = android.R.string.yes,
        defaultNolabel: Int = android.R.string.no,
        logger: PowerfulSamaLogger? = null,
        checkSignatureFunction: ((Array<Signature>) -> Boolean)? = null,
        onSignatureChackFailed: ((Array<Signature>) -> Unit)? = null) {

        applicationContext = application
        isAppDebug = isDebug
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
        PowerfulSama.logger = logger

        if(checkSignatureFunction != null && onSignatureChackFailed != null)
            SamaSignature.init(checkSignatureFunction, onSignatureChackFailed)
    }

    /** Clears the intent used to start an activity */
    private fun clearIntent(activity: Activity?) = activity?.let { if(it is SamaActivity) SamaIntent.clear("${it.samaIntent.uid} ") }

    /** Sets the current activity on which to show the messages */
    private fun setMsgActivity(activity: Activity?) = activity?.let { Msg.setCurrentActivity(it) }

    /** Sets the current activity on which to show the messages */
    private fun setResActivity(activity: Activity?) = activity?.let { Res.setCurrentActivity(it) }
}

interface PowerfulSamaLogger {
    fun logVerbose(clazz: Class<*>, message: String)
    fun logDebug(clazz: Class<*>, message: String)
    fun logInfo(clazz: Class<*>, message: String)
    fun logWarning(clazz: Class<*>, message: String)
    fun logError(clazz: Class<*>, message: String)
    fun logException(clazz: Class<*>, t: Throwable)
    fun logExceptionWorkarounded(clazz: Class<*>, t: Throwable)
}
