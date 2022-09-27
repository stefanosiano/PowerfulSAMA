package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri.fromParts
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.SparseArray
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stefanosiano.powerful_libraries.sama.extensions.contains
import com.stefanosiano.powerful_libraries.sama.extensions.logError
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama.applicationContext
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity

/** Utility class to manage permissions. */
object Perms {

    /** Permissions requested in the manifest of the app. */
    private val requestedPermissions by lazy {
        applicationContext.let {
            it.packageManager.getPackageInfo(
                it.packageName,
                PackageManager.GET_PERMISSIONS
            ).requestedPermissions
        } ?: arrayOf<String>()
    }

    /** Array of helpers for asking permissions. */
    private val permHelperMap = SparseArray<PermHelper>()

    /** Manages the permissions request results. Activities extending [SamaActivity] already call it. */
    internal fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        val permHelper = permHelperMap[requestCode]
        if (permHelper == null || !permHelper.isCallingResult) {
            return false
        }
        permHelperMap.remove(requestCode)
        return permHelper.onRequestPermissionsResult(activity, permissions, grantResults)
    }

    /** Checks if all [perms] are granted. Return true only if all of them are granted. */
    fun hasPermissions(vararg perms: String): Boolean =
        perms.all { ActivityCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED }

    /** Checks if all [perms] are granted. Return true only if all of them are granted. */
    fun hasPermissions(perms: Collection<String>): Boolean =
        perms.all { ActivityCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED }

    /** Checks if all [perms] are granted. Return true only if all of them are granted. */
    fun hasPermissions(perms: List<String>): Boolean =
        perms.all { ActivityCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED }


    /**
     * Ask for [perms] and [optionalPerms] (if they are not already granted).
     * If they are already granted or they are granted now [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale,
     *  so [onShowRationale] will be called.
     * If "Don't ask again" was checked when denying a permission, [onPermanentlyDenied] will be called,
     *  with all permanently denied permissions and permissions needing the rationale to show passed as params.
     * Permanently denied permissions will never include [optionalPerms].
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored.
     * Permissions passed multiple times will be ignored.
     * Optional permissions already included in needed permissions will be ignored.
     */
    fun call(
        perms: List<String>,
        onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
        onPermanentlyDenied: (
            permanentlyDeniedPerms: Array<String>,
            showRationalePerms: Array<String>,
            activity: Activity, requestCode: Int
        ) -> Unit,
        optionalPerms: List<String> = ArrayList(),
        f: () -> Unit
    ) {

        if (PowerfulSama.isAppDebug && (!requestedPermissions.contains(perms) || !requestedPermissions.contains(
                optionalPerms
            ))
        ) {
            val debugMessage = "Warning: The requested permissions are not defined in the manifest. " +
                    "This is only a debug message. Release version will fail silently (function will not be called)"
            Msg.alertDialog()
                .message(debugMessage)
                .show()
            logError(debugMessage)
            return
        }

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper =
            PermHelper(reqCode, optionalPerms.minus(perms.toSet()).distinct(), onShowRationale, onPermanentlyDenied, f)
        permHelperMap.put(reqCode, permHelper)
        val shouldAsk = permHelper.askPermissions(perms.distinct())
        if (shouldAsk) return
        permHelperMap.remove(reqCode)
        f()
    }


    /**
     * Ask for permission specified in [container] and its optional permissions (if they are not already granted).
     * If they are already granted or they are granted now [f] will be called.
     * In case one or more permissions were previously denied the app shows a rationale.
     * If "Don't ask again" was checked when denying a permission, [container]'s onPermanentlyDenied() will be called,
     *  with all permanently denied permissions and permissions needing the rationale to show passed as params.
     * Permanently denied permissions will never include optional permissions.
     * Optional permissions are asked together with required ones, but if they are always denied they are ignored.
     * Permissions passed multiple times will be ignored.
     * Optional permissions already included in needed permissions will be ignored.
     */
    fun call(container: PermissionContainer, f: () -> Unit) {
        when {
            container.rationaleId != null && container.permanentlyDeniedId != null -> call(
                container.perms,
                container.rationaleId,
                container.permanentlyDeniedId,
                container.optionalPerms,
                f
            )
            container.rationaleId != null -> call(
                container.perms,
                container.rationaleId,
                { permanentlyDeniedPerms, showRationalePerms, activity, requestCode ->
                    container.onPermanentlyDenied(
                        permanentlyDeniedPerms,
                        showRationalePerms,
                        activity,
                        requestCode
                    )
                },
                container.optionalPerms,
                f
            )
            container.permanentlyDeniedId != null -> call(
                container.perms,
                { perms, activity, requestCode -> container.onShowRationale(perms, activity, requestCode) },
                container.permanentlyDeniedId,
                container.optionalPerms,
                f
            )
            else -> call(
                container.perms,
                { perms, activity, requestCode -> container.onShowRationale(perms, activity, requestCode) },
                { permanentlyDeniedPerms, showRationalePerms, activity, requestCode ->
                    container.onPermanentlyDenied(
                        permanentlyDeniedPerms,
                        showRationalePerms,
                        activity,
                        requestCode
                    )
                },
                container.optionalPerms,
                f
            )
        }
    }


    /**
     * Ask for [perms] and [optionalPerms] (if they are not already granted).
     * If they are already granted or they are granted now [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale,
     *  so a message with [rationaleId] will be shown.
     * If "Don't ask again" was checked when denying a permission, [onPermanentlyDenied] will be called,
     *  with all permanently denied permissions and permissions needing the rationale to show passed as params.
     * Permanently denied permissions will never include [optionalPerms].
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored.
     * Permissions passed multiple times will be ignored.
     * Optional permissions already included in needed permissions will be ignored.
     */
    fun call(
        perms: List<String>,
        rationaleId: Int,
        onPermanentlyDenied: (
            permanentlyDeniedPerms: Array<String>,
            showRationalePerms: Array<String>,
            activity: Activity, requestCode: Int
        ) -> Unit,
        optionalPerms: List<String> = ArrayList(),
        f: () -> Unit
    ) {
        call(
            perms,
            { permissions, activity, requestCode ->
                Msg.ad(rationaleId).negative(android.R.string.cancel)
                    .onOk(android.R.string.ok) { ActivityCompat.requestPermissions(activity, permissions, requestCode) }
                    .show(activity)
            },
            onPermanentlyDenied, optionalPerms, f
        )
    }


    /**
     * Ask for [perms] and [optionalPerms] (if they are not already granted).
     * If they are already granted or they are granted now [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale,
     *  so [onShowRationale] will be called.
     * If "Don't ask again" was checked when denying a permission, a message with [permanentlyDeniedId] will be shown,
     *  sending to app settings to manually grant permissions.
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored.
     * Permissions passed multiple times will be ignored.
     * Optional permissions already included in needed permissions will be ignored.
     */
    fun call(
        perms: List<String>,
        onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
        permanentlyDeniedId: Int, optionalPerms: List<String> = ArrayList(), f: () -> Unit
    ) {
        call(perms, onShowRationale, { permanentlyDeniedPerms, showRationalePerms, activity, requestCode ->
            Msg.alertDialog().message(permanentlyDeniedId).negative(android.R.string.cancel).onOk(android.R.string.ok) {
                val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS).setData(
                    fromParts(
                        "package",
                        activity.packageName,
                        null
                    )
                )
                activity.startActivityForResult(intent, requestCode)
            }.show(activity)
        }, optionalPerms, f)
    }


    /**
     * Ask for [perms] and [optionalPerms] (if they are not already granted).
     * If they are already granted or they are granted now [f] will be called.
     * In case one or more of [perms] or [optionalPerms] were previously denied the app should show a rationale,
     *  so a message with [rationaleId] will be shown.
     * If "Don't ask again" was checked when denying a permission, a message with [permanentlyDeniedId] will be shown,
     *  sending to app settings to manually grant permissions.
     * [optionalPerms] are asked together with [perms], but if they are always denied they are simply ignored.
     * Permissions passed multiple times will be ignored.
     * Optional permissions already included in needed permissions will be ignored.
     */
    fun call(
        perms: List<String>,
        rationaleId: Int,
        permanentlyDeniedId: Int,
        optionalPerms: List<String> = ArrayList(),
        f: () -> Unit
    ) {
        call(perms,
            { permissions, activity, requestCode ->
                Msg.ad(rationaleId).negative(android.R.string.cancel)
                    .onOk(android.R.string.ok) { ActivityCompat.requestPermissions(activity, permissions, requestCode) }
                    .show(activity)
            },
            { permanentlyDeniedPerms, showRationalePerms, activity, requestCode ->
                Msg.ad(permanentlyDeniedId).negative(android.R.string.cancel).onOk(android.R.string.ok) {
                    val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS).setData(
                        fromParts(
                            "package",
                            activity.packageName,
                            null
                        )
                    )
                    activity.startActivityForResult(intent, requestCode)
                }.show(activity)
            }, optionalPerms, f
        )
    }


    private class PermHelper(
        private val requestCode: Int,
        private val optionalPermissions: List<String>,
        private val onShowRationale: (perms: Array<String>, activity: Activity, requestCode: Int) -> Unit,
        private val onPermanentlyDenied: (
            permanentlyDeniedPerms: Array<String>,
            showRationalePerms: Array<String>,
            activity: Activity, requestCode: Int
        ) -> Unit,
        private val onGranted: () -> Unit
    ) {

        var isCallingResult = false

        fun askPermissions(permissions: List<String>): Boolean {

            val activity = PowerfulSama.getCurrentActivity() ?: return true

            val permissionsToAsk = ArrayList<String>()
            val permissionsToRationale = ArrayList<String>()

            permissions.plus(optionalPermissions)
                .filter {
                    ContextCompat.checkSelfPermission(activity.applicationContext, it) != PERMISSION_GRANTED
                }
                .forEach {
                    // Permission is not granted. Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, it)) {
                        permissionsToRationale.add(it)
                    } else {
                        // No explanation needed; request the permission
                        permissionsToAsk.add(it)
                    }
                }

            return when {
                permissionsToRationale.isNotEmpty() -> {
                    isCallingResult = true
                    permissionsToRationale.addAll(permissionsToAsk)
                    onShowRationale(permissionsToRationale.toTypedArray(), activity, requestCode)
                    true
                }
                permissionsToAsk.isNotEmpty() -> {
                    isCallingResult = true
                    ActivityCompat.requestPermissions(activity, permissionsToAsk.toTypedArray(), requestCode)
                    true
                }
                else ->
                    false
            }
        }


        fun onRequestPermissionsResult(
            activity: Activity,
            permissions: Array<out String>,
            grantResults: IntArray
        ): Boolean {

            val deniedPermissions = ArrayList<String>()
            val rationalePermissions = ArrayList<String>()

            permissions.forEachIndexed { i, permission ->

                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // User rejected the permission
                    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

                    // Permission is optional or needed. user did NOT check "never ask again": show rationale and ask it
                    if (showRationale) rationalePermissions.add(permission)

                    // If permission is optional and (!showRationale) -> user CHECKED "never ask again"
                    // But permission is optional, so function can be called anyway

                    // If permission is needed and user CHECKED "never ask again":
                    // open another dialog explaining again the permission and send to the app settings
                    if (!optionalPermissions.contains(permission) && !showRationale) {
                        deniedPermissions.add(permission)
                    }
                }
            }

            when {
                // If at least 1 needed permission was permanently denied, I need to ask for it
                deniedPermissions.isNotEmpty() ->
                    onPermanentlyDenied(
                        deniedPermissions.toTypedArray(),
                        rationalePermissions.toTypedArray(),
                        activity,
                        requestCode
                    )
                //if at least 1 permission needs rationale, I must show it!
                rationalePermissions.isNotEmpty() ->
                    onShowRationale(rationalePermissions.toTypedArray(), activity, requestCode)
                else ->
                    onGranted()
            }
            return true
        }
    }
}

