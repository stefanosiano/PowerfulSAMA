package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.util.LongSparseArray
import android.util.SparseArray
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference


object Perms {

    /** Weak reference to the current activity */
    private var currentActivity : WeakReference<Activity>? = null

    /** Application context */
    private lateinit var appContext: Context

//    private val permHelperMap = SparseArray<PermHelper>()

    fun onRequestPermissionsResult(activity: Activity, requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
/*
        val permHelper = permHelperMap[requestCode] ?: return false
        if(!permHelper.isCallingResult) return false
        permHelperMap.remove(requestCode)
        return permHelper.onRequestPermissionsResult(activity, permissions, grantResults)
        */
        return true
    }
/*

    fun call(
        perms: List<String>,
        onShowRationale: (perms: Array<String>) -> Unit = {  },
        onPermanentlyDenied: (perms: Array<String>) -> Unit = {  },
        optionalPerms: List<String> = ArrayList(),
        f: () -> Unit
    ) {
        if(PermHelper(
            currentActivity,

            ).askPermissions(perms.distinct()) )
            return
        else
            f()
    }

    interface ShowRationaleListener {
        fun onShowRationale(permissions: Array<String>, activity: Activity, requestCode: Int)
    }

    internal class SimpleOnPermissionDeniedListener : PermissionDeniedListener {

        override fun onPermissionsDenied(permissions: Array<String>) {

        }

    }

    internal class SimpleShowRationaleListener(private val rationaleId: Int) : ShowRationaleListener {

        override fun onShowRationale(permissions: Array<String>, activity: Activity, requestCode: Int) {
            val rationaleDialog = AlertDialog.Builder(activity)
            //todo set rationale message(s)!
            rationaleDialog.setTitle("Title!")
            rationaleDialog.setMessage(rationaleId)
            rationaleDialog.setPositiveButton("YES") { dialog, which -> ActivityCompat.requestPermissions(activity, permissions, requestCode) }
            rationaleDialog.setNegativeButton("NO") { dialog, which -> }
            rationaleDialog.show()
        }
    }


    private class PermHelper(

        private val activityReference: WeakReference<Activity>,
        private val requestCode: Int,
        private var optionalPermissions: List<String>? = null,
        private val onShowRationale: ((perms: Array<String>) -> Unit)? = null,
        private val onPermanentlyDenied: ((perms: Array<String>) -> Unit)? = null,
        private var onGranted: () -> Unit
    ) {

        var isCallingResult = false

        fun askPermissions(permissions: List<String>, showRationaleListener: ShowRationaleListener, onPermissionGranted: () -> Unit): Boolean {

            this.showRationaleListener = showRationaleListener
            this.onGranted = onPermissionGranted

            val activity = activityReference.get() ?: return true

            val permissionsToAsk = ArrayList<String>()
            val permissionsToRationale = ArrayList<String>()


            permissions
                .filter { ContextCompat.checkSelfPermission(activity.applicationContext, it) != PackageManager.PERMISSION_GRANTED }
                .forEach {
                    // Permission is not granted. Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, it)) permissionsToRationale.add(it)
                    else permissionsToAsk.add(it)// No explanation needed; request the permission
                }


            if (permissionsToRationale.isNotEmpty()) {
                permissionsToRationale.addAll(permissionsToAsk)
                showRationaleListener.onShowRationale(permissionsToRationale.toTypedArray(), activity, requestCode)
                return true
            }

            if (permissionsToAsk.isNotEmpty()) {
                isCallingResult = true
                ActivityCompat.requestPermissions(activity, permissionsToAsk.toTypedArray(), requestCode)
                return true
            }

            return false
        }


        internal fun onRequestPermissionsResult(activity: Activity, permissions: Array<out String>, grantResults: IntArray): Boolean {

            var shouldShowRationale = false
            var shouldRunDenied = false

            val permOptional = optionalPermissions ?: ArrayList()

            val deniedPermissions = ArrayList<String>()
            val rationalePermissions = ArrayList<String>()

            permissions.forEachIndexed { i, permission ->

                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission

                    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

                    //if at least 1 permission needs rationale, I must show it!
                    shouldShowRationale = shouldShowRationale || showRationale

                    // permission is optional or needed. user did NOT check "never ask again": show rationale and ask it
                    if (showRationale) rationalePermissions.add(permission)

                    //if permission is optional and (!showRationale) -> user CHECKED "never ask again", but permission is optional, so function can be called anyway

                    //if permission is needed
                    if (!permOptional.contains(permission)) {
                        // user CHECKED "never ask again": open another dialog explaining again the permission and directing to the app setting
                        if (!showRationale)  {
                            shouldRunDenied = true
                            deniedPermissions.add(permission)
                        }
                    }
                }
            }

            deniedPermissions.addAll(rationalePermissions)

            if (shouldRunDenied) {
                onPermissionDenied.invoke(deniedPermissions.toTypedArray())
                return true
            }
            if (shouldShowRationale) {
                showRationaleListener.onShowRationale(rationalePermissions.toTypedArray(), activity, requestCode)
                return true
            }

            onGranted()
            return true
        }
    }*/
}