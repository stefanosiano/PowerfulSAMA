package com.stefanosiano.powerful_libraries.sama.databinding

import android.view.View
import android.widget.Toast
import androidx.databinding.BindingAdapter

@Suppress("UnusedPrivateClass")
private class ViewDataBindingAdapters

/** Sets the visibility of the view: if [visible] then Visible, else Gone. */
@BindingAdapter("visible")
fun setVisibility(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

/** Sets the visibility of the view: if [invisible] then Invisible, else Visible. */
@BindingAdapter("invisible")
fun setInvisibility(view: View, invisible: Boolean) {
    view.visibility = if (invisible) View.INVISIBLE else View.VISIBLE
}

/** Sets the hidden status of the view: if [hidden] then Gone, else Visible. */
@BindingAdapter("hidden")
fun setHidden(view: View, hidden: Boolean) {
    view.visibility = if (hidden) View.GONE else View.VISIBLE
}

/**
 * Sets the visibility status of the view:
 * if current screen orientation equals [orientation] then view is Visible, else it's Gone.
 */
@BindingAdapter("visibleOnOrientation")
fun setVisibleOnOrientation(view: View, orientation: Int) {
    view.visibility =
        if (view.context.resources.configuration.orientation == orientation) View.VISIBLE else View.GONE
}

/** Disable the view if [disable] is true. */
@BindingAdapter("disabled")
fun setDisabled(view: View, disable: Boolean) {
    view.isEnabled = !disable
}

/** Enable the view if [enable] is true. */
@BindingAdapter("enabled")
fun setEnabled(view: View, enable: Boolean) {
    view.isEnabled = enable
}

/** Sets a tooltip on longClick on the view with a toast. */
@BindingAdapter("tooltip")
fun showTooltip(view: View, tooltip: String) =
    view.setOnLongClickListener { Toast.makeText(view.context, tooltip, Toast.LENGTH_SHORT).show(); true }

/** Sets a tooltip on longClick on the view with a toast. */
@BindingAdapter("tooltip")
fun showTooltip(view: View, tooltip: Int) =
    view.setOnLongClickListener { Toast.makeText(view.context, tooltip, Toast.LENGTH_SHORT).show(); true }

/** Set the [onLongClick] as [View.OnLongClickListener] to this [view], without needing passing the view parameter. */
@BindingAdapter("onLongClick")
fun onLongClick(view: View, onLongClick: (() -> Unit)?) {
    onLongClick?.let { view.setOnLongClickListener { it(); true } }
}

/** Set the [onClick] as [View.OnClickListener] to this [view], without the need of passing the view parameter. */
@BindingAdapter("onClick")
fun onClick(view: View, onClick: (() -> Unit)?) {
    onClick?.let { view.setOnClickListener { it() } }
}
