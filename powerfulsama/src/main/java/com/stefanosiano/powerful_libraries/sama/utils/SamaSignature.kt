package com.stefanosiano.powerful_libraries.sama.utils

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build

/** Object to handle checks of the signatures of the app. */
@SuppressLint("PackageManagerGetSignatures")
@Suppress("DEPRECATION")
object SamaSignature {

    /** PackageName of the application. */
    val pkgName: String = PowerfulSama.applicationContext.packageName

    /** PackageManager of the application. */
    val pm: PackageManager = PowerfulSama.applicationContext.packageManager

    /** Function to call when signature check fails. */
    var onSignatureFailed: ((Array<Signature>) -> Unit)? = null

    /** Function to call when signature check succeeds. */
    var onCheckSignature: ((Array<Signature>) -> Boolean)? = null

    /**
     * Read the signatures of the app through [PackageManager] apis and return all signatures found.
     * For more info check [PackageInfo.signingInfo] and [PackageInfo.signatures].
     */
    fun readSignatures(): Array<Signature> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo =
                pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES).signatures
        }

        return signatures
    }

    /**
     * Read the signatures of the app through [PackageManager] apis and return all signatures found.
     * For more info check [PackageInfo.signingInfo] and [PackageInfo.signatures].
     * Then [f] is run, passing the signatures as parameter.
     */
    inline fun readSignatures(crossinline f: (Array<Signature>) -> Unit) = f(readSignatures())

    /**
     * Initializes the check of the signatures.
     * Whenever [checkSignatures] will be called, if [checkF] returns false [f] will be executed.
     */
    internal fun init(checkF: (Array<Signature>) -> Boolean, f: (Array<Signature>) -> Unit) {
        onCheckSignature = checkF
        onSignatureFailed = f
    }

    /**
     * Checks the signatures of the app and eventually calls the associated function,
     *  defined in [PowerfulSama.init].
     * If you want more control, or just want to make tampering more difficult,
     *  call [readSignatures] and make your checks there.
     */
    inline fun checkSignatures() {
        onSignatureFailed?.let { f ->
            val signatures = readSignatures()
            if (onCheckSignature?.invoke(signatures) == false) {
                f(signatures)
            }
        }
    }
}
