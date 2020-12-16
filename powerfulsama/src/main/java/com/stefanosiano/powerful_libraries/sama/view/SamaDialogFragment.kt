package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseArray
import android.view.View
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.atomic.AtomicInteger

/** Abstract DialogFragment for all DialogFragments to extend. It includes a dialogFragment usable by subclasses
 * [layoutId] and [dataBindingId] are used to create the underlying dialogFragment.
 * [uid] is used to restore and reopen the dialog if instantiated through [SamaActivity.manageDialog] (value -1 is ignored).
 * It's automatically generated based on the class name. Customize it if you have more then 1 of these dialogs at the same time.
 * If you want more control over them override [getDialogLayout] and [getDialogDataBindingId] and/ot [bindingData] */
abstract class SamaDialogFragment<T>(
    private val layoutId: Int,
    private val dataBindingId: Int,
    private val bindingData: Any? = null,
    private var uid: Int = -1
): CoroutineScope where T: SamaDialogFragment<T> {

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    protected var dialogFragment: SimpleSamaDialogFragment? = SimpleSamaDialogFragment.new(getDialogLayout(), isFullWidth(), isFullHeight()).with(getDialogDataBindingId(), bindingData ?: this)

    protected open fun isFullWidth() = true
    protected open fun isFullHeight() = false

    internal fun getUidInternal() = uid

    init {
        if(uid == -1)
            uid = uidMap.getOrPut(this::class.java.name) { lastUid.incrementAndGet() }
    }

    /** Restore previous data from events like device rotating when a dialog is shown. The [dialogFragment] in [oldDialog] is null */
    abstract fun restore(oldDialog: T)

    companion object {
        val map = SparseArray<SamaDialogFragment<*>>()
        val uidMap = HashMap<String, Int>()
        val lastUid = AtomicInteger(0)
    }

    internal fun onResumeRestore(activity: SamaActivity) {
        (map.get(uid, null) as? T)?.let { t ->
            restore(t)
            if(autoRestore())
                show(activity)
            else
                map.remove(uid)
        }
    }

    internal fun onSaveInstanceState(activity: SamaActivity) {
        if(dialogFragment?.isAdded == true && map.get(uid, null) != null) {
            dismiss()
            dialogFragment = null
            if(activity.isChangingConfigurations)
                map.put(uid, this)
            else
                map.remove(uid)
        }
        else
            dismiss()
    }

    internal fun onDestroy(activity: SamaActivity) {
        if(activity.isFinishing)
            map.remove(uid)
    }

    /** Returns whether this dialog should reopen itself on activity resume after device was rotated. Defaults to true */
    open fun autoRestore(): Boolean = true

    /** Return the layout used to create the dialog fragment. Defaults to [layoutId] of constructor */
    protected open fun getDialogLayout(): Int = layoutId

    /** Return the data binding id used to create the dialog fragment. Defaults to [dataBindingId] of constructor */
    protected open fun getDialogDataBindingId(): Int = dataBindingId

    /** Shows the dialog over the [activity]. Also, when the activity is destroyed (e.g. rotating device), it automatically dismisses the dialog */
    open fun show(activity: SamaActivity) {
        if(dialogFragment?.isAdded == true) return
        dialogFragment?.show(activity.supportFragmentManager)
        map.put(uid, this)
    }

    /** Dismiss the dialog through [dismissAllowingStateLoss] */
    fun dismiss() { if(dialogFragment?.isAdded == true) dialogFragment?.dismissAllowingStateLoss(); map.remove(uid) }

    @Deprecated("use dismiss()")
            /** Dismiss the dialog through [dismissAllowingStateLoss]. Always use it in [SamaActivity.onSaveInstanceState] */
    fun dismissDialog() = dismiss()

    /** Dismiss the dialog through [dismissAllowingStateLoss] */
    fun dismissDialog(v: View) { dismiss() }

    fun getSamaActivity() = dialogFragment?.activity as? SamaActivity?
}
