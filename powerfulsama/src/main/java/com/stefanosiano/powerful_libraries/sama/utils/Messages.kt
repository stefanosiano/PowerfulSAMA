package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.stefanosiano.powerful_libraries.sama.mainThreadHandler
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicLong


private const val TAG = "Messages"
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
class Messages private constructor(

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
    private var autoDismissDelay: Long = 0) {


    /** Unique id used to check equality with other messages  */
    private val uid: Long = uniqueId.incrementAndGet()

    /** Implementation of the message */
    private var implementation: WeakReference<Any>? = null

    /** Job of the auto dismiss feature. When the message is dismissed, the job should be canceled */
    private var autoDismissJob: Job? = null



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

        /**
         * Creates an alertDialog with one button.
         *
         *  NOTE: it dismisses when clicking on the button.
         *
         * Default values:
         *
         *      positive = R.string.lbl_ok
         *      cancelable = false
         *      messageImpl = MessageImpl.AlertDialogOneButton
         */
        fun asAlertDialogOneButton() = Messages(
            iPositive = android.R.string.ok,
            cancelable = false,
            messageImpl = MessageImpl.AlertDialogOneButton)

        /**
         * Creates a ProgressDialog with specified options.
         *
         * Default values:
         *
         *      messageImpl = MessageImpl.ProgressDialog
         */
        fun asProgressDialog() = Messages(messageImpl = MessageImpl.ProgressDialog)

        /**
         * Creates an alertDialog with specified options.
         * Default values:
         *
         *      messageImpl = MessageImpl.AlertDialog
         */
        fun asAlertDialog() = Messages(messageImpl = MessageImpl.AlertDialog)

        /**
         * Creates a Toast with specified message.
         *
         * Default values:
         *
         *      messageImpl = MessageImpl.Toast
         */
        fun asToast() = Messages(messageImpl = MessageImpl.Toast)


        /**
         * Creates a Snackbar with specified title and okRunnable.
         *
         * Default values:
         *
         *      messageImpl = MessageImpl.Snackbar
         */
        fun asSnackbar(view: View) = Messages(messageImpl = MessageImpl.Snackbar, snackbarView = view)

        internal fun setCurrentActivity(activity: Activity) { currentActivity?.clear(); currentActivity = WeakReference(activity) }
    }





    /** Sets the title. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun title(id: Int): Messages {
        this.iTitle = id
        this.title = null
        return this
    }

    /** Sets the title. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun title(title: String): Messages {
        this.title = title
        this.iTitle = 0
        return this
    }

    /** Sets the positive button label. Default is R.string.lbl_yes.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun positive(id: Int): Messages {
        this.iPositive = id
        this.positive = null
        return this
    }

    /** Sets the positive button label. Default is R.string.lbl_yes.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun positive(positive: String): Messages {
        this.positive = positive
        this.iPositive = 0
        return this
    }

    /** Sets the negative button label. Default is R.string.lbl_no.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun negative(id: Int): Messages {
        this.iNegative = id
        this.negative = null
        return this
    }

    /** Sets the negative button label. Default is R.string.lbl_no.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun negative(negative: String): Messages {
        this.negative = negative
        this.iNegative = 0
        return this
    }

    /** Sets the neutral button label. Default is null. If set, the neutral button is added to the AlertDialog.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun neutral(id: Int): Messages {
        this.iNeutral = id
        this.neutral = null
        return this
    }

    /** Sets the neutral button label. Default is null. If set, the neutral button is added to the AlertDialog.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun neutral(neutral: String): Messages {
        this.neutral = neutral
        this.iNeutral = 0
        return this
    }

    /** Sets the message label. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun message(id: Int): Messages {
        this.iMessage = id
        this.message = ""
        return this
    }

    /** Sets the message label. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified.  */
    fun message(message: String): Messages {
        this.message = message
        this.iMessage = 0
        return this
    }

    /** Sets the indeterminate flag. Default is true  */
    fun indeterminate(indeterminate: Boolean): Messages {
        this.indeterminate = indeterminate
        return this
    }

    /** Sets the cancelable flag. Default is false  */
    fun cancelable(cancelable: Boolean): Messages {
        this.cancelable = cancelable
        return this
    }

    /** Sets the runnable to run when positive button is clicked  */
    fun onOk(onOk: () -> Unit): Messages {
        this.onOk = onOk
        return this
    }

    /** Sets the positive button label and the runnable to run when it's clicked  */
    fun onOk(positive: Int, onOk: () -> Unit): Messages {
        this.iPositive = positive
        this.onOk = onOk
        return this
    }

    /** Sets the runnable to run when negative button is clicked  */
    fun onNo(onNo: () -> Unit): Messages {
        this.onNo = onNo
        return this
    }

    /** Sets the negative button label and the runnable to run when it's clicked  */
    fun onNo(negative: Int, onNo: () -> Unit): Messages {
        this.iNegative = negative
        this.onNo = onNo
        return this
    }

    /** Sets the runnable to run when neutral button is clicked  */
    fun onCancel(onCancel: () -> Unit): Messages {
        this.onCancel = onCancel
        return this
    }

    /** Sets the neutral button label and the runnable to run when it's clicked  */
    fun onCancel(neutral: Int, onCancel: () -> Unit): Messages {
        this.iNeutral = neutral
        this.onCancel = onCancel
        return this
    }

    /** Sets the delay in milliseconds after which the message will automatically dismiss */
    fun autoDismissDelay(autoDismissDelay: Long): Messages {
        this.autoDismissDelay = autoDismissDelay
        return this
    }



    /** Retrieves the activity name by the context. If the activity is not found, it returns the current activity reference */
    private fun retrieveActivityFromContext(context: Context): Activity? {
        var c = context
        while (c is ContextWrapper) {
            if (c is Activity) return c

            c = c.baseContext
        }
        return currentActivity?.get()
    }


    /** Shows the message and returns it. Always prefer show(Activity) if possible, especially for Dialogs */
    fun show(context: Context, showMessage: Boolean = true): Messages? = if(showMessage) showMessage(context) else { onOk?.invoke(); null }

    /** Shows the message and returns it. Always prefer show(Activity) if possible, especially for Dialogs */
    fun <T> showAs(context: Context, showMessage: Boolean = true): T? = show(context, showMessage)?.implementation?.get() as? T?

    /** Shows the message and returns it */
    fun show(activity: Activity, showMessage: Boolean = true): Messages? = if(showMessage) showMessage(activity) else { onOk?.invoke(); null }

    /** Shows the message and returns it */
    fun <T> showAs(activity: Activity, showMessage: Boolean = true): T? = show(activity, showMessage)?.implementation?.get() as? T?

    /** Tries to show the message on the currently open activity. If [showMessage] is met (true, default), then [onOk] will be called */
    fun show(showMessage: Boolean = true): Messages? = currentActivity?.get()?.let { activity -> if(showMessage) showMessage(activity) else { onOk?.invoke(); null } }

    /** Tries to show the message on the currently open activity and returns the implementation (e.g. AlertDialog). If [showMessage] is met (true), then [onOk] will be called */
    fun <T> showAs(showMessage: Boolean = true): T? = show(showMessage)?.implementation?.get() as? T?


    private fun showMessage(context: Context): Messages {

        initStrings(context.applicationContext)

        //Auto-dismiss message after x seconds
        if(autoDismissDelay > 0) {
            autoDismissJob = launch {
                delay(autoDismissDelay)
                if(isShowing() == true) {
                    Log.e(TAG, "Message has been auto dismissed after $autoDismissDelay milliseconds!")
                    dismiss()
                }
            }
        }

        when (messageImpl) {
            MessageImpl.ProgressDialog -> {
                mainThreadHandler.post { buildAsProgressDialog(context); (implementation?.get() as ProgressDialog).show() }
                return this
            }

            MessageImpl.AlertDialogOneButton -> {
                val activity: Activity? = retrieveActivityFromContext(context)
                if(activity != null)
                    mainThreadHandler.post { buildAsAlertDialogOneButton(activity); (implementation?.get() as AlertDialog).show() }
                else
                    mainThreadHandler.post { buildAsToast(context); (implementation?.get() as Toast).show() }
                return this
            }

            MessageImpl.AlertDialog -> {
                val activity: Activity? = retrieveActivityFromContext(context)
                if(activity != null)
                    mainThreadHandler.post { buildAsAlertDialog(activity); (implementation?.get() as AlertDialog).show() }
                else
                    mainThreadHandler.post { buildAsToast(context); (implementation?.get() as Toast).show() }
                return this
            }

            MessageImpl.Toast -> {
                mainThreadHandler.post { buildAsToast(context); (implementation?.get() as Toast).show() }
                return this
            }

            MessageImpl.Snackbar -> {
                if(snackbarView != null) {
                    mainThreadHandler.post { buildAsSnackbar(snackbarView!!); (implementation?.get() as Snackbar).show() }
                }
                else {
                    val activity: Activity? = retrieveActivityFromContext(context)
                    if(activity != null) {
                        val v: View = activity.window.decorView.findViewById(android.R.id.content)
                        mainThreadHandler.post { buildAsSnackbar(v); (implementation?.get() as Snackbar).show() }
                    }
                    else
                        mainThreadHandler.post { buildAsToast(context); (implementation?.get() as Toast).show() }
                }

                return this
            }
        }
        Log.e(TAG, "Cannot understand the implementation type of the message. Skipping show")
        return this
    }

    /** Dismisses the message  */
    fun dismiss() {

        when (messageImpl) {
            MessageImpl.ProgressDialog -> (implementation?.get() as? ProgressDialog?)?.let { if (it.isShowing) it.dismiss() }
            MessageImpl.AlertDialogOneButton, MessageImpl.AlertDialog -> (implementation?.get() as? AlertDialog?)?.let { if (it.isShowing) it.dismiss() }
            MessageImpl.Toast -> (implementation?.get() as? Toast?)?.cancel()
            MessageImpl.Snackbar -> (implementation?.get() as? Snackbar?)?.let { if (it.isShown) it.dismiss() }
            else -> Log.e(TAG, "Cannot understand the implementation type of the message. Skipping dismiss")
        }
        autoDismissJob?.cancel()
    }

    /** Returns if the message is showing (Toast will always return false) */
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


    private fun buildAsProgressDialog(context: Context): Messages {
        val progressDialog = ProgressDialog(context)
        progressDialog.setTitle(title)
        progressDialog.setMessage(message)
        progressDialog.isIndeterminate = indeterminate
        progressDialog.setCancelable(cancelable)
        implementation = WeakReference(progressDialog)
        return this
    }

    private fun buildAsAlertDialog(activity: Activity): Messages {

        initStrings(activity)

        val mAlert = AlertDialog.Builder(activity)
        mAlert.setCancelable(cancelable)
        mAlert.setTitle(title)
        mAlert.setMessage(message)

        mAlert.setPositiveButton(positive) { dialog, _ -> onOk?.invoke(); dialog.dismiss() }
        mAlert.setNegativeButton(negative) { dialog, _ -> onNo?.invoke(); dialog.dismiss() }

        if (!TextUtils.isEmpty(neutral))
            mAlert.setNeutralButton(neutral) { dialog, _ -> onCancel?.invoke(); dialog.dismiss() }


        implementation = WeakReference(mAlert.create())
        return this
    }


    private fun buildAsAlertDialogOneButton(activity: Activity): Messages {

        val mAlert = AlertDialog.Builder(activity)

        mAlert.setCancelable(cancelable)
        mAlert.setTitle(title)
        mAlert.setMessage(message)

        mAlert.setPositiveButton(positive) { dialog, _ -> onOk?.invoke(); dialog.dismiss() }

        implementation = WeakReference(mAlert.create())
        return this
    }

    private fun buildAsToast(context: Context): Messages {
        implementation = WeakReference(Toast.makeText(context, message, Toast.LENGTH_SHORT))
        return this
    }

    private fun buildAsSnackbar(view: View): Messages {
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
        return uid == (other as Messages?)?.uid
    }

    override fun hashCode(): Int {
        return (uid xor uid.ushr(32)).toInt()
    }

}