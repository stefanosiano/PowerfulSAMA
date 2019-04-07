package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import androidx.databinding.ObservableField
import com.stefanosiano.powerful_libraries.sama.getKey
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class SamaSpinner : AppCompatSpinner, CoroutineScope {

    override val coroutineContext = SupervisorJob() + CoroutineExceptionHandler { _, t -> t.printStackTrace() }

    private lateinit var arrayAdapter: ArrayAdapter<String>
    private val itemMap = HashMap<String, String>()
    private var showValue = false
    private var valuesOnly = false

    /** observableString that contains always the value of the current shown item. If it was initialized with a list of strings, it equals [currentKey] */
    val currentValue = ObservableField<String>()

    /** observableString that contains always the key of the current shown item. If it was initialized with a list of strings, it equals [currentValue] */
    val currentKey = ObservableField<String>()

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}


    /**
     * Initializes the spinner
     *
     * @param spinnerLayoutId layout id of the spinner items
     * @param items list of [SamaSpinnerItem] to show
     * @param showValue if true it shows the value, otherwise it shows the key
     * @param key key of the first default item to show (ignored if null)
     * @param value value of the first default item to show (ignored if null)
     */
    fun init(spinnerLayoutId: Int, items: List<SamaSpinnerItem>, showValue: Boolean = true, key: String? = null, value: String? = null) {
        arrayAdapter = ArrayAdapter(context, spinnerLayoutId)
        valuesOnly = false
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
     * @param items list of [String] to show
     * @param value value of the first default item to show (ignored if null)
     */
    fun init(spinnerLayoutId: Int, items: List<String>, value: String? = null) {
        showValue = false
        valuesOnly = true
        arrayAdapter = ArrayAdapter(context, spinnerLayoutId)
        itemMap.clear()

        if(value != null) arrayAdapter.add(value)

        arrayAdapter.addAll(items)
        super.setAdapter(arrayAdapter)
        init()
    }

    /** Common initialization of the spinner */
    private fun init() {
        onItemSelectedListener = object: OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                launch {
                    currentKey.set(getSelectedKey())
                    currentValue.set(getSelectedValue())
                }
            }
        }

        launch {
            currentKey.set(getSelectedKey())
            currentValue.set(getSelectedValue())
        }
    }



    /** Sets the selected items to the shown item that matches [value]. It does nothing if it's not found */
    fun setSelection(value: String) { (0 until adapter.count).firstOrNull { adapter.getItem(it) == value }?.let { setSelection(it) } }

    /** Gets the current shown item */
    fun getSelection(): String = selectedItem as String

    /** Sets the selection of the spinner to the first occurrence of [value]. If it was initialized with a list of strings, it calls [setSelection] */
    fun setValueSelected(value: String) {
        if(valuesOnly) return setSelection(value)
        if(showValue)
            (0 until adapter.count).firstOrNull { adapter.getItem(it) == value }?.let { setSelection(it) }
        else
            setKeySelected(itemMap.getKey(value) ?: "")
    }

    /** Sets the selection of the spinner to the first occurrence of [key]. If it was initialized with a list of strings, it calls [setSelection] */
    fun setKeySelected(key: String) {
        if(valuesOnly) return setSelection(key)
        if(showValue)
            setValueSelected(itemMap.get(key) ?: "")
        else
            (0 until adapter.count).firstOrNull { adapter.getItem(it) == key }?.let { setSelection(it) }
    }

    /** Gets the value associated to the currently shown item. If it was initialized with a list of strings, it calls [getSelection] */
    fun getSelectedValue(): String? {
        if(valuesOnly) return getSelection()
        return if(showValue) selectedItem as? String else itemMap.get(getSelectedKey())
    }

    /** Gets the key associated to the currently shown item. If it was initialized with a list of strings, it calls [getSelection] */
    fun getSelectedKey(): String? {
        if(valuesOnly) return getSelection()
        return if(showValue) getSelectedValue().let { itemMap.getKey(it) } else selectedItem as? String
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
