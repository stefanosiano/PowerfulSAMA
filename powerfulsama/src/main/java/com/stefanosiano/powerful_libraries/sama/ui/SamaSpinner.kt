package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama
import com.stefanosiano.powerful_libraries.sama.utils.WeakPair
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob


/**
 * Custom Spinner that uses data binding to always have the most updated values.
 * It supports a collection of strings, or a collection of [SamaSpinnerItem] (pairs key, value).
 */
class SamaSpinner : AppCompatSpinner, CoroutineScope {

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    private var arrayAdapter: ArrayAdapter<String>? = null
    private val itemMap = HashMap<String, String>()
    private var showValue = false

    /** observableString that contains always the value of the current shown item. If it was initialized with a collection of strings, it equals [currentKey] */
    private val currentItem = ObservableField<SimpleSpinnerItem>()

    /** Set of observable strings to update when an item is selected. It will contain the key */
    private val obserablesKeySet: MutableSet<WeakPair<ObservableField<String>, Observable.OnPropertyChangedCallback>> = HashSet()

    /** Set of observable strings to update when an item is selected. It will contain the value */
    private val obserablesValueSet: MutableSet<WeakPair<ObservableField<String>, Observable.OnPropertyChangedCallback>> = HashSet()

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}


    /** Common initialization of the spinner */
    init {
        onItemSelectedListener = object: OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val key = tryOrNull { getSelectedKey() } ?: return
                currentItem.set(SimpleSpinnerItem(key, itemMap[key]))
            }
        }

        val key = getSelectedKey()
        currentItem.set(SimpleSpinnerItem(key, itemMap[key]))
        currentItem.addOnChangedAndNow { item -> if(item == null) return@addOnChangedAndNow; item.key?.let { k -> obserablesKeySet.forEach { it.first()?.set(k) } } ; item.value?.let { v -> obserablesValueSet.forEach { it.first()?.set(v) } } }


        arrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item)
        super.setAdapter(arrayAdapter)
    }


    /** Initializes the spinner, using [spinnerLayoutId] for the spinner items */
    fun init(spinnerLayoutId: Int) {
        val temp = ArrayList<String>()
        val old = selectedItem
        (0 until (arrayAdapter?.count ?: 0)).forEach { i -> arrayAdapter?.getItem(i)?.let { temp.add(it) } }
        runOnUi {
            arrayAdapter?.clear()
            arrayAdapter = ArrayAdapter(context, spinnerLayoutId)
            arrayAdapter?.addAll(temp)
            super.setAdapter(arrayAdapter)
            arrayAdapter?.notifyDataSetChanged()
            (0 until adapter.count).firstOrNull { adapter.getItem(it) == old }?.let { setSelection(it) }
        }
    }


    /** Sets [items] as the array of [SamaSpinnerItem] to show in the spinner, whenever it changes */
    fun bindItemsArray(items: ObservableField<Array<out SamaSpinnerItem>>?, showValue: Boolean = true) = items?.addOnChangedAndNow { if (it != null) setItems(it.toList(), showValue) }

    /** Sets [items] as the collection of [SamaSpinnerItem] to show in the spinner, whenever it changes */
    fun bindItems(items: ObservableField<Collection<SamaSpinnerItem>>?, showValue: Boolean = true) = items?.addOnChangedAndNow { if (it != null) setItems(it.toList(), showValue) }

    /** Sets [items] as the array of [String] to show in the spinner, whenever it changes */
    fun bindItemsArray(items: ObservableField<Array<out String>>?) = items?.addOnChangedAndNow { if (it != null) setItems(it.toList()) }

    /** Sets [items] as the collection of [String] to show in the spinner, whenever it changes */
    fun bindItems(items: ObservableField<Collection<String>>?) = items?.addOnChangedAndNow { if (it != null) setItems(it.toList()) }


    /** Sets [items] as the array of [String] to show in the spinner */
    fun setItems(items: Array<out String>?) = setItems(items?.map { SimpleSpinnerItem(it, it) }, false)

    /** Sets [items] as the collection of [String] to show in the spinner */
    fun setItems(items: Collection<String>?) = setItems(items?.map { SimpleSpinnerItem(it, it) }, false)

    /** Sets [items] as the array of [SamaSpinnerItem] to show in the spinner */
    fun setItems(items: Array<out SamaSpinnerItem>?, showValue: Boolean = true) = setItems(items?.toList(), showValue)

    /** Sets [items] as the collection of [SamaSpinnerItem] to show in the spinner */
    fun setItems(items: Collection<SamaSpinnerItem>?, showValue: Boolean = true) {
        if(items == null) return
        itemMap.clear()
        items.map { itemMap.put(it.key(), it.value()) }
        val old = selectedItem as? String? ?: currentItem.get()?.let { (if(showValue) it.value ?: itemMap[it.key] else it.key ?: itemMap.getKey(it.value)) } ?: ""
        arrayAdapter?.clear()
        arrayAdapter?.addAll( items.map { if(showValue) it.value() else it.key() } )
        arrayAdapter?.notifyDataSetChanged()
        this.showValue = showValue
        if(showValue) setSelectedValue(old) else setSelectedKey(old)
    }


    /** Sets the selection of the spinner to the first occurrence of [value]. If it was initialized with a collection of strings, it calls [setSelection] */
    fun setSelectedValue(value: String) = (0 until adapter.count).firstOrNull { adapter.getItem(it) == if(showValue) value else itemMap.getKey(value) }?.let { setSelection(it) }

    /** Sets the selection of the spinner to the first occurrence of [key]. If it was initialized with a collection of strings, it calls [setSelection] */
    fun setSelectedKey(key: String) = (0 until adapter.count).firstOrNull { adapter.getItem(it) == if(showValue) itemMap[key] else key }?.let { setSelection(it) }


    /** Gets the value associated to the currently shown item. If it was initialized with a collection of strings, it calls [getSelection] */
    fun getSelectedValue(): String? = if(showValue) selectedItem as? String else itemMap[getSelectedKey()]

    /** Gets the key associated to the currently shown item. If it was initialized with a collection of strings, it calls [getSelection] */
    fun getSelectedKey(): String? = if(showValue) getSelectedValue().let { itemMap.getKey(it) } else selectedItem as? String


    /**
     * Adds the observable strings to the internal list.
     * When an item is selected, all registered observables are updated. When one observable changes, the item is selected.
     * If it was initialized with a collection of strings, it will contain the current shown value, otherwise the key associated to it
     */
    fun bindKey(obs: ObservableField<String>?) {
        if (obs == null) return
        val spinner = this.toWeakReference()
        val weakObs = obs.toWeakReference()
        val callback = obs.addOnChangedAndNow {
            weakObs.get()?.also { obs ->
                if (obserablesKeySet.firstOrNull { set -> set.first() == obs } != null && obs.get() != currentItem.get()?.key)
                    obs.get()?.let { spinner.get()?.setSelectedKey(it); currentItem.set(SimpleSpinnerItem(it, itemMap[it])) }
            }
        }

        weakObs.get()?.let { obserablesKeySet.add(WeakPair(it, callback)); currentItem.set(SimpleSpinnerItem(it.get(), itemMap[it.get()])) }
    }

    /**
     * Adds the observable strings to the internal list.
     * When an item is selected, all registered observables are updated. When one observable changes, the item is selected.
     * If it was initialized with a collection of strings, it will contain the current shown value, otherwise the value associated to it
     */
    fun bindValue(obs: ObservableField<String>?) {
        if (obs == null) return
        val spinner = this.toWeakReference()
        val weakObs = obs.toWeakReference()
        val callback = obs.addOnChangedAndNow {
            weakObs.get()?.also { obs ->
                if (obserablesValueSet.firstOrNull { set -> set.first() == obs } != null && obs.get() != currentItem.get()?.value)
                    obs.get()?.let { spinner.get()?.setSelectedValue(it); currentItem.set(SimpleSpinnerItem(itemMap.getKey(it), it)) }
            }
        }

        weakObs.get()?.let { obserablesKeySet.add(WeakPair(it, callback)); currentItem.set(SimpleSpinnerItem(itemMap.getKey(it.get()), it.get())) }
    }




    /** Simple class with 2 simple fields that implements [SamaSpinnerItem] */
    private class SimpleSpinnerItem(val key: String?, val value: String?) : SamaSpinnerItem{
        override fun value() = value ?:""
        override fun key() = key ?:""
    }





    /** Interface used to populate a [SamaSpinner] */
    interface SamaSpinnerItem {

        /** Returns the value of the item */
        fun value(): String

        /** Returns the key of the item */
        fun key(): String
    }
}
