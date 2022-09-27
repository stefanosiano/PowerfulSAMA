package com.stefanosiano.powerful_libraries.sama.databinding

import androidx.appcompat.widget.SearchView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.stefanosiano.powerful_libraries.sama.ui.SamaSearchView

@Suppress("UnusedPrivateClass")
private class SamaSearchViewDataBindingAdapters

/** Listener called when a query is set. */
@BindingAdapter("ssvQueryAttrChanged")
fun setSsvQueryListener(searchView: SamaSearchView, listener: InverseBindingListener) {
    searchView.addOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            listener.onChange(); return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            listener.onChange(); return true
        }
    })
}

/** Set the current [query]. */
@BindingAdapter("ssvQuery")
fun setSsvQuery(searchView: SamaSearchView, query: String?) {
    if (query != searchView.getSsvQuery()) searchView.setSsvQuery(query)
}

/** Get the current query. */
@InverseBindingAdapter(attribute = "ssvQuery")
fun getSsvQuery(searchView: SamaSearchView): String = searchView.getSsvQuery()
