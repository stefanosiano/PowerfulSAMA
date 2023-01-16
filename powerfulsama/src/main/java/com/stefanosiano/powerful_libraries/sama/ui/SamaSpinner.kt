package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import com.stefanosiano.powerful_libraries.sama.getKey
import com.stefanosiano.powerful_libraries.sama.logDebug
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.ui.SamaSpinner.SamaSpinnerItem

/**
 * Custom Spinner that uses data binding to always have the most updated values.
 * It supports a collection of strings, or a collection of [SamaSpinnerItem] (pairs key, value).
 */
open class SamaSpinner : AppCompatSpinner {

    private var arrayAdapter: ArrayAdapter<String>? = null
    private val itemMap = HashMap<String, String>()

    /** Key to use after setting items (if Key was selected before items were available). */
    private var toSelectKey: String? = null

    /** Key to use after setting items (if Key was selected before items were available). */
    private val listeners = ArrayList<(key: String, value: String) -> Unit>()

    /** Common initialization of the spinner. */
    init {
        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // if an item was selected, i should have key and value. this is only a lifesaver
                val key = getSpnKey() ?: return
                val value = getSpnValue() ?: return
                if (key == toSelectKey) toSelectKey = null
                logDebug("Selected item: $key -> $value")
                listeners.forEach { it(key, value) }
            }
        }

        arrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item)
        super.setAdapter(arrayAdapter)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
        super(context, attrs, defStyleAttr)

    /** Initializes the spinner, using [spinnerLayoutId] for the spinner items. */
    fun init(spinnerLayoutId: Int) {
        val temp = ArrayList<String>()
        val old = getSpnKey()
        (0 until (arrayAdapter?.count ?: 0)).forEach { i ->
            arrayAdapter?.getItem(i)?.let { temp.add(it) }
        }
        post {
            arrayAdapter?.clear()
            arrayAdapter = ArrayAdapter(context, spinnerLayoutId)
            arrayAdapter?.addAll(temp)
            super.setAdapter(arrayAdapter)
            arrayAdapter?.notifyDataSetChanged()
            setSpnKey(toSelectKey ?: old)
        }
    }

    /** Add [l] to be called when an item is selected. */
    fun addListener(l: (key: String, value: String) -> Unit) { listeners.add(l) }

    /** Sets [items] as the array of [SamaSpinnerItem] to show in the spinner. */
    fun setItems(items: Array<out SamaSpinnerItem>?) = setItems(items?.toList())

    /** Sets [items] as the collection of [SamaSpinnerItem] to show in the spinner. */
    fun setItems(items: Collection<SamaSpinnerItem>?) {
        if (items == null) return
        itemMap.clear()
        items.forEach { itemMap[it.key()] = it.value() }
        val old = getSpnKey()
        arrayAdapter?.clear()
        arrayAdapter?.addAll(items.map { it.value() })
        arrayAdapter?.notifyDataSetChanged()

        logVerbose(
            if (items.isNotEmpty()) "Setting spinner items: " else "No items set for this spinner"
        )
        items.forEach { logVerbose(it.toString()) }

        setSpnKey(toSelectKey ?: old)
    }

    /**
     * Sets the selection of the spinner to the first occurrence of [value].
     * If it was initialized with a collection of strings, it calls [setSelection].
     */
    fun setSpnValue(value: String?) {
        value ?: return
        if (getSpnValue() == value) return
        val selectedIndex = (0 until adapter.count).firstOrNull { adapter.getItem(it) == value }
        if (selectedIndex != null) {
            toSelectKey = null
            setSelection(selectedIndex)
        } else {
            toSelectKey = itemMap.getKey(value)
        }
    }

    /**
     * Sets the selection of the spinner to the first occurrence of [key].
     * If it was initialized with a collection of strings, it calls [setSelection].
     */
    fun setSpnKey(key: String?) {
        key ?: return
        if (getSpnKey() == key) return
        val selectedIndex = (0 until adapter.count)
            .firstOrNull { adapter.getItem(it) == itemMap[key] }
        if (selectedIndex != null) {
            toSelectKey = null
            setSelection(selectedIndex)
        } else {
            toSelectKey = key
        }
    }

    /** Gets the value associated to the currently shown item. */
    fun getSpnValue(): String? = selectedItem as? String

    /** Gets the key associated to the currently shown item. */
    fun getSpnKey(): String? = (selectedItem as? String).let { itemMap.getKey(it) }

    /** Simple class representing a pair key/value. */
    open class SamaSpinnerItem(
        /** Key of this [SamaSpinnerItem]. */
        open val key: String?,
        /** Value of this [SamaSpinnerItem]. */
        open val value: String?
    ) {
        /** Get the value of this [SamaSpinnerItem], or an empty string if it's null. */
        open fun value() = value ?: ""

        /** Get the key of this [SamaSpinnerItem], or an empty string if it's null. */
        open fun key() = key ?: ""
    }
}
