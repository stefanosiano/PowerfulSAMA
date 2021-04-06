package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseArray
import android.view.*
import androidx.databinding.*
import androidx.lifecycle.LiveData
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserverImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.atomic.AtomicInteger

/** Abstract DialogFragment for all DialogFragments to extend. It includes a dialogFragment usable by subclasses
 * [layoutId] and [dataBindingId] are used to create the underlying dialogFragment. [dialogBindingId] is used to bind the [SimpleSamaDialogFragment] directly, if it's not -1.
 * [uid] is used to restore and reopen the dialog if instantiated through [SamaActivity.manageDialog] (value -1 is ignored). Values over 10000 are used internally, so use lower values.
 * It's automatically generated based on the class name. Customize it if you have more then 1 of these dialogs at the same time.
 * If you want more control over them override [getDialogLayout] and [getDialogDataBindingId] and/ot [bindingData] */
abstract class SamaDialogFragment(
    private val layoutId: Int,
    private val dataBindingId: Int = -1,
    private val bindingData: Any? = null,
    private val fullWidth: Boolean = true,
    private val fullHeight: Boolean = false,
    private var uid: Int = -1
): CoroutineScope {

    companion object {
        val map = SparseArray<SamaDialogFragment>()
        val uidMap = HashMap<String, Int>()
        val lastUid = AtomicInteger(10000)
    }

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Object that takes care of observing liveData and observableFields */
    private val samaObserver: SamaObserver = SamaObserverImpl()

    /** Actual dialog object. You may use it to customize the dialog */
    protected var dialogFragment: SimpleSamaDialogFragment? = SimpleSamaDialogFragment.new(getDialogLayout(), fullWidth, fullHeight).with(getDialogDataBindingId(), bindingData ?: this)

    /** Flag to know whether to automatically close the dialog when the activity is destroyed and to reopen when the activity restarts (if instantiated through [SamaActivity.manageDialog] */
    protected var enableAutoManagement = true

    internal fun getUidInternal() = uid

    init {
        samaObserver.initObserver(this)
        if(uid == -1)
            uid = uidMap.getOrPut(this::class.java.name) { lastUid.incrementAndGet() }
    }

    /** Restore previous data from events like device rotating when a dialog is shown. The [dialogFragment] in [oldDialog] is null */
    abstract fun restore(oldDialog: SamaDialogFragment)

    internal fun onResumeRestore(activity: SamaActivity) {
        (map.get(uid, null))?.let { t ->
            restore(t)
            if(enableAutoManagement)
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
        samaObserver.destroyObserver()
        if(activity.isFinishing)
            map.remove(uid)
    }

    /** Return the layout used to create the dialog fragment. Defaults to [layoutId] of constructor */
    protected open fun getDialogLayout(): Int = layoutId

    /** Return the data binding id used to create the dialog fragment. Defaults to [dataBindingId] of constructor */
    protected open fun getDialogDataBindingId(): Int = dataBindingId

    /** Shows the dialog over the [activity]. If [enableAutoManagement] is set, or if the dialog was created through [SamaActivity.manageDialog],
     * when the activity is destroyed (e.g. rotating device) it automatically dismisses the dialog */
    open fun show(activity: SamaActivity) {
        if(enableAutoManagement) activity.manageDialogInternal(this)
        if(dialogFragment?.isAdded == true) return
        dialogFragment?.show(activity.supportFragmentManager)
        map.put(uid, this)
        samaObserver.startObserver()
    }

    /** Dismiss the dialog through [dismissAllowingStateLoss] */
    fun dismiss(v: View) = dismiss()

    /** Dismiss the dialog through [dismissAllowingStateLoss] */
    fun dismiss() {
        samaObserver.stopObserver()
        map.remove(uid)
        onDismiss()
        if(dialogFragment?.isAdded == true) dialogFragment?.dismissAllowingStateLoss()
    }

    /** Method called when the dialog is been dismissed (right before) */
    open fun onDismiss() {}

    /** Dismiss the dialog through [dismissAllowingStateLoss] */
    fun dismissDialog(v: View) { dismiss() }















    /** Observes a liveData until this object is destroyed into an observable field. Does not update the observable if the value of the liveData is null */
    protected fun <T> observeAsOf(liveData: LiveData<T>): ObservableField<T> = samaObserver.observeAsOf(liveData)
    /** Observes a liveData until this object is destroyed, using a custom observer */
    protected fun <T> observe(liveData: LiveData<T>, observerFunction: (data: T) -> Unit): LiveData<T> = samaObserver.observe(liveData, observerFunction)

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed */
    protected fun <T> observe(o: ObservableList<T>, vararg obs: Observable, obFun: (data: List<T>) -> Unit): Unit = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableInt, vararg obs: Observable, obFun: (data: Int) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableLong, vararg obs: Observable, obFun: (data: Long) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableByte, vararg obs: Observable, obFun: (data: Byte) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableChar, vararg obs: Observable, obFun: (data: Char) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableBoolean, vararg obs: Observable, obFun: (data: Boolean) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableFloat, vararg obs: Observable, obFun: (data: Float) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableDouble, vararg obs: Observable, obFun: (data: Double) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R, T> observe(o: ObservableField<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }
}
