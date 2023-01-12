package com.stefanosiano.powerful_libraries.sama_sample

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.multidex.MultiDex
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSamaLogger

class SampleApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        PowerfulSama.init(
            application = this,
            isDebug = BuildConfig.DEBUG,
            defaultMessagesTheme = R.style.AppTheme,
            defaultMessageCustomization = null,
            defaultYeslabel = android.R.string.ok,
            defaultNolabel = android.R.string.no,
            logger = object : PowerfulSamaLogger {
                override fun logDebug(clazz: Class<*>, message: String) { Log.d(clazz.simpleName, message) }
                override fun logError(clazz: Class<*>, message: String) { Log.e(clazz.simpleName, message) }
                override fun logException(clazz: Class<*>, t: Throwable) { t.printStackTrace() }
                override fun logExceptionWorkarounded(clazz: Class<*>, t: Throwable) { t.printStackTrace() }
                override fun logInfo(clazz: Class<*>, message: String) { Log.i(clazz.simpleName, message) }
                override fun logVerbose(clazz: Class<*>, message: String) { Log.v(clazz.simpleName, message) }
                override fun logWarning(clazz: Class<*>, message: String) { Log.w(clazz.simpleName, message) }
            }
        )
    }
}
