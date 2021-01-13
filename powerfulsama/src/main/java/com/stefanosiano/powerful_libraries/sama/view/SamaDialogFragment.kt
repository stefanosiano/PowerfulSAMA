package com.stefanosiano.powerful_libraries.sama.view

import android.content.DialogInterface
import android.os.Bundle
import android.util.SparseArray
import android.view.*
import androidx.databinding.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.utils.ObservableF
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger

/** Abstract DialogFragment for all DialogFragments to extend. It includes a dialogFragment usable by subclasses
 * [layoutId] and [dataBindingId] are used to create the underlying dialogFragment. [dialogBindingId] is used to bind the [SimpleSamaDialogFragment] directly, if it's not -1.
 * [uid] is used to restore and reopen the dialog if instantiated through [SamaActivity.manageDialog] (value -1 is ignored). Values over 10000 are used internally, so use lower values.
 * It's automatically generated based on the class name. Customize it if you have more then 1 of these dialogs at the same time.
 * If you want more control over them override [getDialogLayout] and [getDialogDataBindingId] and/ot [bindingData] */
abstract class SamaDialogFragment(
    private val layoutId: Int,
    private val dataBindingId: Int = -1,
    private val fullWidth: Boolean = false,
    private val fullHeight: Boolean = false,
    private val dialogBindingId: Int = -1,
    private var uid: Int = -1
): DialogFragment(), CoroutineScope {

    companion object {
        val map = SparseArray<SamaDialogFragment>()
        val uidMap = HashMap<String, Int>()
        val lastUid = AtomicInteger(10000)
    }

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    private val bindingPairs: MutableList<Pair<Int, Any>> = ArrayList()

    /** Object that takes care of observing liveData and observableFields */
    private val samaObserver = SamaObserver(this)

    protected var enableAutoManagement = true

    internal fun getUidInternal() = uid

    init {
        if(dialogBindingId != -1 && !this.bindingPairs.asSequence().map { it.first }.contains(dialogBindingId))
            this.bindingPairs.add(Pair(dialogBindingId, this))
        if(uid == -1)
            uid = uidMap.getOrPut(this::class.java.name) { lastUid.incrementAndGet() }
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
        if(isAdded && map.get(uid, null) != null) {
            dismiss()
            if(activity.isChangingConfigurations)
                map.put(uid, this)
            else
                map.remove(uid)
        }
        else
            dismiss()
    }

    /** Restore previous data from events like device rotating when a dialog is shown. The [dialogFragment] in [oldDialog] is null */
    abstract fun restore(oldDialog: SamaDialogFragment)

    /**
     * Sets the data to work with data binding
     * Calling this method multiple times will associate the id to the last data passed.
     * Multiple dataBindingIds are allowed
     *
     * @param dataBindingId the id of the variable in the layout
     * @param bindingData the data to bind to the id
     */
    fun with(dataBindingId: Int, bindingData: Any): SamaDialogFragment {
        if(!this.bindingPairs.asSequence().map { it.first }.contains(dataBindingId))
            this.bindingPairs.add(Pair(dataBindingId, bindingData))
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); logVerbose("onCreate") }
    override fun onResume() { super.onResume(); logVerbose("onResume") }
    override fun onStart() {
        super.onStart(); logVerbose("onStart")
        if(fullWidth || fullHeight) {
            dialog?.window?.setLayout(if(fullWidth) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
                if(fullHeight) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
    override fun onPause() { super.onPause(); logVerbose("onPause") }
    override fun onStop() { super.onStop(); logVerbose("onStop") }
    override fun onDestroy() {
        super.onDestroy(); logVerbose("onDestroy")
        samaObserver.destroyObserver()
        if(activity?.isFinishing == true)
            map.remove(uid)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        with(dataBindingId, this)
        if(bindingPairs.isNotEmpty()) {
            val binding: ViewDataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)
            for (pair in bindingPairs)
                binding.setVariable(pair.first, pair.second)
            return binding.root
        }

        return inflater.inflate(layoutId, container, false)
    }

    /** Function used to call [dismiss] from dataBinding */
    @Suppress("UNUSED_PARAMETER")
    fun dismiss(view: View) = dismiss()

    /** Returns whether this dialog should reopen itself on activity resume after device was rotated. Defaults to true */
    open fun autoRestore(): Boolean = true

    /** Shows the dialog over the [activity]. If [enableAutoManagement] is set, or if the dialog was created through [SamaActivity.manageDialog],
     * when the activity is destroyed (e.g. rotating device) it automatically dismisses the dialog */
    open fun show(activity: SamaActivity) {
        if(enableAutoManagement) activity.manageDialogInternal(this)
        if(isAdded) return
        super.show(activity.supportFragmentManager, tag)
        map.put(uid, this)
        samaObserver.restartObserver()
    }

    /** Dismiss the dialog through [dismissAllowingStateLoss] */
    override fun dismiss() {
        if(isAdded) super.dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        samaObserver.stopObserver()
        map.remove(uid)
        super.onDismiss(dialog)
    }

    /** Dismiss the dialog through [dismissAllowingStateLoss] */
    fun dismissDialog(v: View) { if(isAdded) super.dismissAllowingStateLoss() }















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
