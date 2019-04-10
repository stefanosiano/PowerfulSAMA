package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import com.stefanosiano.powerful_libraries.sama.addOnChangedAndNow
import com.stefanosiano.powerful_libraries.sama.getKey
import com.stefanosiano.powerful_libraries.sama.toWeakReference
import com.stefanosiano.powerful_libraries.sama.utils.WeakPair
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob


/**
 * Custom Spinner that uses data binding to always have the most updated values.
 * It supports a collection of strings, or a collection of [SamaSpinnerItem] (pairs key, value).
 */
class SamaSpinner : AppCompatSpinner, CoroutineScope {

    override val coroutineContext = SupervisorJob() + CoroutineExceptionHandler { _, t -> t.printStackTrace() }

    private lateinit var arrayAdapter: ArrayAdapter<String>
    private val itemMap = HashMap<String, String>()
    private var showValue = false
    private var valuesOnly = false

    /** observableString that contains always the value of the current shown item. If it was initialized with a collection of strings, it equals [currentKey] */
    private val currentValue = ObservableField<String>()

    /** observableString that contains always the key of the current shown item. If it was initialized with a collection of strings, it equals [currentValue] */
    private val currentKey = ObservableField<String>()

    /** Set of observable strings to update when an item is selected. It will contain the key */
    private val obserablesKeySet: MutableSet<WeakPair<ObservableField<String>, Observable.OnPropertyChangedCallback>> = HashSet()

