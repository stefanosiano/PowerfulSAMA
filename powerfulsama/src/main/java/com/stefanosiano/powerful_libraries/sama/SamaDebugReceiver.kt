package com.stefanosiano.powerful_libraries.sama

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

const val ExtraDbName = "extra_db_name"

class SamaDebugReceiver : BroadcastReceiver() {

    //todo annotation to create pullDb script from db name
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            "com.stefanosiano.powerful_libraries.sama.action.ACTION_COPY_DB" -> {
                if(!BuildConfig.DEBUG) return
                context ?: return
                val dbName = intent.extras?.getString(ExtraDbName) ?: return logError("No database name specified")
                val db = context.getDatabasePath(dbName)
                val dbTo = File(context.getExternalFilesDir("samaDebug"), dbName)
                logDebug("database copied from ${db.absolutePath} to ${dbTo.absolutePath}")
                db.copyTo(dbTo, true)
            }
        }
    }
}