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

    private var isInitialized = false

    private var tempItems: Collection<String>? = null

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}


    /** Common initialization of the spinner */
    init {
        onItemSelectedListener = object: OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(!isInitialized) return
                val key = getSelectedKey()
                currentKey.set(key)
                currentValue.set(itemMap[key])
            }
        }

        val key = getSelectedKey()
        currentKey.set(key)
        currentKey.addOnChangedAndNow { k -> currentValue.set(itemMap.get(k)); if(!isInitialized) return@addOnChangedAndNow; obserablesKeySet.forEach { it.first()?.set(k) } }
        currentValue.addOnChangedAndNow { v -> currentKey.set(itemMap.getKey(v)); if(!isInitialized) return@addOnChangedAndNow; obserablesValueSet.forEach { it.first()?.set(v) } }
    }


    /** Initializes the spinner, using [spinnerLayoutId] for the spinner items */
    fun init(spinnerLayoutId: Int) {
        isInitialized = false
        arrayAdapter = ArrayAdapter(context, spinnerLayoutId)
        super.setAdapter(arrayAdapter)
        isInitialized = true
        tempItems?.let { arrayAdapter.addAll(it) }
    }



    /** Sets [items] as the array of [SamaSpinnerItem] to show in the spinner, whenever it changes */
    fun bindItemsArray(items: ObservableField<Array<out SamaSpinnerItem>>, showValue: Boolean = true) = items.addOnChangedAndNow { if (it != null) setItems(it.toList(), showValue) }

    /** Sets [items] as the collection of [SamaSpinnerItem] to show in the spinner, whenever it changes */
    fun bindItems(items: ObservableField<Collection<SamaSpinnerItem>>, showValue: Boolean = true) = items.addOnChangedAndNow { if (it != null) setItems(it.toList(), showValue) }

    /** Sets [items] as the array of [String] to show in the spinner, whenever it changes */
    fun bindItemsArray(items: ObservableField<Array<out String>>) = items.addOnChangedAndNow { if (it != null) setItems(it.toList()) }

    /** Sets [items] as the collection of [String] to show in the spinner, whenever it changes */
    fun bindItems(items: ObservableField<Collection<String>>) = items.addOnChangedAndNow { if (it != null) setItems(it.toList()) }


    /** Sets [items] as the array of [SamaSpinnerItem] to show in the spinner */
    fun setItems(items: Array<out SamaSpinnerItem>, showValue: Boolean = true) = setItems(items.toList(), showValue)

    /** Sets [items] as the collection of [SamaSpinnerItem] to show in the spinner */
    fun setItems(items: Collection<SamaSpinnerItem>, showValue: Boolean = true) {
        itemMap.clear()
        items.map { itemMap.put(it.key(), it.value()) }
        val old = selectedItem as? String? ?: (if(showValue) currentValue.get() ?: itemMap[currentKey.get()] else currentKey.get() ?: itemMap.getKey(currentValue.get())) ?: ""
        refreshItems( items.map { if(showValue) it.value() else it.key() } )
        valuesOnly = false
        this.showValue = showValue
        if(showValue) setSelectedValue(old) else setSelectedKey(old)
    }

    /** Sets [items] as the array of [String] to show in the spinner */
    fun setItems(items: Array<out String>) = setItems(items.toList())

    /** Sets [items] as the collection of [String] to show in the spinner */
    fun setItems(items: Collection<String>) {
        itemMap.clear()
        val old = selectedItem as? String? ?: currentKey.get() ?: ""
        refreshItems(items)
        showValue = false
        valuesOnly = true
        setSelection(old)
    }

    /** Clear map and adapter, add new items and refresh adapter changes */
    private fun refreshItems(items: Collection<String>) {
        if(!isInitialized) { tempItems = items; return }
        arrayAdapter.clear()
        arrayAdapter.addAll(items)
        arrayAdapter.notifyDataSetChanged()
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
                    obs.get()?.let { spinner.get()?.setSelectedKey(it); currentKey.set(it); currentValue.set(itemMap[it]) }
            }
        }

        weakObs.get()?.let { obserablesKeySet.add(WeakPair(it, callback)); currentKey.set(it.get()); currentValue.set(itemMap[it.get()]) }
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
                    obs.get()?.let { spinner.get()?.setSelectedValue(it); currentValue.set(it); currentValue.set(itemMap.getKey(it)) }
            }
        }

        weakObs.get()?.let { obserablesValueSet.add(WeakPair(it, callback)); currentValue.set(it.get()); currentValue.set(itemMap.getKey(it.get())) }
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
