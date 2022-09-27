package com.stefanosiano.powerful_libraries.sama.utils

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build

/** Helper class to validate the signature of the app. */
object SamaSignature {

    private val pkgName = PowerfulSama.applicationContext.packageName
    private val pm = PowerfulSama.applicationContext.packageManager

    /** Function to be called in case a signature check fails. Used inside [checkSignatures] method. */
    var onSignatureFailed : ((Array<Signature>) -> Unit)? = null
    /** Function to be called in case a signature check succeeds. Used inside [checkSignatures] method. */
    var onCheckSignature : ((Array<Signature>) -> Boolean)? = null


    /**
     * Reads the signatures of the app, through [PackageManager] apis, and return all signatures found.
     * For more info check [android.content.pm.PackageInfo.signingInfo] and [android.content.pm.PackageInfo.signatures].
     */
    fun readSignatures(): Array<Signature> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
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
     * Reads the signatures of the app, through [PackageManager] apis, and run [f] passing all signatures found.
     * For more info check [android.content.pm.PackageInfo.signingInfo] and [android.content.pm.PackageInfo.signatures].
     */
    inline fun readSignatures(crossinline f: (Array<Signature>) -> Unit) {
        val signatures = readSignatures()
        f(signatures)
    }

    /**
     * Checks the signatures of the app and eventually calls the associated function, defined in [PowerfulSama.init].
     * If you want more control, or just want to make tampering more difficult,
     * call [readSignatures] and make your checks there.
     */
    inline fun checkSignatures() {
        onSignatureFailed?.let { f ->
            val signatures = readSignatures()
            if(onCheckSignature?.invoke(signatures) == false) {
                f(signatures)
            }
        }
    }


    /** Initializes the check of the signatures. Whenever [checkSignatures] will be called,
     * if [checkF] returns false [f] will be executed. */
    internal fun init(checkF: (Array<Signature>) -> Boolean, f: (Array<Signature>) -> Unit) {
        onCheckSignature = checkF
        onSignatureFailed = f
    }
}
