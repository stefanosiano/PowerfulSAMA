package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.ContextWrapper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.stefanosiano.powerful_libraries.sama.runOnUi
import com.stefanosiano.powerful_libraries.sama.runOnUiAndWait
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicLong


private const val TAG = "Msg"
/**
 * Class that manages common messages in the app, like ProgressDialogs, AlertDialog, Snackbar, etc.
 *
 * Default values for every message (if not overwritten by "as..." methods:
 *
 *      title = null
 *      message = ""
 *      positive = R.string.lbl_yes
 *      negative = R.string.lbl_no
 *      neutral = null
 *      okRunnable = null
 *      noRunnable = null
 *      cancelRunnable = null
 *      indeterminate = true
 *      cancelable = false
 *      autoDismissDelay = 0 (doesn't dismiss)
 */
class Msg private constructor(

    /** Theme of the message (if available) */
    private var theme: Int? = null,

    /** Title of the message (if available) */
    private var title: String? = null,

    /** Message to show */
    private var message: String = "",

    /** String to show in positive button */
    private var positive: String? = null,

    /** String to show in negative button */
    private var negative: String? = null,

    /** String to show in neutral button */
    private var neutral: String? = null,

    /** Title to show (if available). Overwrites title if != 0 */
    private var iTitle: Int = 0,

    /** Message to show. Overwrites message if != 0 */
    private var iMessage: Int = 0,

    /** String to show in positive button. Overwrites positive if != 0 */
    private var iPositive: Int = defaultYes,

    /** String to show in negative button. Overwrites negative if != 0 */
    private var iNegative: Int = defaultNo,

    /** String to show in neutral button. Overwrites neutral if != 0 */
    private var iNeutral: Int = 0,

    /** Tunnable to run after clicking on positive button */
    private var onOk: (() -> Unit)? = null,

    /** Tunnable to run after clicking on negative button */
    private var onNo: (() -> Unit)? = null,

    /** Tunnable to run after clicking on neutral button */
    private var onCancel: (() -> Unit)? = null,

    /** Set if the message is indeterminate (if available) */
    private var indeterminate: Boolean = false,

    /** Set if the message is cancelable (if available) */
    private var cancelable: Boolean = false,

    /** Implementation type of the message */
    private var messageImpl: MessageImpl? = null,

    /** View to bind the snackbar to (when showing it) */
    private var snackbarView: View? = null,

    /** Delay in milliseconds after which the message will automatically dismiss */
    private var autoDismissDelay: Long = 0,

    /** Customization function of the message. Note: It will be called on UI thread */
    private var customize: ((Any) -> Unit)? = null) {


    /** Unique id used to check equality with other messages  */
    private val uid: Long = uniqueId.incrementAndGet()

    /** Implementation of the message */
    private var implementation: WeakReference<Any>? = null

    /** Job of the auto dismiss feature. When the message is dismissed, the job should be canceled */
    private var autoDismissJob: Job? = null

    /** Flag to know if the buildAs function has been called */
    private var isBuilt = false



    /**
     * Class that manages common messages in the app, like ProgressDialogs, AlertDialog, Snackbar, etc.
     *
     * Default values for every message (if not overwritten by "as..." methods:
     *
     *      title = null
     *      message = ""
     *      positive = R.string.lbl_yes
     *      negative = R.string.lbl_no
     *      neutral = null
     *      okRunnable = null
     *      noRunnable = null
     *      cancelRunnable = null
     *      indeterminate = true
     *      cancelable = false
     */
    companion object : CoroutineScope {

        private val loggingExceptionHandler = CoroutineExceptionHandler { _, t -> t.printStackTrace() }
        override val coroutineContext = SupervisorJob() + loggingExceptionHandler

        /** Shared unique id used to check equality with other messages  */
        private val uniqueId: AtomicLong = AtomicLong(0)

        /** Weak reference to the current activity */
        private var currentActivity : WeakReference<Activity>? = null

        /** Default "Yes" string id */
        internal var defaultYes : Int = android.R.string.yes

        /** Default "No" string id */
        internal var defaultNo : Int = android.R.string.no

        /** Default theme used by messages */
        internal var defaultTheme : Int? = null

        /** Default customization function. Note: It will be called on UI thread */
        internal var defaultCustomization : ((Any) -> Unit)? = null

        /**
         * Creates an alertDialog with one button, with an optional [theme].
         *
         *  NOTE: it dismisses when clicking on the button.
         *
         * Default values:
         *
         *      positive = R.string.lbl_ok
         *      cancelable = false
         *      messageImpl = MessageImpl.AlertDialogOneButton
         */
        fun alertDialogOneButton(theme: Int? = null) = Msg(
            theme = theme ?: defaultTheme,
            iPositive = android.R.string.ok,
            cancelable = false,
            messageImpl = MessageImpl.AlertDialogOneButton,
            customize = defaultCustomization)

        /**
         * Creates a ProgressDialog with specified options, with an optional [theme].
         *
         * Default values:
         *
         *      messageImpl = MessageImpl.ProgressDialog
         */
        fun progressDialog(theme: Int? = null) = Msg(theme = theme ?: defaultTheme, messageImpl = MessageImpl.ProgressDialog, customize = defaultCustomization)

        /**
         * Creates an alertDialog with specified options, with an optional [theme].
         * Default values:
         *
         *      messageImpl = MessageImpl.AlertDialog
         */
        fun alertDialog(theme: Int? = null) = Msg(theme = theme ?: defaultTheme, messageImpl = MessageImpl.AlertDialog, customize = defaultCustomization)

        /**
         * Creates a Toast with specified message.
         *
         * Default values:
         *
         *      messageImpl = MessageImpl.Toast
         */
        fun toast() = Msg(messageImpl = MessageImpl.Toast, customize = defaultCustomization)


        /**
         * Creates a Snackbar with specified title and okRunnable.
         *
         * Default values:
         *
         *      messageImpl = MessageImpl.Snackbar
         */
        fun snackbar(view: View) = Msg(messageImpl = MessageImpl.Snackbar, snackbarView = view, customize = defaultCustomization)

        internal fun setCurrentActivity(activity: Activity) { currentActivity?.clear(); currentActivity = WeakReference(activity) }
    }





    /** Sets the title. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun title(id: Int): Msg { this.iTitle = id; this.title = null; return this }

    /** Sets the title. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun title(title: String): Msg { this.title = title; this.iTitle = 0; return this }

    /** Sets the positive button label. Default is R.string.lbl_yes.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun positive(id: Int): Msg { this.iPositive = id; this.positive = null; return this }

    /** Sets the positive button label. Default is R.string.lbl_yes.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun positive(positive: String): Msg { this.positive = positive; this.iPositive = 0; return this }

    /** Sets the negative button label. Default is R.string.lbl_no.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun negative(id: Int): Msg { this.iNegative = id; this.negative = null; return this }

    /** Sets the negative button label. Default is R.string.lbl_no.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun negative(negative: String): Msg { this.negative = negative; this.iNegative = 0; return this }

    /** Sets the neutral button label. Default is null. If set, the neutral button is added to the AlertDialog.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun neutral(id: Int): Msg { this.iNeutral = id; this.neutral = null; return this }

    /** Sets the neutral button label. Default is null. If set, the neutral button is added to the AlertDialog.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun neutral(neutral: String): Msg { this.neutral = neutral; this.iNeutral = 0; return this }

    /** Sets the message label. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun message(id: Int): Msg { this.iMessage = id; this.message = ""; return this }

    /** Sets the message label. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun message(message: String): Msg { this.message = message; this.iMessage = 0; return this }

    /** Sets the indeterminate flag. Default is true  */
    fun indeterminate(indeterminate: Boolean): Msg { this.indeterminate = indeterminate; return this }

    /** Sets the cancelable flag. Default is false  */
    fun cancelable(cancelable: Boolean): Msg { this.cancelable = cancelable; return this }

    /** Sets the runnable to run when positive button is clicked  */
    fun onOk(onOk: () -> Unit): Msg { this.onOk = onOk; return this }

    /** Sets the positive button label and the runnable to run when it's clicked  */
    fun onOk(positive: Int, onOk: () -> Unit): Msg { this.iPositive = positive; this.onOk = onOk; return this }

    /** Sets the runnable to run when negative button is clicked  */
    fun onNo(onNo: () -> Unit): Msg { this.onNo = onNo; return this }

    /** Sets the negative button label and the runnable to run when it's clicked  */
    fun onNo(negative: Int, onNo: () -> Unit): Msg { this.iNegative = negative; this.onNo = onNo; return this }

    /** Sets the runnable to run when neutral button is clicked  */
    fun onCancel(onCancel: () -> Unit): Msg { this.onCancel = onCancel; return this }

    /** Sets the neutral button label and the runnable to run when it's clicked  */
    fun onCancel(neutral: Int, onCancel: () -> Unit): Msg { this.iNeutral = neutral; this.onCancel = onCancel; return this }

    /** Sets the delay in milliseconds after which the message will automatically dismiss */
    fun autoDismissDelay(autoDismissDelay: Long): Msg { this.autoDismissDelay = autoDismissDelay; return this }



    /** Retrieves the activity name by the context. If the activity is not found, it returns the current activity reference */
    private fun getActivityFromCtx(context: Context): Activity? {
        var c = context
        while (c is ContextWrapper) {
            if (c is Activity) return c

            c = c.baseContext
        }
        return currentActivity?.get()
    }


    /** Shows the message and returns it. Always prefer an Activity to a Context if possible, especially for AlertDialogs and ProgressDialog */
    fun show(context: Context, showMessage: Boolean = true): Msg? = if(showMessage) showMessage(context) else { onOk?.invoke(); null }

    /** Shows the message and returns it. Always prefer an Activity to a Context if possible, especially for AlertDialogs and ProgressDialog */
    fun <T> showAs(context: Context, showMessage: Boolean = true): T? = show(context, showMessage)?.implementation?.get() as? T?

    /** Tries to show the message on the currently open activity. If [showMessage] is met (true, default), then [onOk] will be called */
    fun show(showMessage: Boolean = true): Msg? = currentActivity?.get()?.let { activity -> if(showMessage) showMessage(activity) else { onOk?.invoke(); null } }

    /** Tries to show the message on the currently open activity and returns the implementation (e.g. AlertDialog). If [showMessage] is met (true), then [onOk] will be called */
    fun <T> showAs(showMessage: Boolean = true): T? = show(showMessage)?.implementation?.get() as? T?

    /** Build the message and returns it. Optionally calls [f] right after building the message. Always prefer an Activity to a Context if possible, especially for AlertDialogs and ProgressDialog */
    fun <T> buildAs(f: ((T?) -> Unit)? = null): T? = currentActivity?.get()?.let { buildAs(it, f) }

    /** Build the message and returns it. Always prefer an Activity to a Context if possible, especially for AlertDialogs and ProgressDialog */
    fun build(): Msg? = currentActivity?.get()?.let { build(it) }

    /** Build the message and returns it. Optionally calls [f] right after building the message. Always prefer an Activity to a Context if possible, especially for AlertDialogs and ProgressDialog */
    fun <T> buildAs(ctx: Context, f: ((T?) -> Unit)? = null): T? { val m = build(ctx).implementation?.get() as? T?; f?.invoke(m); return m }

    /** Build the message and returns it. Always prefer an Activity to a Context if possible, especially for AlertDialogs and ProgressDialog */
    fun build(ctx: Context): Msg {

        initStrings(ctx.applicationContext)

        when (messageImpl) {
            MessageImpl.ProgressDialog -> runOnUiAndWait { buildAsProgressDialog(ctx) }
            MessageImpl.AlertDialogOneButton -> runOnUiAndWait { getActivityFromCtx(ctx)?.let { buildAsAlertDialogOneButton(it) } ?: buildAsToast(ctx) }
            MessageImpl.AlertDialog -> runOnUiAndWait { getActivityFromCtx(ctx)?.let { buildAsAlertDialog(it) } ?: buildAsToast(ctx) }
            MessageImpl.Toast -> runOnUiAndWait { buildAsToast(ctx) }
            MessageImpl.Snackbar -> {
                snackbarView?.let { runOnUiAndWait { buildAsSnackbar(it) } } ?: runOnUiAndWait {
                    (getActivityFromCtx(ctx)?.window?.decorView?.findViewById(android.R.id.content) as? View?)?.let { buildAsSnackbar(it) } ?: buildAsToast(ctx)
                }
            }
            else -> Log.e(TAG, "Cannot understand the implementation type of the message. Skipping show")
        }
        return this
    }


    private fun showMessage(ctx: Context): Msg {

        initStrings(ctx.applicationContext)

        autoDismissJob?.cancel()
        //Auto-dismiss message after x seconds
        if(autoDismissDelay > 0) {
            autoDismissJob = launch {
                delay(autoDismissDelay)
                dismiss()
            }
        }

        runOnUiAndWait {
            if (!isBuilt) build(ctx)

            when (messageImpl) {
                MessageImpl.ProgressDialog -> (implementation?.get() as ProgressDialog).show()
                MessageImpl.AlertDialogOneButton -> (implementation?.get() as? AlertDialog?)?.show() ?: (implementation?.get() as? Toast?)?.show()
                MessageImpl.AlertDialog -> (implementation?.get() as? AlertDialog?)?.show() ?: (implementation?.get() as? Toast?)?.show()
                MessageImpl.Toast -> (implementation?.get() as Toast).show()
                MessageImpl.Snackbar -> (implementation?.get() as? Snackbar?)?.show() ?: (implementation?.get() as? Toast?)?.show()
                else -> { Log.e(TAG, "Cannot understand the implementation type of the message. Skipping show") }
            }

            implementation?.get()?.let { customize?.invoke(it) }
        }

        return this
    }

    /** Dismisses the message */
    fun dismiss() {

        runOnUi {
            when (messageImpl) {
                MessageImpl.ProgressDialog -> (implementation?.get() as? ProgressDialog?)?.let { if (it.isShowing) it.dismiss() }
                MessageImpl.AlertDialogOneButton, MessageImpl.AlertDialog -> (implementation?.get() as? AlertDialog?)?.let { if (it.isShowing) it.dismiss() }
                MessageImpl.Toast -> (implementation?.get() as? Toast?)?.cancel()
                MessageImpl.Snackbar -> (implementation?.get() as? Snackbar?)?.let { if (it.isShown) it.dismiss() }
                else -> Log.e(TAG, "Cannot understand the implementation type of the message. Skipping dismiss")
            }
            autoDismissJob?.cancel()
        }
    }

    /** Returns if the message is showing (Toasts will always return false) */
    fun isShowing() =
        when (messageImpl) {
            MessageImpl.ProgressDialog -> (implementation?.get() as? ProgressDialog?)?.isShowing
            MessageImpl.AlertDialogOneButton, MessageImpl.AlertDialog -> (implementation?.get() as? AlertDialog?)?.isShowing
            MessageImpl.Toast -> false
            MessageImpl.Snackbar -> (implementation?.get() as? Snackbar?)?.isShown
            else -> { Log.e(TAG, "Cannot understand the implementation type of the message. Skipping isShowing"); false }
        }



    /** Retrieves the strings from the ids.  */
    private fun initStrings(context: Context) {
        if (iTitle != 0) title = context.getString(iTitle)
        if (iMessage != 0) message = context.getString(iMessage)
        if (iPositive != 0) positive = context.getString(iPositive)
        if (iNegative != 0) negative = context.getString(iNegative)
        if (iNeutral != 0) neutral = context.getString(iNeutral)
    }


    private fun buildAsProgressDialog(context: Context): Msg {
        val progressDialog = ProgressDialog(context, theme ?: 0)
        progressDialog.setTitle(title)
        progressDialog.setMessage(message)
        progressDialog.isIndeterminate = indeterminate
        progressDialog.setCancelable(cancelable)
        implementation = WeakReference(progressDialog)
        isBuilt = true
        return this
    }

    private fun buildAsAlertDialog(activity: Activity): Msg {

        initStrings(activity)

        val mAlert = AlertDialog.Builder(activity, theme ?: 0)
        mAlert.setCancelable(cancelable)
        mAlert.setTitle(title)
        mAlert.setMessage(message)

        mAlert.setPositiveButton(positive) { dialog, _ -> onOk?.invoke(); dialog.dismiss() }
        mAlert.setNegativeButton(negative) { dialog, _ -> onNo?.invoke(); dialog.dismiss() }

        if (!TextUtils.isEmpty(neutral))
            mAlert.setNeutralButton(neutral) { dialog, _ -> onCancel?.invoke(); dialog.dismiss() }


        implementation = WeakReference(mAlert.create())
        isBuilt = true
        return this
    }


    private fun buildAsAlertDialogOneButton(activity: Activity): Msg {

        val mAlert = AlertDialog.Builder(activity, theme ?: 0)

        mAlert.setCancelable(cancelable)
        mAlert.setTitle(title)
        mAlert.setMessage(message)

        mAlert.setPositiveButton(positive) { dialog, _ -> onOk?.invoke(); dialog.dismiss() }

        implementation = WeakReference(mAlert.create())
        isBuilt = true
        return this
    }

    private fun buildAsToast(context: Context): Msg {
        implementation = WeakReference(Toast.makeText(context, message, Toast.LENGTH_SHORT))
        isBuilt = true
        return this
    }

    private fun buildAsSnackbar(view: View): Msg {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        if (onOk != null) snackbar.setAction(positive) { onOk?.invoke() }
        implementation = WeakReference(snackbar)
        return this
    }


    /** Implementation type used to show and dismiss the message  */

    private enum class MessageImpl {
        ProgressDialog,
        AlertDialogOneButton,
        AlertDialog,
        Toast,
        Snackbar
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return uid == (other as Msg?)?.uid
    }

    override fun hashCode(): Int {
        return (uid xor uid.ushr(32)).toInt()
    }

}