/** Abstract class that contains info about permissions to run a specific function. Used through [Perms.call]. */
@Suppress("UnnecessaryAbstractClass", "UnusedPrivateMember")
abstract class PermissionContainer(
    /** Required permissions needed for the function that will be asked for (if not already granted). */
    val perms: List<String>,
    /**
     * Optional permissions for the function that will be asked for (if not already granted).
     * If denied, the function will be called anyway.
     */
    val optionalPerms: List<String> = ArrayList(),
    /**
     * String for the rationale to show after denying required permissions, to show the user why permissions are needed.
     * If null [onShowRationale] will be called.
     */
    val rationaleId: Int? = null,
    /**
     * String for the message to show when the user chooses "Don't ask again" when denying required permissions.
     * If null, [onPermanentlyDenied] will be called.
     */
    val permanentlyDeniedId: Int? = null
) {
    /** Called in case one or more of [perms] or [optionalPerms] were denied and the app should show a rationale. */
    fun onShowRationale(perms: Array<String>, activity: Activity, requestCode: Int) = Unit

    /**
     * Called if "Don't ask again" was checked when denying a permission,
     *  with all permanently denied permissions and permissions needing the rationale to show.
     * Permanently denied permissions will never include [optionalPerms].
     */
    fun onPermanentlyDenied(
        permanentlyDeniedPerms: Array<String>,
        showRationalePerms: Array<String>,
        activity: Activity,
        requestCode: Int
    ) = Unit
}
