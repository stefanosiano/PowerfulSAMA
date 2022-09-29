package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity

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
