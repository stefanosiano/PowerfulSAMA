package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.ObservableField
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.ui.SamaSpinner.SamaSpinnerItem
import java.lang.ref.WeakReference


/**
 * Custom Spinner that uses data binding to always have the most updated values.
 * It supports a collection of strings, or a collection of [SamaSpinnerItem] (pairs key, value).
 */
open class SamaSpinner : AppCompatSpinner {

    private var arrayAdapter: ArrayAdapter<String>? = null
    private val itemMap = HashMap<String, String>()

    /** Set of observable strings to update when an item is selected. It will contain the key */
    private val obserablesKeySet: MutableList<WeakReference<ObservableField<String>>> = ArrayList()

    /** Set of observable strings to update when an item is selected. It will contain the value */
    private val obserablesValueSet: MutableList<WeakReference<ObservableField<String>>> = ArrayList()

    /** Key to use after setting items (if Key was selected before items were available) */
    private var toSelectKey: String? = null

    /** Key to use after setting items (if Key was selected before items were available) */
    private val listeners = ArrayList<(key: String, value: String) -> Unit>()

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}


    /** Common initialization of the spinner */
    init {
        onItemSelectedListener = object: OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val key = getSpnKey()
                val value = getSpnValue()
                if(key == toSelectKey) toSelectKey = null
                logDebug("Selected item: $key -> $value")
                obserablesKeySet.forEach { it.get()?.set(key) }
                obserablesValueSet.forEach { it.get()?.set(value) }
                //if an item was selected, i should have key and value. this is only a lifesaver
                key ?: return
                value ?: return
                listeners.forEach { it(key, value) }
            }
        }

        arrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item)
        super.setAdapter(arrayAdapter)
    }


    /** Initializes the spinner, using [spinnerLayoutId] for the spinner items */
    fun init(spinnerLayoutId: Int) {
        val temp = ArrayList<String>()
        val old = getSpnKey()
        (0 until (arrayAdapter?.count ?: 0)).forEach { i -> arrayAdapter?.getItem(i)?.let { temp.add(it) } }
        runOnUi {
            arrayAdapter?.clear()
            arrayAdapter = ArrayAdapter(context, spinnerLayoutId)
            arrayAdapter?.addAll(temp)
            super.setAdapter(arrayAdapter)
            arrayAdapter?.notifyDataSetChanged()
            setSpnKey(toSelectKey ?: old)
        }
    }

    fun addListener(l: (key: String, value: String) -> Unit) { listeners.add(l) }


    /** Sets [items] as the array of [String] to show in the spinner */
    fun setItems(items: Array<out String>?) = setItems(items?.toList())

    /** Sets [items] as the collection of [String] to show in the spinner */
    fun setItems(items: Iterable<String>?) = setItems(items?.map { SimpleSpinnerItem(it, it) })

    /** Sets [items] as the array of [SamaSpinnerItem] to show in the spinner */
    fun setItems(items: Array<out SamaSpinnerItem>?) = setItems(items?.toList())

    /** Sets [items] as the collection of [SamaSpinnerItem] to show in the spinner */
    fun setItems(items: Collection<SamaSpinnerItem>?) {
        if(items == null) return
        itemMap.clear()
        items.forEach { itemMap.put(it.key(), it.value()) }
        val old = getSpnKey()
        arrayAdapter?.clear()
        arrayAdapter?.addAll( items.map { it.value() } )
        arrayAdapter?.notifyDataSetChanged()

        logVerbose(if(items.isNotEmpty()) "Setting spinner items: " else "No items set for this spinner")
        items.forEach { logVerbose(it.toString()) }

        setSpnKey(toSelectKey ?: old)
    }


    /** Sets the selection of the spinner to the first occurrence of [value]. If it was initialized with a collection of strings, it calls [setSelection] */
    @Deprecated(message = "use [setSpnValue] instead", replaceWith = ReplaceWith("setSpnValue(value)"))
    fun setSelectedValue(value: String) = setSpnValue(value)

    /** Sets the selection of the spinner to the first occurrence of [key]. If it was initialized with a collection of strings, it calls [setSelection] */
    @Deprecated(message = "use [setSpnKey] instead", replaceWith = ReplaceWith("setSpnKey(key)"))
    fun setSelectedKey(key: String) = setSpnKey(key)


    /** Sets the selection of the spinner to the first occurrence of [value]. If it was initialized with a collection of strings, it calls [setSelection] */
    fun setSpnValue(value: String?) {
        value ?: return
        if(getSpnValue() == value) return
        val selectedIndex = (0 until adapter.count).firstOrNull { adapter.getItem(it) == value }
        if(selectedIndex != null) {
            toSelectKey = null
            setSelection(selectedIndex)
        }
        else {
            toSelectKey = itemMap.getKey(value)
        }
    }

    /** Sets the selection of the spinner to the first occurrence of [key]. If it was initialized with a collection of strings, it calls [setSelection] */
    fun setSpnKey(key: String?) {
        key ?: return
        if(getSpnKey() == key) return
        val selectedIndex = (0 until adapter.count).firstOrNull { adapter.getItem(it) == itemMap[key] }
        if(selectedIndex != null) {
            toSelectKey = null
            setSelection(selectedIndex)
        }
        else {
            toSelectKey = key
        }
    }


    /** Gets the value associated to the currently shown item. If it was initialized with a collection of strings, it calls [getSelection] */
    @Deprecated(message = "use [getSpnValue] instead", replaceWith = ReplaceWith("getSpnValue()"))
    fun getSelectedValue(): String? = getSpnValue()

    /** Gets the key associated to the currently shown item. If it was initialized with a collection of strings, it calls [getSelection] */
    @Deprecated(message = "use [getSpnKey] instead", replaceWith = ReplaceWith("getSpnKey()"))
    fun getSelectedKey(): String? = getSpnKey()


    /** Gets the value associated to the currently shown item */
    fun getSpnValue(): String? = selectedItem as? String

    /** Gets the key associated to the currently shown item */
    fun getSpnKey(): String? = (selectedItem as? String).let { itemMap.getKey(it) }






    /**
     * Adds the observable strings to the internal list.
     * When an item is selected, all registered observables are updated. When one observable changes, the item is selected.
     * If it was initialized with a collection of strings, it will contain the current shown value, otherwise the key associated to it
     */
    @Deprecated(message = "use data binding instead")
    fun bindKey(obs: ObservableField<String>?) {
        if (obs == null) return
        obserablesKeySet.add(WeakReference(obs))
        obs.addOnChangedAndNow {
            it ?: return@addOnChangedAndNow
            setSpnKey(it)
        }
    }

    /**
     * Adds the observable strings to the internal list.
     * When an item is selected, all registered observables are updated. When one observable changes, the item is selected.
     * If it was initialized with a collection of strings, it will contain the current shown value, otherwise the value associated to it
     */
    @Deprecated(message = "use data binding instead")
    fun bindValue(obs: ObservableField<String>?) {
        if (obs == null) return
        obserablesValueSet.add(WeakReference(obs))
        obs.addOnChangedAndNow {
            it ?: return@addOnChangedAndNow
            setSpnValue(it)
        }
    }







    @BindingAdapter("spnValueAttrChanged")
    fun setSpnValueListener(spinner: SamaSpinner, listener: InverseBindingListener) {
        spinner.addListener { _, _ -> listener.onChange() }
    }
    @BindingAdapter("spnValue")
    fun setSpnValue(spinner: SamaSpinner, value: String?) { if (value != spinner.getSpnValue()) spinner.setSpnValue(value) }
    @InverseBindingAdapter(attribute = "spnValue")
    fun getSpnValue(spinner: SamaSpinner): String? = spinner.getSpnValue()

    @BindingAdapter("spnKeyAttrChanged")
    fun setSpnKeyListener(spinner: SamaSpinner, listener: InverseBindingListener) {
        spinner.addListener { _, _ -> listener.onChange() }
    }
    @BindingAdapter("spnKey")
    fun setSpnKey(spinner: SamaSpinner, key: String?) { if (key != spinner.getSpnKey()) spinner.setSpnKey(key) }
    @InverseBindingAdapter(attribute = "spnKey")
    fun getSpnKey(spinner: SamaSpinner): String? = spinner.getSpnKey()








    /** Simple class with 2 simple fields that implements [SamaSpinnerItem] */
    data class SimpleSpinnerItem(val key: String?, val value: String?) : SamaSpinnerItem{
        override fun value() = value ?: ""
        override fun key() = key ?: ""
    }





    /** Interface used to populate a [SamaSpinner] */
    interface SamaSpinnerItem {

        /** Returns the value of the item */
        fun value(): String

        /** Returns the key of the item */
        fun key(): String
    }
}
