package com.stefanosiano.powerful_libraries.sama

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import java.io.File

const val ExtraDbName = "extra_db_name"

const val ActionDbCopy = "com.stefanosiano.powerful_libraries.sama.action.ACTION_COPY_DB"

class SamaDebugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            ActionDbCopy -> {
                val isDebuggable = 0 != context?.applicationInfo?.flags?.and(ApplicationInfo.FLAG_DEBUGGABLE)
                if(!isDebuggable) return
                context ?: return
                val dbName = intent.extras?.getString(ExtraDbName) ?: return logError("No database name specified")
                val db = context.getDatabasePath(dbName)
                val dbTo = File(context.getExternalFilesDir("samaDebug"), dbName)

                val wal = context.getDatabasePath("$dbName-wal")
                val toWal = File(context.getExternalFilesDir("samaDebug"), "$dbName-wal")

                val shm = context.getDatabasePath("$dbName-shm")
                val toShm = File(context.getExternalFilesDir("samaDebug"), "$dbName-shm")

                tryOrPrint { db.copyTo(dbTo, true); logDebug("database copied from ${db.absolutePath} to ${dbTo.absolutePath}") }
                if(wal.exists()) tryOrPrint { wal.copyTo(toWal, true); logDebug("database wal copied to ${toWal.absolutePath}") }
                if(shm.exists()) tryOrPrint { shm.copyTo(toShm, true); logDebug("database shm copied to ${toShm.absolutePath}") }

            }
        }
    }
}