package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.delayUntil
import com.stefanosiano.powerful_libraries.sama.logError
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.logWarning
import com.stefanosiano.powerful_libraries.sama.runOnUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicLong

/**
 * Class that manages common messages in the app, like ProgressDialogs, AlertDialogs, Snackbars and Toasts.
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
 *      duration = LENGHT_SHORT
 */
class Msg private constructor(

    /** Theme of the message (if available). */
    private var theme: Int? = null,

    /** Title of the message (if available). */
    private var title: String? = null,

    /** Message to show. */
    private var message: String = "",

    /** String to show in positive button. */
    private var positive: String? = null,

    /** String to show in negative button. */
    private var negative: String? = null,

    /** String to show in neutral button. */
    private var neutral: String? = null,

    /** Title to show (if available). Overwrites title if != 0. */
    private var iTitle: Int = 0,

    /** Message to show. Overwrites message if != 0. */
    private var iMessage: Int = 0,

    /** String to show in positive button. Overwrites positive if != 0. */
    private var iPositive: Int = defaultYes,

    /** String to show in negative button. Overwrites negative if != 0. */
    private var iNegative: Int = defaultNo,

    /** String to show in neutral button. Overwrites neutral if != 0. */
    private var iNeutral: Int = 0,

    /** Runnable to run after clicking on positive button. */
    private var onOk: ((Msg?) -> Unit)? = null,

    /** Runnable to run after clicking on negative button. */
    private var onNo: ((Msg) -> Unit)? = null,

    /** Runnable to run after clicking on neutral button. */
    private var onCancel: ((Msg) -> Unit)? = null,

    /** Runnable to run right before [show] is called. */
    private var onShow: ((Msg) -> Unit)? = null,

    /** Runnable to run right after [dismiss] is called. */
    private var onDismiss: ((Msg) -> Unit)? = null,

    /** Set if the message is indeterminate (if available). */
    private var indeterminate: Boolean = false,

    /** Set if the message is cancelable (if available). */
    private var cancelable: Boolean = false,

    /** Implementation type of the message. */
    private var messageImpl: MessageImpl? = null,

    /** View to bind the snackbar to (when showing it). */
    private var snackbarView: View? = null,

    /** Delay in milliseconds after which the message will automatically dismiss. */
    private var autoDismissDelay: Long = 0,

    /** Duration of the message in milliseconds (for [Toast] and [Snackbar]). Use one of Msg.LENGHT... constants. */
    private var duration: Int = LENGHT_SHORT,

    /** Customization function of the message. Note: It will be called on UI thread. */
    private var customize: ((Any) -> Unit)? = null
) {

    /** Unique id used to check equality with other messages . */
    private val uid: Long = uniqueId.incrementAndGet()

    /**
     * Implementation of the message. Careful when using it.
     * This is a weakReference to the underlying implementation (AlertDialog, ProgressDialog, Toast or Snackbar).
     * Cast it to the right class and bear in mind it can be null.
     */
    var implementation: WeakReference<Any>? = null

    /** Job of the auto dismiss feature. When the message is dismissed, the job should be canceled. */
    private var autoDismissJob: Job? = null

    /** Flag to know if the buildAs function has been called. */
    private var isBuilt = false

    /** Flag to know if the screen orientation should be locked while showing the message. */
    private var lockOrientation = false

    /** Flag to know if the caller should wait for the message to dismiss to continue its work. */
    private var waitForDismiss = false

    /** Sets the title. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified. */
    fun title(id: Int): Msg = this.also {
        this.iTitle = id
        this.title = null
    }

    /** Sets the title. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified. */
    fun title(title: String): Msg = this.also {
        this.title = title
        this.iTitle = 0
    }

    /** Sets the positive button label. Default is R.string.lbl_yes.
     * Method that takes the text id overrides the one with string, if both are specified. */
    fun positive(id: Int): Msg = this.also {
        this.iPositive = id
        this.positive = null
    }

    /** Sets the positive button label. Default is R.string.lbl_yes.
     * Method that takes the text id overrides the one with string, if both are specified. */
    fun positive(positive: String): Msg = this.also {
        this.positive = positive
        this.iPositive = 0
    }

    /** Sets the negative button label. Default is R.string.lbl_no.
     * Method that takes the text id overrides the one with string, if both are specified. */
    fun negative(id: Int): Msg = this.also {
        this.iNegative = id
        this.negative = null
    }

    /** Sets the negative button label. Default is R.string.lbl_no.
     * Method that takes the text id overrides the one with string, if both are specified. */
    fun negative(negative: String): Msg = this.also {
        this.negative = negative
        this.iNegative = 0
    }

    /** Sets the neutral button label. Default is null. If set, the neutral button is added to the AlertDialog.
     * Method that takes the text id overrides the one with string, if both are specified. */
    fun neutral(id: Int): Msg = this.also {
        this.iNeutral = id
        this.neutral = null
    }

    /** Sets the neutral button label. Default is null. If set, the neutral button is added to the AlertDialog.
     * Method that takes the text id overrides the one with string, if both are specified. */
    fun neutral(neutral: String): Msg = this.also {
        this.neutral = neutral
        this.iNeutral = 0
    }

    /** Sets the message label. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified. */
    fun message(id: Int): Msg = this.also {
        this.iMessage = id
        this.message = ""
    }

    /** Sets the message label. Default is null.
     * Method that takes the text id overrides the one with string, if both are specified. */
    fun message(message: String): Msg = this.also {
        this.message = message
        this.iMessage = 0
    }

    /** Sets the indeterminate flag. Default is true . */
    fun indeterminate(indeterminate: Boolean): Msg = this.also {
        this.indeterminate = indeterminate
    }

    /** Sets the cancelable flag. Default is false . */
    fun cancelable(cancelable: Boolean): Msg = this.also {
        this.cancelable = cancelable
    }

    /** Sets the runnable to run when positive button is clicked . */
    fun onOk(onOk: (Msg?) -> Unit): Msg = this.also {
        this.onOk = onOk
    }

    /** Sets the positive button label and the runnable to run when it's clicked . */
    fun onOk(positive: Int, onOk: (Msg?) -> Unit): Msg = this.also {
        this.iPositive = positive
        this.onOk = onOk
    }

    /** Sets the runnable to run when negative button is clicked . */
    fun onNo(onNo: (Msg) -> Unit): Msg = this.also {
        this.onNo = onNo
    }

    /** Sets the negative button label and the runnable to run when it's clicked . */
    fun onNo(negative: Int, onNo: (Msg) -> Unit): Msg = this.also {
        this.iNegative = negative
        this.onNo = onNo
    }

    /** Sets the runnable to run when neutral button is clicked . */
    fun onCancel(onCancel: (Msg) -> Unit): Msg = this.also {
        this.onCancel = onCancel
    }

    /** Sets the neutral button label and the runnable to run when it's clicked . */
    fun onCancel(neutral: Int, onCancel: (Msg) -> Unit): Msg = this.also {
        this.iNeutral = neutral
        this.onCancel = onCancel
    }

    /** Sets the delay in milliseconds after which the message will automatically dismiss. */
    fun autoDismissDelay(autoDismissDelay: Long): Msg = this.also {
        this.autoDismissDelay = autoDismissDelay
    }

    /** Sets the duration of the message in milliseconds (for [Toast] and [Snackbar]). */
    fun duration(duration: Int): Msg = this.also {
        this.duration = duration
    }

    /** Sets the runnable to run right before the message is shown. */
    fun onShow(onShow: (Msg) -> Unit): Msg = this.also {
        this.onShow = onShow
    }

    /** Sets the runnable to run right after the message is dismissed. */
    fun onDismiss(onDismiss: (Msg) -> Unit): Msg = this.also {
        this.onDismiss = onDismiss
    }

    /** Sets whether to lock the orientation while showing the message until it's dismissed.
     * Does nothing if api level < MR2.
     * Be sure to check correctness by calling [Activity.setRequestedOrientation] with
     *  [ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED].
     * If the activity changed while showing the message, it's possible that the orientation will not be restored after
     *  dismissing the message.
     */
    fun lockOrientation(lockOrientation: Boolean): Msg = this.also {
        this.lockOrientation = lockOrientation
    }

    /** Customization function of the message, called right after showing it. Note: It will be called on main thread. */
    fun customize(customize: (Any) -> Unit): Msg = this.also {
        this.customize = customize
    }

    /**
     * Retrieves the activity name by the context.
     * If the activity is not found, it returns the current activity reference.
     */
    private fun getActivityFromCtx(context: Context): Activity? {
        var c = context
        while (c is ContextWrapper) {
            if (c is Activity) {
                return c
            }
            c = c.baseContext
        }
        return PowerfulSama.getCurrentActivity()
    }

    /**
     * Shows the message and returns it.
     * If [showMessage] is met (true, default), then the message will be shown, otherwise [onOk] will be called.
     */
    fun show(context: Context? = null, showMessage: Boolean = true): Msg? =
        if (showMessage) {
            showMessage(context)
        } else {
            onOk?.invoke(null)
            null
        }

    /** Shows the message and returns it. */
    fun show(context: Context? = null): Msg = showMessage(context)

    /**
     * Shows the message and waits until it's dismissed.
     * If [showMessage] is met (true, default), then the message will be shown, otherwise [onOk] will be called.
     */
    suspend fun showAndWait(context: Context? = null, showMessage: Boolean = true) {
        waitForDismiss = true
        show(context, showMessage)
        delayUntil { !waitForDismiss }
    }

    /**
     * Shows the message and returns its implementation (e.g. AlertDialog).
     * Optionally calls [f] right after building the message and before showing it.
     * If [showMessage] is met (true by default), then the message will be shown, otherwise [onOk] will be called.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> showAs(context: Context? = null, f: ((T?) -> Unit)? = null, showMessage: Boolean = true): Msg? {
        val msg = build(context)
        f?.invoke(msg.implementation?.get() as? T?)
        return msg.show(context, showMessage)
    }

    /**
     * Build the message and returns it.
     * Optionally calls [f] right after building the message and before showing it.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> buildAs(ctx: Context? = null, f: ((T?) -> Unit)? = null): T? {
        val msg = build(ctx)
        f?.invoke(msg.implementation?.get() as? T?)
        return msg.implementation?.get() as? T?
    }

    /** Build the message and returns it. */
    fun build(ctx: Context?): Msg {
        logVerbose("Building message")
        val context = ctx ?: PowerfulSama.getCurrentActivity() ?: PowerfulSama.applicationContext
        initStrings(context.applicationContext)

        var finished = false
        runBlocking {
            runOnUi {
                logError("a1")
                when (messageImpl) {
                    MessageImpl.ProgressDialog -> getActivityFromCtx(context)
                        ?.let { buildAsProgressDialog(context) }
                        ?: buildAsToast(context)
                    MessageImpl.AlertDialogOneButton -> getActivityFromCtx(context)
                        ?.let { buildAsAlertDialogOneButton(it) }
                        ?: buildAsToast(context)
                    MessageImpl.AlertDialog -> getActivityFromCtx(context)
                        ?.let { buildAsAlertDialog(it) }
                        ?: buildAsToast(context)
                    MessageImpl.Toast -> buildAsToast(context)
                    MessageImpl.Snackbar -> snackbarView?.let { buildAsSnackbar(it) }
                        ?: run {
                            getActivityFromCtx(context)?.window?.decorView?.findViewById<View?>(android.R.id.content)
                                ?.let { buildAsSnackbar(it) }
                                ?: buildAsToast(context)
                        }
                    else -> logError("Cannot understand the implementation type of the message. Skipping show")
                }
                logError("a2")
                finished = true
            }
            while (isActive && !finished) {
                delay(10)
            }
        }
        return this
    }

    /** Build the message and returns it. */
    private suspend fun build2(ctx: Context?): Msg {
        logVerbose("Building message")
        val context = ctx ?: PowerfulSama.getCurrentActivity() ?: PowerfulSama.applicationContext
        initStrings(context.applicationContext)
        withContext(Dispatchers.Main) {
            when (messageImpl) {
                MessageImpl.ProgressDialog -> getActivityFromCtx(context)
                    ?.let { buildAsProgressDialog(context) }
                    ?: buildAsToast(context)
                MessageImpl.AlertDialogOneButton -> getActivityFromCtx(context)
                    ?.let { buildAsAlertDialogOneButton(it) }
                    ?: buildAsToast(context)
                MessageImpl.AlertDialog -> getActivityFromCtx(context)
                    ?.let { buildAsAlertDialog(it) }
                    ?: buildAsToast(context)
                MessageImpl.Toast -> buildAsToast(context)
                MessageImpl.Snackbar -> snackbarView?.let { buildAsSnackbar(it) }
                    ?: run {
                        getActivityFromCtx(context)?.window?.decorView?.findViewById<View?>(android.R.id.content)
                            ?.let { buildAsSnackbar(it) }
                            ?: buildAsToast(context)
                    }
                else -> logError("Cannot understand the implementation type of the message. Skipping show")
            }
        }
        return this
    }

    @Suppress("DEPRECATION")
    private fun showMessage(ctx: Context?): Msg {
        logVerbose("Showing message")
        val context = ctx ?: PowerfulSama.getCurrentActivity() ?: PowerfulSama.applicationContext
        initStrings(context.applicationContext)

        autoDismissJob?.cancel()
        // Auto-dismiss message after x seconds
        if (autoDismissDelay > 0) {
            autoDismissJob = launch {
                delay(autoDismissDelay)
                logWarning("Auto dismissing message: $autoDismissDelay milliseconds are passed")
                dismiss()
            }
        }

        launch(Dispatchers.Main) {
            if (!isBuilt) build2(context)
            when (messageImpl) {
                MessageImpl.ProgressDialog -> {
                    onShow?.invoke(this@Msg)
                    if (lockOrientation) {
                        lockOrientation(context)
                    }
                    (implementation?.get() as ProgressDialog?)?.show()
                        ?: logError("No activity found to show this progress dialog. Skipping show")
                }
                MessageImpl.AlertDialogOneButton -> {
                    onShow?.invoke(this@Msg)
                    if (lockOrientation) {
                        lockOrientation(context)
                    }
                    (implementation?.get() as? AlertDialog?)?.show()
                        ?: (implementation?.get() as? Toast?)?.show()
                }
                MessageImpl.AlertDialog -> {
                    onShow?.invoke(this@Msg)
                    if (lockOrientation) {
                        lockOrientation(context)
                    }
                    (implementation?.get() as? AlertDialog?)?.show()
                        ?: (implementation?.get() as? Toast?)?.show()
                }
                MessageImpl.Toast -> {
                    onShow?.invoke(this@Msg)
                    (implementation?.get() as? Toast?)?.show()
                }
                MessageImpl.Snackbar -> {
                    onShow?.invoke(this@Msg)
                    if (lockOrientation) {
                        lockOrientation(context)
                    }
                    (implementation?.get() as? Snackbar?)?.show()
                        ?: (implementation?.get() as? Toast?)?.show()
                }
                else -> logError("Cannot understand the implementation type of the message. Skipping show")
            }
            implementation?.get()?.let { customize?.invoke(it) }
        }

        return this
    }

    private fun lockOrientation(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            (getActivityFromCtx(context) ?: PowerfulSama.getCurrentActivity())?.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
    }

    private fun unlockOrientation(context: Context) {
        (getActivityFromCtx(context) ?: PowerfulSama.getCurrentActivity())?.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    /** Dismisses the message. */
    @Suppress("DEPRECATION")
    fun dismiss() {
        logVerbose("Dismiss message")
        launch(Dispatchers.Main) {
            when (messageImpl) {
                MessageImpl.ProgressDialog -> {
                    (implementation?.get() as? ProgressDialog?)?.let {
                        if (lockOrientation) {
                            unlockOrientation(it.context)
                        }
                        if (it.isShowing) {
                            it.dismiss()
                        }
                    }
                }
                MessageImpl.AlertDialogOneButton, MessageImpl.AlertDialog -> {
                    (implementation?.get() as? AlertDialog?)?.let {
                        if (lockOrientation) {
                            unlockOrientation(it.context)
                        }
                        if (it.isShowing) {
                            it.dismiss()
                        }
                    }
                }
                MessageImpl.Toast -> {
                    onDismiss?.invoke(this@Msg)
                    (implementation?.get() as? Toast?)?.cancel()
                    waitForDismiss = false
                }
                MessageImpl.Snackbar -> {
                    onDismiss?.invoke(this@Msg)
                    (implementation?.get() as? Snackbar?)?.let {
                        if (lockOrientation) {
                            unlockOrientation(it.context)
                        }
                        if (it.isShown) {
                            it.dismiss()
                        }
                        waitForDismiss = false
                    }
                }
                else -> logError("Cannot understand the implementation type of the message. Skipping dismiss")
            }
            autoDismissJob?.cancel()
        }
    }

    /** Returns if the message is showing (Toasts will always return false). */
    @Suppress("DEPRECATION")
    fun isShowing() =
        when (messageImpl) {
            MessageImpl.ProgressDialog -> (implementation?.get() as? ProgressDialog?)?.isShowing
            MessageImpl.AlertDialogOneButton, MessageImpl.AlertDialog ->
                (implementation?.get() as? AlertDialog?)?.isShowing
            MessageImpl.Toast -> false
            MessageImpl.Snackbar -> (implementation?.get() as? Snackbar?)?.isShown
            else -> {
                logError("Cannot understand the implementation type of the message. Skipping isShowing")
                false
            }
        }

    /** Retrieves the strings from the ids. */
    private fun initStrings(context: Context) {
        if (iTitle != 0) title = context.getString(iTitle)
        if (iMessage != 0) message = context.getString(iMessage)
        if (iPositive != 0) positive = context.getString(iPositive)
        if (iNegative != 0) negative = context.getString(iNegative)
        if (iNeutral != 0) neutral = context.getString(iNeutral)
    }

    @Suppress("DEPRECATION")
    private fun buildAsProgressDialog(context: Context): Msg {
        val progressDialog = ProgressDialog(context, theme ?: 0)
        progressDialog.setTitle(title)
        progressDialog.setMessage(message)
        progressDialog.isIndeterminate = indeterminate
        progressDialog.setCancelable(cancelable)
        progressDialog.setOnDismissListener {
            onDismiss?.invoke(this)
            waitForDismiss = false
        }
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

        mAlert.setPositiveButton(positive) { dialog, _ ->
            onOk?.invoke(this)
            dialog.dismiss()
        }
        mAlert.setNegativeButton(negative) { dialog, _ ->
            onNo?.invoke(this)
            dialog.dismiss()
        }
        mAlert.setOnDismissListener {
            onDismiss?.invoke(this)
            waitForDismiss = false
        }

        if (!TextUtils.isEmpty(neutral)) {
            mAlert.setNeutralButton(neutral) { dialog, _ ->
                onCancel?.invoke(this)
                dialog.dismiss()
            }
        }

        implementation = WeakReference(mAlert.create())
        isBuilt = true
        return this
    }

    private fun buildAsAlertDialogOneButton(activity: Activity): Msg {
        val mAlert = AlertDialog.Builder(activity, theme ?: 0)

        mAlert.setCancelable(cancelable)
        mAlert.setTitle(title)
        mAlert.setMessage(message)

        mAlert.setPositiveButton(positive) { dialog, _ ->
            onOk?.invoke(this)
            dialog.dismiss()
        }
        mAlert.setOnDismissListener {
            onDismiss?.invoke(this)
            waitForDismiss = false
        }

        implementation = WeakReference(mAlert.create())
        isBuilt = true
        return this
    }

    private fun buildAsToast(context: Context): Msg {
        val d = when (duration) {
            LENGHT_SHORT -> Toast.LENGTH_SHORT
            LENGHT_LONG -> Toast.LENGTH_LONG
            else -> duration
        }
        implementation = WeakReference(Toast.makeText(context, message, d))
        isBuilt = true
        return this
    }

    private fun buildAsSnackbar(view: View): Msg {
        val d = when (duration) {
            LENGHT_SHORT -> Snackbar.LENGTH_SHORT
            LENGHT_LONG -> Snackbar.LENGTH_LONG
            LENGHT_INDEFINITE -> Snackbar.LENGTH_INDEFINITE
            else -> duration
        }
        val snackbar = Snackbar.make(view, message, d)
        if (onOk != null) {
            snackbar.setAction(positive) { onOk?.invoke(this) }
        }
        implementation = WeakReference(snackbar)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return uid == (other as Msg?)?.uid
    }

    override fun hashCode(): Int = (uid xor uid.ushr(32)).toInt()

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

        const val LENGHT_SHORT = -1
        const val LENGHT_LONG = -2
        const val LENGHT_INDEFINITE = -3

        override val coroutineContext = coroutineSamaHandler(SupervisorJob())

        /** Shared unique id used to check equality with other messages . */
        private val uniqueId: AtomicLong = AtomicLong(0)

        /** Default "Yes" string id. */
        internal var defaultYes: Int = android.R.string.yes

        /** Default "No" string id. */
        internal var defaultNo: Int = android.R.string.no

        /** Default theme used by messages. */
        internal var defaultTheme: Int? = null

        /** Default customization function. Note: It will be called on UI thread. */
        internal var defaultCustomization: ((Any) -> Unit)? = null

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
            customize = defaultCustomization
        )

        /** Alias for [alertDialogOneButton] with specified [messageId]. */
        fun adob(messageId: Int) = alertDialogOneButton().message(messageId)

        /** Alias for [alertDialogOneButton] with specified [message]. */
        fun adob(message: String) = alertDialogOneButton().message(message)

        /**
         * Creates a ProgressDialog with specified options, with an optional [theme].
         *
         * Default values:
         *
         *      messageImpl = MessageImpl.ProgressDialog
         */
        fun progressDialog(theme: Int? = null) = Msg(
            theme = theme ?: defaultTheme,
            messageImpl = MessageImpl.ProgressDialog,
            customize = defaultCustomization
        )

        /** Alias for [progressDialog] with specified [messageId]. */
        fun pd(messageId: Int) = progressDialog().message(messageId)

        /** Alias for [progressDialog] with specified [message]. */
        fun pd(message: String) = progressDialog().message(message)

        /**
         * Creates an alertDialog with specified options, with an optional [theme].
         * Default values:
         *
         *      messageImpl = MessageImpl.AlertDialog
         */
        fun alertDialog(theme: Int? = null) = Msg(
            theme = theme ?: defaultTheme,
            messageImpl = MessageImpl.AlertDialog,
            customize = defaultCustomization
        )

        /** Alias for [alertDialog] with specified [messageId]. */
        fun ad(messageId: Int) = alertDialog().message(messageId)

        /** Alias for [alertDialog] with specified [message]. */
        fun ad(message: String) = alertDialog().message(message)

        /**
         * Creates a Toast with specified message.
         *
         * Default values:
         *
         *      messageImpl = MessageImpl.Toast
         */
        fun toast() = Msg(messageImpl = MessageImpl.Toast, customize = defaultCustomization)

        /** Alias for [toast] with specified [messageId]. */
        fun ts(messageId: Int) = toast().message(messageId)

        /** Alias for [toast] with specified [message]. */
        fun ts(message: String) = toast().message(message)

        /**
         * Creates a Snackbar with specified title and okRunnable.
         *
         * Default values:
         *
         *      messageImpl = MessageImpl.Snackbar
         */
        fun snackbar(view: View) = Msg(
            messageImpl = MessageImpl.Snackbar,
            snackbarView = view,
            customize = defaultCustomization
        )

        /** Alias for [snackbar] with specified [messageId]. */
        fun sb(view: View, messageId: Int) = snackbar(view).message(messageId)

        /** Alias for [snackbar] with specified [message]. */
        fun sb(view: View, message: String) = snackbar(view).message(message)
    }

    /** Implementation type used to show and dismiss the message . */
    private enum class MessageImpl {
        ProgressDialog,
        AlertDialogOneButton,
        AlertDialog,
        Toast,
        Snackbar
    }
}
