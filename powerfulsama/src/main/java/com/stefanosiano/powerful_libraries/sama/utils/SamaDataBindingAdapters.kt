package com.stefanosiano.powerful_libraries.sama.utils

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.stefanosiano.powerful_libraries.sama.ui.BigDecimalEditText
import com.stefanosiano.powerful_libraries.sama.ui.BigDecimalTextInputEditText
import com.stefanosiano.powerful_libraries.sama.ui.SamaSearchView
import com.stefanosiano.powerful_libraries.sama.ui.SamaSpinner
import java.math.BigDecimal


class SamaDataBindingAdapters

/** Puts the [bitmap] into the [imageView]. */
@BindingAdapter("src")
fun setImageViewSource(imageView: ImageView, bitmap: Bitmap?) { imageView.post { imageView.setImageBitmap(bitmap) } }

/** Puts the [drawable] into the [imageView]. */
@BindingAdapter("src")
fun setImageViewSource(imageView: ImageView, drawable: Drawable?) { imageView.post { imageView.setImageDrawable(drawable) } }

/** Puts the resource with id [id] into the [imageView]. */
@BindingAdapter("src")
fun setImageViewSource(imageView: ImageView, id: Int?) { imageView.post { imageView.setImageResource(id ?: 0) } }

/** Sets the visibility of the view: if [visible] then Visible, else Gone. */
@BindingAdapter("visible")
fun setVisibility(view: View, visible: Boolean) { view.visibility = if(visible) View.VISIBLE else View.GONE }

/** Sets the visibility of the view: if [invisible] then Invisible, else Visible. */
@BindingAdapter("invisible")
fun setInvisibility(view: View, invisible: Boolean) { view.visibility = if(invisible) View.INVISIBLE else View.VISIBLE }

/** Sets the hidden status of the view: if [hidden] then Gone, else Visible. */
@BindingAdapter("hidden")
fun setHidden(view: View, hidden: Boolean) { view.visibility = if(hidden) View.GONE else View.VISIBLE }

/** Sets the visibility status of the view: if current screen orientation equals [orientation] then view is Visible, else it's Gone. */
@BindingAdapter("visibleOnOrientation")
fun setVisibleOnOrientation(view: View, orientation: Int) { view.visibility = if(view.context.resources.configuration.orientation == orientation) View.VISIBLE else View.GONE }

/** Disable the view if [disable] is true. */
@BindingAdapter("disabled")
fun setDisabled(view: View, disable: Boolean) { view.isEnabled = !disable }

/** Enable the view if [enable] is true. */
@BindingAdapter("enabled")
fun setEnabled(view: View, enable: Boolean) { view.isEnabled = enable }

/** Sets a tooltip on longClick on the view with a toast. */
@BindingAdapter("tooltip")
fun showTooltip(view: View, tooltip: String) = view.setOnLongClickListener { Toast.makeText(view.context, tooltip, Toast.LENGTH_SHORT).show(); true }

/** Sets a tooltip on longClick on the view with a toast. */
@BindingAdapter("tooltip")
fun showTooltip(view: View, tooltip: Int) = view.setOnLongClickListener { Toast.makeText(view.context, tooltip, Toast.LENGTH_SHORT).show(); true }


/** Sets the tint color of the ImageView. */
@BindingAdapter("tint")
fun setImageViewTint(imageView: ImageView, color: Int) { ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color)) }

/** Sets the layoutManager of the [recyclerView] based on the [columns]. */
@BindingAdapter("columns")
fun setLayoutManager(recyclerView: RecyclerView, columns: Int) {
    recyclerView.layoutManager = if(columns <= 1) LinearLayoutManager(recyclerView.context)
    else GridLayoutManager(recyclerView.context, columns)
}

/** Sets the hasFixedSize of the [recyclerView]. */
@BindingAdapter("fixedSize")
fun bindRecyclerViewItems(recyclerView: RecyclerView, hasFixedSize: Boolean) { recyclerView.setHasFixedSize(hasFixedSize) }

/** Sets the stroke effect to the [textView]. */
@BindingAdapter("strikethrough")
fun setShapeBackgroundColor(textView: TextView, strikethrough: Boolean) {
    textView.paintFlags = if(strikethrough) textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
}

/** Sets the error id to the TextInputLayout . */
@BindingAdapter("tilError")
fun setTextInputLayoutError(textInputLayoutError: TextInputLayout, error: Int) {
    val stringError = try {textInputLayoutError.context.getString(error)} catch (e: Exception) {""}
    setTextInputLayoutError(textInputLayoutError, stringError)
}

/** Sets the error string to the TextInputLayout . */
@BindingAdapter("tilError")
fun setTextInputLayoutError(textInputLayoutError: TextInputLayout, error: String) {
    textInputLayoutError.error = error
    textInputLayoutError.isErrorEnabled = !TextUtils.isEmpty(error)
}



@BindingAdapter("textAttrChanged")
fun setListeners(view: BigDecimalEditText, attrChange: InverseBindingListener) {
    view.addTextChangedListener( object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { attrChange.onChange() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

@BindingAdapter("text")
fun setBdetText(view: BigDecimalEditText, newValue: BigDecimal) { view.setTextBd(newValue) }

@InverseBindingAdapter(attribute = "text")
fun getBdetText(view: BigDecimalEditText) : BigDecimal { return view.getTextBd() }


@BindingAdapter("textAttrChanged")
fun setListeners(view: BigDecimalTextInputEditText, attrChange: InverseBindingListener) {
    view.addTextChangedListener( object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { attrChange.onChange() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

@BindingAdapter("text")
fun setTietText(view: BigDecimalTextInputEditText, newValue: BigDecimal) { view.setTextBd(newValue) }

@InverseBindingAdapter(attribute = "text")
fun getTietText(view: BigDecimalTextInputEditText) : BigDecimal { return view.getTextBd() }





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

@BindingAdapter("ssvQueryAttrChanged")
fun setSsvQueryListener(searchView: SamaSearchView, listener: InverseBindingListener) {
    searchView.addOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean { listener.onChange(); return true }
        override fun onQueryTextChange(newText: String?): Boolean { listener.onChange(); return true }
    })
}
@BindingAdapter("ssvQuery")
fun setSsvQuery(searchView: SamaSearchView, query: String?) { if (query != searchView.getSsvQuery()) searchView.setSsvQuery(query) }
@InverseBindingAdapter(attribute = "ssvQuery")
fun getSsvQuery(searchView: SamaSearchView): String = searchView.getSsvQuery()

@BindingAdapter("onLongClick")
fun onLongClick(view: View, onLongClick: (() -> Unit)?) { onLongClick?.let { view.setOnLongClickListener { it(); true } } }

@BindingAdapter("onClick")
fun onClick(view: View, onClick: (() -> Unit)?) { onClick?.let { view.setOnClickListener { it() } } }