    /** Set of observable strings to update when an item is selected. It will contain the value */
    private val obserablesValueSet: MutableSet<WeakPair<ObservableField<String>, Observable.OnPropertyChangedCallback>> = HashSet()

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}



    init {
        currentKey.addOnChangedAndNow { key -> obserablesKeySet.forEach { it.first()?.set(key) } }
        currentValue.addOnChangedAndNow { value -> obserablesValueSet.forEach { it.first()?.set(value) } }
    }

    /**
     * Initializes the spinner
     *
     * @param spinnerLayoutId layout id of the spinner items
     * @param items collection of [SamaSpinnerItem] to show
     * @param showValue if true it shows the value, otherwise it shows the key
     * @param key key of the first default item to show (ignored if null)
     * @param value value of the first default item to show (ignored if null)
     */
    fun init(spinnerLayoutId: Int, items: Collection<SamaSpinnerItem>, showValue: Boolean = true, key: String? = null, value: String? = null) {
        arrayAdapter = ArrayAdapter(context, spinnerLayoutId)
        valuesOnly = false
        this.showValue = showValue
        itemMap.clear()

        if(key != null && value != null) {
            arrayAdapter.add(if(showValue) value else key)
            itemMap.put(key, value)
        }

        arrayAdapter.addAll(items.map { if(showValue) it.value() else it.key() })
        items.map { itemMap.put(it.key(), it.value()) }

        super.setAdapter(arrayAdapter)
        init()
    }

    /**
     * Initializes the spinner
     *
     * @param spinnerLayoutId layout id of the spinner items
     * @param items array of [SamaSpinnerItem] to show
     * @param showValue if true it shows the value, otherwise it shows the key
     * @param key key of the first default item to show (ignored if null)
     * @param value value of the first default item to show (ignored if null)
     */
    fun init(spinnerLayoutId: Int, items: Array<out SamaSpinnerItem>, showValue: Boolean = true, key: String? = null, value: String? = null) =
        init(spinnerLayoutId, items.toList(), showValue, key, value)



    /**
     * Initializes the spinner
     *
     * @param spinnerLayoutId layout id of the spinner items
     * @param items collection of [String] to show
     * @param value value of the first default item to show (ignored if null)
     */
    fun init(spinnerLayoutId: Int, items: Collection<String>, value: String? = null) {
        showValue = false
        valuesOnly = true
        arrayAdapter = ArrayAdapter(context, spinnerLayoutId)
        itemMap.clear()

        if(value != null) arrayAdapter.add(value)

        arrayAdapter.addAll(items)
        super.setAdapter(arrayAdapter)
        init()
    }
    /**
     * Initializes the spinner
     *
     * @param spinnerLayoutId layout id of the spinner items
     * @param items array of [String] to show
     * @param value value of the first default item to show (ignored if null)
     */
    fun init(spinnerLayoutId: Int, items: Array<out String>, value: String? = null) = init(spinnerLayoutId, items.toList(), value)

    /** Common initialization of the spinner */
    private fun init() {
        onItemSelectedListener = object: OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val key = getSelectedKey()
                currentKey.set(key)
                currentValue.set(itemMap[key])
            }
        }

        val key = getSelectedKey()
        currentKey.set(key)
        currentValue.set(itemMap[key])
    }


    /** Sets the selected items to the shown item that matches [value]. It does nothing if it's not found */
    fun setSelection(value: String) { (0 until adapter.count).firstOrNull { adapter.getItem(it) == value }?.let { setSelection(it) } }

    /** Gets the current shown item */
    fun getSelection(): String = selectedItem as String

    /** Sets the selection of the spinner to the first occurrence of [value]. If it was initialized with a collection of strings, it calls [setSelection] */
    fun setSelectedValue(value: String) {
        if(valuesOnly) return setSelection(value)
        (0 until adapter.count).firstOrNull { adapter.getItem(it) == if(showValue) value else itemMap.getKey(value) }?.let { setSelection(it) }
    }

    /** Sets the selection of the spinner to the first occurrence of [key]. If it was initialized with a collection of strings, it calls [setSelection] */
    fun setSelectedKey(key: String) {
        if(valuesOnly) return setSelection(key)
        (0 until adapter.count).firstOrNull { adapter.getItem(it) == if(showValue) itemMap[key] else key }?.let { setSelection(it) }
    }

    /** Gets the value associated to the currently shown item. If it was initialized with a collection of strings, it calls [getSelection] */
    fun getSelectedValue(): String? {
        if(valuesOnly) return getSelection()
        return if(showValue) selectedItem as? String else itemMap.get(getSelectedKey())
    }

    /** Gets the key associated to the currently shown item. If it was initialized with a collection of strings, it calls [getSelection] */
    fun getSelectedKey(): String? {
        if(valuesOnly) return getSelection()
        return if(showValue) getSelectedValue().let { itemMap.getKey(it) } else selectedItem as? String
    }


    /**
     * Adds the observable strings to the internal list.
     * When an item is selected, all registered observables are updated. When one observable changes, the item is selected.
     * If it was initialized with a collection of strings, it will contain the current shown value, otherwise the key associated to it
     */
    fun bindKey(obs: ObservableField<String>) {
        val spinner = this.toWeakReference()
        val weakObs = obs.toWeakReference()
        val callback = obs.addOnChangedAndNow {
            weakObs.get()?.also { obs ->
                if (obserablesKeySet.firstOrNull { set -> set.first() == obs } != null && obs.get() != currentKey.get())
                    obs.get()?.let { spinner.get()?.setSelectedKey(it) }
            }
        }

        weakObs.get()?.let { obserablesKeySet.add(WeakPair(it, callback)) }
        currentKey.get()?.let { obs.set(it) }
    }

    /**
     * Adds the observable strings to the internal list.
     * When an item is selected, all registered observables are updated. When one observable changes, the item is selected.
     * If it was initialized with a collection of strings, it will contain the current shown value, otherwise the value associated to it
     */
    fun bindValue(obs: ObservableField<String>) {
        val spinner = this.toWeakReference()
        val weakObs = obs.toWeakReference()
        val callback = obs.addOnChangedAndNow {
            weakObs.get()?.also { obs ->
                if (obserablesValueSet.firstOrNull { set -> set.first() == obs } != null && obs.get() != currentValue.get())
                    obs.get()?.let { spinner.get()?.setSelectedValue(it) }
            }
        }

        weakObs.get()?.let { obserablesValueSet.add(WeakPair(it, callback)) }
        currentValue.get()?.let { obs.set(it) }
    }




    /** Simple class with 2 simple fields that implements [SamaSpinnerItem] */
    class SimpleSpinnerItem(private val key: String, private val value: String) : SamaSpinnerItem{
        override fun value() = value
        override fun key() = key
    }

    /** Interface used to populate a [SamaSpinner] */
    interface SamaSpinnerItem {

        /** Returns the value of the item */
        fun value(): String

        /** Returns the key of the item */
        fun key(): String
    }
}
