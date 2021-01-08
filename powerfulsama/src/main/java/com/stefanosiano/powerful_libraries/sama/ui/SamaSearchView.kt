package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.databinding.ObservableList
import com.stefanosiano.powerful_libraries.sama.*
import kotlinx.coroutines.*

/** Class that provides easy to use SearchView with data binding */
open class SamaSearchView : SearchView, CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Adapter used to show suggestions while searching */
    private lateinit var mSuggestionsAdapter: ArrayAdapter<String>

    /** Delay in milliseconds to execute the listener or update the observable */
    private var millis = 0L

    /** Job used to handle the delay */
    private var requeryJob: Job? = null

    /** Flag to decide whether to clear focus and close keyboard when submitting a query */
    private var clearFocusOnSubmit = true

    /** Key to use after setting items (if Key was selected before items were available) */
    private val listeners = ArrayList<OnQueryTextListener>()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.searchViewStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val attrSet = context.theme.obtainStyledAttributes(attrs, R.styleable.SamaSearchView, defStyleAttr, 0)
        clearFocusOnSubmit = attrSet.getBoolean(R.styleable.SamaSearchView_ssvClearFocusOnSubmit, clearFocusOnSubmit)
        millis = attrSet.getInt(R.styleable.SamaSearchView_ssvMillis, 0).toLong()
        val query = attrSet.getString(R.styleable.SamaSearchView_ssvQuery) ?: ""
        attrSet.recycle()
        setQuery(query, true)
    }

    init {
        super.setOnQueryTextListener ( object : OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                listeners.forEach { it.onQueryTextSubmit(query) }
                if(clearFocusOnSubmit) clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText ?: return true

                requeryJob?.cancel()
                if(millis > 0) {
                    requeryJob = launch {
                        delay(millis)
                        if(isActive)
                            listeners.forEach { it.onQueryTextChange(newText) }
                    }
                }
                else
                    listeners.forEach { it.onQueryTextChange(newText) }

                return true
            }
        })
    }

    override fun setOnQueryTextListener(listener: OnQueryTextListener?) {
        listener ?: return
        listeners.add(listener)
    }

    fun addOnQueryTextListener(listener: OnQueryTextListener) {
        listeners.add(listener)
    }

    fun setSsvMillis(millis: Int?) { this.millis = (millis?:0).toLong() }
    fun getSsvMillis() = millis.toInt()

    fun setSsvQuery(query: String?) { setQuery(query, true) }
    fun getSsvQuery() = query.toString()


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