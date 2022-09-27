package com.stefanosiano.powerful_libraries.sama.databinding

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter

@Suppress("UnusedPrivateClass")
private class ImageViewDataBindingAdapters

/** Puts the [bitmap] into the [imageView]. */
@BindingAdapter("src")
fun setImageViewSource(imageView: ImageView, bitmap: Bitmap?) {
    imageView.setImageBitmap(bitmap)
}

/** Puts the [drawable] into the [imageView]. */
@BindingAdapter("src")
fun setImageViewSource(imageView: ImageView, drawable: Drawable?) {
    imageView.setImageDrawable(drawable)
}

/** Puts the resource with id [id] into the [imageView]. */
@BindingAdapter("src")
fun setImageViewSource(imageView: ImageView, id: Int?) {
    imageView.setImageResource(id ?: 0)
}

/** Sets the tint color of the ImageView. */
@BindingAdapter("tint")
fun setImageViewTint(imageView: ImageView, color: Int) {
    ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color))
}
