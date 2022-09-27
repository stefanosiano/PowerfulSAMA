package com.stefanosiano.powerful_libraries.sama.databinding

import android.text.Editable
import android.text.TextWatcher
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.stefanosiano.powerful_libraries.sama.ui.BigDecimalEditText
import com.stefanosiano.powerful_libraries.sama.ui.BigDecimalTextInputEditText
import com.stefanosiano.powerful_libraries.sama.ui.BigDecimalTextView
import java.math.BigDecimal

@Suppress("UnusedPrivateClass")
private class SamaBigDecimalViewDataBindingAdapters

/** Listener called when a new text is set. */
@BindingAdapter("textAttrChanged")
fun setBdetTextListener(view: BigDecimalEditText, attrChange: InverseBindingListener) {
    view.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            attrChange.onChange()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

/** Set [newValue] as text. */
@BindingAdapter("text")
fun setBdetText(view: BigDecimalEditText, newValue: BigDecimal) {
    view.setTextBd(newValue)
}

/** Get the current text as [BigDecimal]. */
@InverseBindingAdapter(attribute = "text")
fun getBdetText(view: BigDecimalEditText): BigDecimal = view.getTextBd()

/** Listener called when a new text is set. */
@BindingAdapter("textAttrChanged")
fun setBdtTextListener(view: BigDecimalTextView, attrChange: InverseBindingListener) {
    view.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            attrChange.onChange()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

/** Set [newValue] as text. */
@BindingAdapter("text")
fun setBdtText(view: BigDecimalTextView, newValue: BigDecimal) {
    view.setTextBd(newValue)
}

/** Get the current text as [BigDecimal]. */
@InverseBindingAdapter(attribute = "text")
fun getBdtText(view: BigDecimalTextView): BigDecimal = view.getTextBd()

/** Listener called when a new text is set. */
@BindingAdapter("textAttrChanged")
fun setListeners(view: BigDecimalTextInputEditText, attrChange: InverseBindingListener) {
    view.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            attrChange.onChange()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

/** Set [newValue] as text. */
@BindingAdapter("text")
fun setTietText(view: BigDecimalTextInputEditText, newValue: BigDecimal) {
    view.setTextBd(newValue)
}

/** Get the current text as [BigDecimal]. */
@InverseBindingAdapter(attribute = "text")
fun getTietText(view: BigDecimalTextInputEditText): BigDecimal = view.getTextBd()
