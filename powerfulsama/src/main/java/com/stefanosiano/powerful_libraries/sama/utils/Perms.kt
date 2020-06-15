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
import com.stefanosiano.powerful_libraries.sama.contains
import com.stefanosiano.powerful_libraries.sama.logError
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama.applicationContext
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity

/** Utility class to manage permissions */
object Perms {

    /** Permissions requested in the manifest of the app */
    private val requestedPermissions by lazy { applicationContext.let { it.packageManager.getPackageInfo(it.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions } ?: arrayOf<String>() }

    /** Array of helpers for asking permissions */
    private val permHelperMap = SparseArray<PermHelper>()

    /** Request codes used to pass to activity's onRequestPermissionsResult method */
    private val requestCodes = AtomicInteger(42000)

    /** Manages the permissions request results. Activities extending [SamaActivity] already call it */
    fun onRequestPermissionsResult(activity: Activity, requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        val permHelper = permHelperMap[requestCode] ?: return false
        if(!permHelper.isCallingResult) return false
        permHelperMap.remove(requestCode)
        return permHelper.onRequestPermissionsResult(activity, permissions, grantResults)
    }

    /** Checks if all [perms] are granted. Return true only if all of them are granted */
    fun hasPermissions(perms: List<String>): Boolean =
        perms.all { ActivityCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED }

    /** Checks if [perm] is granted */
    fun hasPermissions(perm: String): Boolean =
        ActivityCompat.checkSelfPermission(applicationContext, perm) == PackageManager.PERMISSION_GRANTED


    /** Ask for [perms] and [optionalPerms] (if they are not already granted). If they are asked and then granted, [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale, so [onShowRationale] will be called.
     * If "Don't ask again" was checked when denying a permission, [onPermanentlyDenied] will be called, with all permanently denied permissions and
     *  permissions needing the rationale to show passed as param. Permanently denied permissions will never include [optionalPerms]
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored and don't show any message
     * permissions passed multiple times will be ignored. Optional permissions already included in needed permissions will be ignored.
     *
     * Returns true if a permission has to be asked, false otherwise
     */
    fun askPermissions(perms: List<String>,
                       onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
                       onPermanentlyDenied: (permanentlyDeniedPerms: Array<String>, showRationalePerms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
                       optionalPerms: List<String> = ArrayList(),
                       f: () -> Unit): Boolean {

        if(PowerfulSama.isAppDebug && !requestedPermissions.contains(perms)) {
            Msg.alertDialog().message("Warning: The requested permissions are not defined in the manifest. This is only a debug message. Release version will fail silently (function will not be called)").show()
            logError("Warning: The requested permissions are not defined in the manifest. This is only a debug message. Release version will fail silently (function will not be called)")
            return false
        }

        val reqCode = requestCodes.incrementAndGet()
        val permHelper = PermHelper(reqCode, optionalPerms.minus(perms).distinct(), onShowRationale, onPermanentlyDenied, f)
        permHelperMap.put(reqCode, permHelper)
        val shouldAsk = permHelper.askPermissions(perms.distinct())
        if(shouldAsk) return true
        permHelperMap.remove(reqCode)
        return false
    }

    /** Ask for [perms] and [optionalPerms] (if they are not already granted). If they are asked and then granted, [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale, so a message with [rationaleId] will be shown.
     * If "Don't ask again" was checked when denying a permission, [onPermanentlyDenied] will be called, with all permanently denied permissions and
     *  permissions needing the rationale to show passed as param. Permanently denied permissions will never include [optionalPerms]
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored and don't show any message
     * permissions passed multiple times will be ignored. Optional permissions already included in needed permissions will be ignored.
     *
     * Returns true if a permission has to be asked, false otherwise
     */
    fun askPermissions(perms: List<String>, rationaleId: Int, onPermanentlyDenied: (permanentlyDeniedPerms: Array<String>, showRationalePerms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
                       optionalPerms: List<String> = ArrayList(), f: () -> Unit) =
        askPermissions(perms,
            { permissions, activity, requestCode ->
                Msg.alertDialog().message(rationaleId).negative(android.R.string.cancel)
                    .onOk(android.R.string.ok) { ActivityCompat.requestPermissions(activity, permissions, requestCode) }.show(activity)
            },
            onPermanentlyDenied, optionalPerms, f
        )



    /** Ask for [perms] and [optionalPerms] (if they are not already granted). If they are asked and then granted, [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale, so [onShowRationale] will be called.
     * If "Don't ask again" was checked when denying a permission, a message with [permanentlyDeniedId] will be shown, sending to app settings to manually grant permissions
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored and don't show any message
     * permissions passed multiple times will be ignored. Optional permissions already included in needed permissions will be ignored.
     *
     * Returns true if a permission has to be asked, false otherwise
     */
    fun askPermissions(perms: List<String>, onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
                       permanentlyDeniedId: Int, optionalPerms: List<String> = ArrayList(), f: () -> Unit) =
        askPermissions(perms, onShowRationale,
            { permanentlyDeniedPerms, showRationalePerms, activity, requestCode ->
                Msg.alertDialog().message(permanentlyDeniedId).negative(android.R.string.cancel).onOk(android.R.string.ok) {
                    val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS).setData(fromParts("package", activity.packageName, null))
                    activity.startActivityForResult(intent, requestCode)
                }.show(activity)
            },
            optionalPerms, f)


    /** Ask for [perms] and [optionalPerms] (if they are not already granted). If they are asked and then granted, [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale, so a message with [rationaleId] will be shown.
     * If "Don't ask again" was checked when denying a permission, a message with [permanentlyDeniedId] will be shown, sending to app settings to manually grant permissions
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored and don't show any message
     * permissions passed multiple times will be ignored. Optional permissions already included in needed permissions will be ignored.
     *
     * Returns true if a permission has to be asked, false otherwise
     */
    fun askPermissions(perms: List<String>, rationaleId: Int, permanentlyDeniedId: Int, optionalPerms: List<String> = ArrayList(), f: () -> Unit) =
        askPermissions(perms,
            { permissions, activity, requestCode -> Msg.alertDialog().message(rationaleId).negative(android.R.string.cancel)
                .onOk(android.R.string.ok) { ActivityCompat.requestPermissions(activity, permissions, requestCode) }.show(activity) },
            permanentlyDeniedId, optionalPerms, f)



    /** Ask for [perms] and [optionalPerms] (if they are not already granted). If they are already granted or they are granted now [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale, so [onShowRationale] will be called.
     * If "Don't ask again" was checked when denying a permission, [onPermanentlyDenied] will be called, with all permanently denied permissions and
     *  permissions needing the rationale to show passed as param. Permanently denied permissions will never include [optionalPerms]
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored and don't show any message
     * permissions passed multiple times will be ignored. Optional permissions already included in needed permissions will be ignored. */
    fun call(perms: List<String>,
             onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             onPermanentlyDenied: (permanentlyDeniedPerms: Array<String>, showRationalePerms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             optionalPerms: List<String> = ArrayList(),
             f: () -> Unit) {
        if(!askPermissions(perms, onShowRationale, onPermanentlyDenied, optionalPerms, f)) f()
    }


    /** Ask for [perms] and [optionalPerms] (if they are not already granted). If they are already granted or they are granted now [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale, so a message with [rationaleId] will be shown.
     * If "Don't ask again" was checked when denying a permission, [onPermanentlyDenied] will be called, with all permanently denied permissions and
     *  permissions needing the rationale to show passed as param. Permanently denied permissions will never include [optionalPerms]
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored and don't show any message
     * permissions passed multiple times will be ignored. Optional permissions already included in needed permissions will be ignored. */
    fun call(perms: List<String>, rationaleId: Int, onPermanentlyDenied: (permanentlyDeniedPerms: Array<String>, showRationalePerms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             optionalPerms: List<String> = ArrayList(), f: () -> Unit) {
        if(!askPermissions(perms, rationaleId, onPermanentlyDenied, optionalPerms, f)) f()
    }


    /** Ask for [perms] and [optionalPerms] (if they are not already granted). If they are already granted or they are granted now [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale, so [onShowRationale] will be called.
     * If "Don't ask again" was checked when denying a permission, a message with [permanentlyDeniedId] will be shown, sending to app settings to manually grant permissions
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored and don't show any message
     * permissions passed multiple times will be ignored. Optional permissions already included in needed permissions will be ignored. */
    fun call(perms: List<String>,
             onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
             permanentlyDeniedId: Int, optionalPerms: List<String> = ArrayList(), f: () -> Unit) {
        if(!askPermissions(perms, onShowRationale, permanentlyDeniedId, optionalPerms, f)) f()
    }


    /** Ask for [perms] and [optionalPerms] (if they are not already granted). If they are already granted or they are granted now [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale, so a message with [rationaleId] will be shown.
     * If "Don't ask again" was checked when denying a permission, a message with [permanentlyDeniedId] will be shown, sending to app settings to manually grant permissions
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored and don't show any message
     * permissions passed multiple times will be ignored. Optional permissions already included in needed permissions will be ignored. */
    fun call(perms: List<String>, rationaleId: Int, permanentlyDeniedId: Int, optionalPerms: List<String> = ArrayList(), f: () -> Unit) {
        if(!askPermissions(perms, rationaleId, permanentlyDeniedId, optionalPerms, f)) f()
    }




    private class PermHelper(
        private val requestCode: Int,
        private val optionalPermissions: List<String>,
        private val onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
        private val onPermanentlyDenied: (permanentlyDeniedPerms: Array<String>, showRationalePerms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
        private val onGranted: () -> Unit) {

        var isCallingResult = false

        fun askPermissions(permissions: List<String>): Boolean {

            val activity = PowerfulSama.getCurrentActivity() ?: return true

            val permissionsToAsk = ArrayList<String>()
            val permissionsToRationale = ArrayList<String>()

            permissions.plus(optionalPermissions)
                .filter { ContextCompat.checkSelfPermission(activity.applicationContext, it) != PackageManager.PERMISSION_GRANTED }
                .forEach {
                    // Permission is not granted. Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, it)) permissionsToRationale.add(it)
                    else permissionsToAsk.add(it)// No explanation needed; request the permission
                }

            if (permissionsToRationale.isNotEmpty()) {
                isCallingResult = true
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

            val deniedPermissions = ArrayList<String>()
            val rationalePermissions = ArrayList<String>()

            permissions.forEachIndexed { i, permission ->

                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission

                    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

                    // permission is optional or needed. user did NOT check "never ask again": show rationale and ask it
                    if (showRationale) rationalePermissions.add(permission)

                    //if permission is optional and (!showRationale) -> user CHECKED "never ask again", but permission is optional, so function can be called anyway

                    //if permission is needed
                    if (!optionalPermissions.contains(permission)) {
                        // user CHECKED "never ask again": open another dialog explaining again the permission and directing to the app setting
                        if (!showRationale) deniedPermissions.add(permission)
                    }
                }
            }

            //if at least 1 needed permission was permanently denied, I need to ask for it
            if (deniedPermissions.isNotEmpty()) {
                onPermanentlyDenied(deniedPermissions.toTypedArray(), rationalePermissions.toTypedArray(), activity, requestCode)
                return true
            }

            //if at least 1 permission needs rationale, I must show it!
            if (rationalePermissions.isNotEmpty()) {
                onShowRationale(rationalePermissions.toTypedArray(), activity, requestCode)
                return true
            }

            onGranted()
            return true
        }
    }
}