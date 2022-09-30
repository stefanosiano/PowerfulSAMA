package com.stefanosiano.powerful_libraries.sama.databinding

import android.graphics.Paint
import android.text.TextUtils
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.stefanosiano.powerful_libraries.sama.extensions.tryOr

@Suppress("UnusedPrivateClass")
private class SamaDataBindingAdapters


/** Sets the layoutManager of the [recyclerView] based on the [columns]. */
@BindingAdapter("columns")
fun setLayoutManager(recyclerView: RecyclerView, columns: Int) {
    recyclerView.layoutManager = if (columns <= 1) {
        LinearLayoutManager(recyclerView.context)
    } else {
        GridLayoutManager(recyclerView.context, columns)
    }
}

/** Sets the hasFixedSize of the [recyclerView]. */
@BindingAdapter("fixedSize")
fun setHasFixedSize(recyclerView: RecyclerView, hasFixedSize: Boolean) {
    recyclerView.setHasFixedSize(hasFixedSize)
}

/** Sets the stroke effect to the [textView]. */
@BindingAdapter("strikethrough")
fun setStrikethrough(textView: TextView, strikethrough: Boolean) {
    textView.paintFlags = if (strikethrough) {
        textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    } else {
        textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }
}

/** Sets the error id to the TextInputLayout. */
@BindingAdapter("tilError")
fun setTextInputLayoutError(textInputLayoutError: TextInputLayout, error: Int) {
    val stringError = tryOr("") { textInputLayoutError.context.getString(error) }
    setTextInputLayoutError(textInputLayoutError, stringError)
}

/** Sets the error string to the TextInputLayout. */
@BindingAdapter("tilError")
fun setTextInputLayoutError(textInputLayoutError: TextInputLayout, error: String) {
    textInputLayoutError.error = error
    textInputLayoutError.isErrorEnabled = !TextUtils.isEmpty(error)
}
