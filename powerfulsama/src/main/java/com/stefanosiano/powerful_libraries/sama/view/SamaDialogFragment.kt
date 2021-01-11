package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseArray
import android.view.View
import androidx.databinding.*
import androidx.lifecycle.LiveData
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.utils.ObservableF
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.atomic.AtomicInteger

/** Abstract DialogFragment for all DialogFragments to extend. It includes a dialogFragment usable by subclasses
 * [layoutId] and [dataBindingId] are used to create the underlying dialogFragment.
 * [uid] is used to restore and reopen the dialog if instantiated through [SamaActivity.manageDialog] (value -1 is ignored).
 * It's automatically generated based on the class name. Customize it if you have more then 1 of these dialogs at the same time.
 * If you want more control over them override [getDialogLayout] and [getDialogDataBindingId] and/ot [bindingData] */
abstract class SamaDialogFragment(
    private val layoutId: Int,
    private val dataBindingId: Int,
    private val bindingData: Any? = null,
    private var uid: Int = -1
): CoroutineScope {

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Object that takes care of observing liveData and observableFields */
    private val samaObserver = SamaObserver(this)

    protected var dialogFragment: SimpleSamaDialogFragment? = SimpleSamaDialogFragment.new(getDialogLayout(), isFullWidth(), isFullHeight()).with(getDialogDataBindingId(), bindingData ?: this)

    protected open fun isFullWidth() = true
    protected open fun isFullHeight() = false

    internal fun getUidInternal() = uid

    init {
        if(uid == -1)
            uid = uidMap.getOrPut(this::class.java.name) { lastUid.incrementAndGet() }
    }

    /** Restore previous data from events like device rotating when a dialog is shown. The [dialogFragment] in [oldDialog] is null */
    abstract fun restore(oldDialog: SamaDialogFragment)

    companion object {
        val map = SparseArray<SamaDialogFragment>()
        val uidMap = HashMap<String, Int>()
        val lastUid = AtomicInteger(0)
    }

    internal fun onResumeRestore(activity: SamaActivity) {
        (map.get(uid, null))?.let { t ->
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
        samaObserver.destroyObserver()
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
        samaObserver.restartObserver()
    }

    /** Dismiss the dialog through [dismissAllowingStateLoss] */
    fun dismiss() {
        samaObserver.stopObserver()
        if(dialogFragment?.isAdded == true) dialogFragment?.dismissAllowingStateLoss()
        map.remove(uid)
    }

    /** Dismiss the dialog through [dismissAllowingStateLoss] */
    fun dismissDialog(v: View) { dismiss() }

    fun getSamaActivity() = dialogFragment?.activity as? SamaActivity?








    /** Observes a liveData until this object is destroyed, using a custom observer. Useful when liveData is not used in a lifecycleOwner */
    protected fun <T> observe(liveData: LiveData<T>): LiveData<T> = samaObserver.observe(liveData)
    /** Observes a liveData until this object is destroyed into an observable field. Does not update the observable if the value of the liveData is null */
    protected fun <T> observeAsOf(liveData: LiveData<T>): ObservableField<T> = samaObserver.observeAsOf(liveData)
    /** Observes a liveData until this object is destroyed into an observableF. Update the observable with [defaultValue] if the value of the liveData is null */
    protected fun <T> observeAsOf(liveData: LiveData<T>, defaultValue: T): ObservableF<T> = samaObserver.observeAsOf(liveData, defaultValue)
    /** Observes a liveData until this object is destroyed, using a custom observer */
    protected fun <T> observe(liveData: LiveData<T>, observerFunction: suspend (data: T) -> Unit): LiveData<T> = samaObserver.observe(liveData, observerFunction)

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed */
    protected fun <T> observe(o: ObservableList<T>, vararg obs: Observable, obFun: suspend (data: ObservableList<T>) -> Unit): Unit where T: Any = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableInt, defValue: R, vararg obs: Observable, obFun: suspend (data: Int) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableInt, vararg obs: Observable, obFun: suspend (data: Int) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableLong, defValue: R, vararg obs: Observable, obFun: suspend (data: Long) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableLong, vararg obs: Observable, obFun: suspend (data: Long) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableByte, defValue: R, vararg obs: Observable, obFun: suspend (data: Byte) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableByte, vararg obs: Observable, obFun: suspend (data: Byte) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableChar, defValue: R, vararg obs: Observable, obFun: suspend (data: Char) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableChar, vararg obs: Observable, obFun: suspend (data: Char) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableBoolean, defValue: R, vararg obs: Observable, obFun: suspend (data: Boolean) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableBoolean, vararg obs: Observable, obFun: suspend (data: Boolean) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableFloat, defValue: R, vararg obs: Observable, obFun: suspend (data: Float) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableFloat, vararg obs: Observable, obFun: suspend (data: Float) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableDouble, defValue: R, vararg obs: Observable, obFun: suspend (data: Double) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableDouble, vararg obs: Observable, obFun: suspend (data: Double) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R, T> observe(o: ObservableField<T>, defValue: R, vararg obs: Observable, obFun: suspend (data: T) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R, T> observe(o: ObservableField<T>, vararg obs: Observable, obFun: suspend (data: T) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

}
