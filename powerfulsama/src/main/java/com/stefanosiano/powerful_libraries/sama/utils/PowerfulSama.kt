package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.Signature
import android.os.Bundle
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.view.SamaIntent
import java.lang.ref.WeakReference

/** Class to initialize the SAMA library. */
object PowerfulSama {
    internal var logger: PowerfulSamaLogger? = null
    internal var isAppDebug: Boolean = false
    internal lateinit var applicationContext: Context

    /** Weak reference to the current activity. */
    private var currentActivity: WeakReference<Activity>? = null

    /**
     * Initializes the SAMA library.
     *
     * [application] needed to initialize the library
     * [defaultMessagesTheme] is used as theme for all messages
     * [defaultMessageCustomization] is used as customization function called after the message
     *  has been shown. Note: It will be called on UI thread
     * [defaultYeslabel] Default "Yes" text
     * [defaultNolabel] Default "No" text
     * [logger] Logger used internally for base Sama classes
     * [checkSignatureFunction] Function to check whether the signatures of the app are correct
     * [onSignatureChackFailed] Function to run if the signatures of the app are NOT correct
     *  (will be run by [SamaSignature.checkSignatures])
     */
    @Suppress("LongParameterList")
    fun init(
        application: Application,
        isDebug: Boolean,
        defaultMessagesTheme: Int? = null,
        defaultMessageCustomization: ((Any) -> Unit)? = null,
        defaultYeslabel: Int = android.R.string.ok,
        defaultNolabel: Int = android.R.string.cancel,
        logger: PowerfulSamaLogger? = null,
        checkSignatureFunction: ((Array<Signature>) -> Boolean)? = null,
        onSignatureChackFailed: ((Array<Signature>) -> Unit)? = null
    ) {
        applicationContext = application
        isAppDebug = isDebug
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) { setCurrentActivity(activity) }
                override fun onActivityStarted(activity: Activity) { setCurrentActivity(activity) }
                override fun onActivityDestroyed(activity: Activity) { clearIntent(activity) }
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    setCurrentActivity(activity)
                }
            }
        )

        Res.setApplicationContext(application)
        Msg.defaultTheme = defaultMessagesTheme
        Msg.defaultCustomization = defaultMessageCustomization
        Msg.defaultYes = defaultYeslabel
        Msg.defaultNo = defaultNolabel
        PowerfulSama.logger = logger

        if (checkSignatureFunction != null && onSignatureChackFailed != null) {
            SamaSignature.init(checkSignatureFunction, onSignatureChackFailed)
        }
    }

    /** Clears the intent used to start an activity. */
    private fun clearIntent(activity: Activity?) = activity?.let {
        if (it is SamaActivity) {
            SamaIntent.clear("${it.samaIntent.uid} ")
        }
    }

    /** Sets the current activity on which to show the messages. */
    private fun setCurrentActivity(activity: Activity?) = activity?.let {
        currentActivity?.clear()
        currentActivity = WeakReference(activity)
    }

    /**
     * Get the current activity as a weak reference. Can be null if no activities are running
     *  (e.g. in services, broadcast receivers, threads finishing after activity's onDestroy, etc).
     */
    fun getCurrentActivity(): Activity? = currentActivity?.get()
}

/** Class used to log messages from the library. */
interface PowerfulSamaLogger {
    /** Log verbose messages. */
    fun logVerbose(clazz: Class<*>, message: String)

    /** Log debug messages. */
    fun logDebug(clazz: Class<*>, message: String)

    /** Log info messages. */
    fun logInfo(clazz: Class<*>, message: String)

    /** Log warning messages. */
    fun logWarning(clazz: Class<*>, message: String)

    /** Log error messages. */
    fun logError(clazz: Class<*>, message: String)

    /** Log exceptions. */
    fun logException(clazz: Class<*>, t: Throwable)

    /** Log exceptions with a built-in workaround for them. */
    fun logExceptionWorkarounded(clazz: Class<*>, t: Throwable)
}
