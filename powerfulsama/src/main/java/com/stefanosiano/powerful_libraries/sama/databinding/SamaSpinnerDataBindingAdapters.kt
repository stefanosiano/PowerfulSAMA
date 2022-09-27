package com.stefanosiano.powerful_libraries.sama.databinding

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.stefanosiano.powerful_libraries.sama.ui.SamaSpinner

@Suppress("UnusedPrivateClass")
private class SamaSpinnerDataBindingAdapters

/** Listener called when a value is selected. */
@BindingAdapter("spnValueAttrChanged")
fun setSpnValueListener(spinner: SamaSpinner, listener: InverseBindingListener) {
    spinner.addListener { _, _ -> listener.onChange() }
}

/** Sets the selection of the spinner to the first occurrence of [value]. */
@BindingAdapter("spnValue")
fun setSpnValue(spinner: SamaSpinner, value: String?) {
    if (value != spinner.getSpnValue()) spinner.setSpnValue(value)
}

/** Gets the value associated to the currently shown item. */
@InverseBindingAdapter(attribute = "spnValue")
fun getSpnValue(spinner: SamaSpinner): String? = spinner.getSpnValue()

/** Listener called when a key is selected. */
@BindingAdapter("spnKeyAttrChanged")
fun setSpnKeyListener(spinner: SamaSpinner, listener: InverseBindingListener) {
    spinner.addListener { _, _ -> listener.onChange() }
}

/** Sets the selection of the spinner to the first occurrence of [key]. */
@BindingAdapter("spnKey")
fun setSpnKey(spinner: SamaSpinner, key: String?) {
    if (key != spinner.getSpnKey()) spinner.setSpnKey(key)
}

/** Gets the key associated to the currently shown item. */
@InverseBindingAdapter(attribute = "spnKey")
fun getSpnKey(spinner: SamaSpinner): String? = spinner.getSpnKey()
