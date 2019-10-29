package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.util.SparseArray
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import android.net.Uri.fromParts
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.content.Intent


object Perms {

    /** Weak reference to the current activity */
    private var currentActivity : WeakReference<Activity>? = null

    private val permHelperMap = SparseArray<PermHelper>()

    private val requestCodes = AtomicInteger(42000)

    fun onRequestPermissionsResult(activity: Activity, requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {

        val permHelper = permHelperMap[requestCode] ?: return false
        if(!permHelper.isCallingResult) return false
        permHelperMap.remove(requestCode)
        return permHelper.onRequestPermissionsResult(activity, permissions, grantResults)
    }


    fun askPermissions(perms: List<String>,
             onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             onPermanentlyDenied: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             optionalPerms: List<String> = ArrayList(),
             f: () -> Unit): Boolean {

        val reqCode = requestCodes.incrementAndGet()
        val permHelper = PermHelper(currentActivity!!, reqCode, optionalPerms, onShowRationale, onPermanentlyDenied, f)
        permHelperMap.put(reqCode, permHelper)
        val shouldAsk = permHelper.askPermissions(perms.distinct())
        if(shouldAsk) return true
        permHelperMap.remove(reqCode)
        return false
    }

    fun askPermissions(perms: List<String>, rationaleId: Int, onPermanentlyDenied: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             optionalPerms: List<String> = ArrayList(), f: () -> Unit) = askPermissions(perms,
        { permissions, activity, requestCode -> Msg.alertDialog().message(rationaleId).negative(android.R.string.cancel)
            .onOk(android.R.string.ok) { ActivityCompat.requestPermissions(activity, permissions, requestCode) }.show(activity) },
        onPermanentlyDenied, optionalPerms, f)



    fun askPermissions(perms: List<String>, onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             permanentlyDeniedId: Int, optionalPerms: List<String> = ArrayList(), f: () -> Unit) =
        askPermissions(perms, onShowRationale,
        { perms, activity, requestCode ->
            Msg.alertDialog().message(permanentlyDeniedId).negative(android.R.string.cancel)
                .onOk(android.R.string.ok) {
                    val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(fromParts("package", activity.packageName, null))
                    activity.startActivityForResult(intent, requestCode)
                }.show(activity)
        },
        optionalPerms, f)


    fun askPermissions(perms: List<String>, rationaleId: Int, permanentlyDeniedId: Int, optionalPerms: List<String> = ArrayList(), f: () -> Unit) =
        askPermissions(perms,
            { permissions, activity, requestCode -> Msg.alertDialog().message(rationaleId).negative(android.R.string.cancel)
                .onOk(android.R.string.ok) { ActivityCompat.requestPermissions(activity, permissions, requestCode) }.show(activity) },
            permanentlyDeniedId, optionalPerms, f)



    fun call(perms: List<String>,
             onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             onPermanentlyDenied: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             optionalPerms: List<String> = ArrayList(),
             f: () -> Unit) {
        if(!askPermissions(perms, onShowRationale, onPermanentlyDenied, optionalPerms, f)) f()
    }

    fun call(perms: List<String>, rationaleId: Int, onPermanentlyDenied: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             optionalPerms: List<String> = ArrayList(), f: () -> Unit) {
        if(!askPermissions(perms, rationaleId, onPermanentlyDenied, optionalPerms, f)) f()
    }


    fun call(perms: List<String>,
             onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             permanentlyDeniedId: Int, optionalPerms: List<String> = ArrayList(), f: () -> Unit) {
        if(!askPermissions(perms, onShowRationale, permanentlyDeniedId, optionalPerms, f)) f()
    }


    fun call(perms: List<String>, rationaleId: Int, permanentlyDeniedId: Int, optionalPerms: List<String> = ArrayList(), f: () -> Unit) {
        if(!askPermissions(perms, rationaleId, permanentlyDeniedId, optionalPerms, f)) f()
    }




    private class PermHelper(
        private val activityReference: WeakReference<Activity>,
        private val requestCode: Int,
        private val optionalPermissions: List<String>,
        private val onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
        private val onPermanentlyDenied: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
        private val onGranted: () -> Unit) {

        var isCallingResult = false

        fun askPermissions(permissions: List<String>): Boolean {

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
                onShowRationale(permissionsToRationale.toTypedArray(), activity, requestCode)
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
                    if (!optionalPermissions.contains(permission)) {
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
                onPermanentlyDenied(deniedPermissions.toTypedArray(), activity, requestCode)
                return true
            }

            if (shouldShowRationale) {
                onShowRationale(rationalePermissions.toTypedArray(), activity, requestCode)
                return true
            }

            onGranted()
            return true
        }
    }
}