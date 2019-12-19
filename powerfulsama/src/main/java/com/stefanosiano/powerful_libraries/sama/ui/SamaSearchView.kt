package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.databinding.ObservableList
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.utils.WeakPair

/** Class that provides easy to use SearchView with data binding */
open class SamaSearchView : SearchView {

    /** Adapter used to show suggestions while searching */
    private lateinit var mSuggestionsAdapter: ArrayAdapter<String>

    /** Delay in milliseconds to execute the listener or update the observable */
    var millis = 0L

    /** Handler used to handle the delay */
    private val requeryHandler = Handler()

    /** Current query */
    private var currentQuery = ""

    /** Set of observable strings to update when the query changes */
    private val obserablesSet: MutableSet<WeakPair<ObservableField<String>, Observable.OnPropertyChangedCallback>> = HashSet()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.searchViewStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {

        setOnQueryTextListener ( object : OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query ?: ""
                onQueryUpdated()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                if(newText == null || (currentQuery == newText))
                    return true

                currentQuery = newText

                if(millis > 0) {
                    requeryHandler.removeCallbacksAndMessages(null)
                    requeryHandler.postDelayed( { onQueryUpdated() }, millis)
                }
                else
                    onQueryUpdated()

                return true
            }
        })
    }

    /** Update the observable strings and call the optional function */
    private fun onQueryUpdated() = obserablesSet.forEach { it.first()?.set(currentQuery) }

    /**
     * Adds the observable strings to the internal list.
     * When the query changes, all registered observables are updated.
     * When one observable changes, the query is updated
     */
    fun bindQuery(queryObs : ObservableField<String>?) {

        val searchView = this.toWeakReference()
        val weakObs = queryObs?.toWeakReference() ?: return
        val callback = queryObs.addOnChangedAndNow {
            weakObs.get()?.also { obs ->
                if (obserablesSet.firstOrNull { set -> set.first() == obs } != null && obs.get() != currentQuery)
                    searchView.get()?.setQuery(obs.get(), false)
            }
        }
        if(currentQuery.isEmpty())
            currentQuery = queryObs.get() ?: ""
        queryObs.set(currentQuery)

        obserablesSet.add(WeakPair(weakObs.get() ?: return, callback))
    }

    override fun onViewRemoved(child: View?) {
        super.onViewRemoved(child)
        obserablesSet.forEach { it.first()?.removeOnPropertyChangedCallback(it.second() ?: return@forEach) }
        obserablesSet.clear()
    }

    /** Removes the observable from the registered observables */
    fun unbindQuery(queryObs : ObservableField<String>?) = obserablesSet.filter { queryObs != null && it.first() == queryObs }.forEach { it.first()?.removeOnPropertyChangedCallback(it.second() ?: return@forEach) }



    /** Sets the [suggestions] to show when writing, using [layoutId]. When the user clicks on a suggestion, [f] will be called */
    fun bindSuggestions(layoutId: Int, suggestions: ObservableList<String>, f: (String) -> Unit) {
        mSuggestionsAdapter = ArrayAdapter(context, layoutId)
        bindSuggestions(suggestions, f)
        suggestions.onAnyChange {
            mSuggestionsAdapter.clear()
            mSuggestionsAdapter.addAll(suggestions)
            runOnUi { mSuggestionsAdapter.notifyDataSetChanged() }
        }
    }

    /** Sets the [suggestions] to show when writing, using [layoutId]. When the user clicks on a suggestion, [f] will be called */
    fun bindSuggestions(layoutId: Int, suggestions: List<String>, f: (String) -> Unit) {
        mSuggestionsAdapter = ArrayAdapter(context, layoutId)
        bindSuggestions(suggestions, f)
    }

    /** Sets the [suggestions] to show when writing. When the user clicks on a suggestion, [f] will be called */
    private fun bindSuggestions(suggestions: List<String>, f: (String) -> Unit){
        mSuggestionsAdapter.addAll(suggestions)
        val searchAutoComplete = findViewById<SearchAutoComplete>(R.id.search_src_text)

        searchAutoComplete.setOnItemClickListener { _, _, position, _ ->
            mSuggestionsAdapter.getItem(position)?.let { logVerbose("Clicked on $it"); f(it) }
        }
        runOnUi { searchAutoComplete.setAdapter(mSuggestionsAdapter) }
    }
}