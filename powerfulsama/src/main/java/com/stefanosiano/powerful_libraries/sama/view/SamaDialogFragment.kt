package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseArray
import android.view.View
import androidx.fragment.app.DialogFragment
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserverImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.atomic.AtomicInteger

/**
 * Abstract DialogFragment for all DialogFragments to extend.
 * It includes a dialogFragment usable by subclasses.
 * [layoutId] and [dataBindingId] are used to create the underlying dialogFragment.
 * [dialogBindingId] is used to bind the [SimpleSamaDialogFragment] directly, if it's not -1.
 * [uid] is used to restore and reopen the dialog if instantiated through
 *  [SamaActivity.manageDialog] (value -1 is ignored).
 *  Values over 10000 are used internally, so use lower values.
 * It's automatically generated based on the class name.
 * Customize it if you have more then 1 of these dialogs at the same time.
 * If you want more control over them override [getDialogLayout] and [getDialogDataBindingId]
 *  and/or [bindingData].
 */
abstract class SamaDialogFragment(
    private val layoutId: Int,
    private val dataBindingId: Int = -1,
    private val bindingData: Any? = null,
    private val fullWidth: Boolean = true,
    private val fullHeight: Boolean = false,
    private var uid: Int = -1
) : CoroutineScope, SamaObserver by SamaObserverImpl() {

    private val coroutineJob: Job = SupervisorJob()

    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Actual dialog object. You may use it to customize the dialog. */
    protected var dialogFragment: SimpleSamaDialogFragment? = SimpleSamaDialogFragment.new(
        getDialogLayout(),
        fullWidth,
        fullHeight
    ).with(getDialogDataBindingId(), bindingData ?: this)

    /**
     * Flag to know whether to automatically close the dialog when the activity is destroyed and
     *  to reopen when the activity restarts (if instantiated through [SamaActivity.manageDialog].
     */
    protected var enableAutoManagement = true

    init {
        initObserver(this)
        if (uid == -1) {
            uid = uidMap.getOrPut(this::class.java.name) { lastUid.incrementAndGet() }
        }
    }

    internal fun getUidInternal() = uid

    /**
     * Restore previous data from events like device rotating when a dialog is shown.
     * The [dialogFragment] in [oldDialog] is null.
     */
    abstract fun restore(oldDialog: SamaDialogFragment)

    internal fun onResumeRestore(activity: SamaActivity) {
        map.get(uid, null)?.let { fragment ->
            restore(fragment)
            if (enableAutoManagement) {
                show(activity)
            } else {
                map.remove(uid)
            }
        }
    }

    internal fun onSaveInstanceState(activity: SamaActivity) {
        if (dialogFragment?.isAdded == true && map.get(uid, null) != null) {
            dismiss()
            dialogFragment = null
            if (activity.isChangingConfigurations) {
                map.put(uid, this)
            } else {
                map.remove(uid)
            }
        } else {
            dismiss()
        }
    }

    internal fun onDestroy(activity: SamaActivity) {
        destroyObserver()
        if (activity.isFinishing) {
            map.remove(uid)
        }
    }

    /** Return the layout used by the dialog fragment. Defaults to [layoutId]. */
    protected open fun getDialogLayout(): Int = layoutId

    /** Return the data binding id used by the dialog fragment. Defaults to [dataBindingId]. */
    protected open fun getDialogDataBindingId(): Int = dataBindingId

    /**
     * Shows the dialog over the [activity].
     * If [enableAutoManagement] is set, or if the dialog was created through
     *  [SamaActivity.manageDialog], when the activity is destroyed (e.g. rotating device)
     *  it automatically dismisses the dialog.
     */
    open fun show(activity: SamaActivity) {
        if (enableAutoManagement) activity.manageDialogInternal(this)
        if (dialogFragment?.isAdded == true) return
        dialogFragment?.show(activity.supportFragmentManager)
        map.put(uid, this)
        startObserver()
    }

    /** Dismiss the dialog through [DialogFragment.dismissAllowingStateLoss]. */
    @Suppress("UnusedPrivateMember")
    fun dismiss(v: View) = dismiss()

    /** Dismiss the dialog through [DialogFragment.dismissAllowingStateLoss]. */
    fun dismiss() {
        stopObserver()
        map.remove(uid)
        onDismiss()
        if (dialogFragment?.isAdded == true) {
            dialogFragment?.dismissAllowingStateLoss()
        }
    }

    /** Method called when the dialog is been dismissed (right before). */
    open fun onDismiss() {}

    companion object {
        private val map = SparseArray<SamaDialogFragment>()
        private val uidMap = HashMap<String, Int>()
        private val lastUid = AtomicInteger(10000)
    }
}
