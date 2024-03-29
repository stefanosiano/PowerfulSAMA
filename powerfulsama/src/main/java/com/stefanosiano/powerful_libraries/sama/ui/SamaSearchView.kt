package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import com.stefanosiano.powerful_libraries.sama.R
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.logVerbose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Class that provides easy to use SearchView with data binding. */
open class SamaSearchView : SearchView, CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Adapter used to show suggestions while searching. */
    private var mSuggestionsAdapter: ArrayAdapter<String>? = null

    /**
     * Layout of the suggestions dropdown menu.
     * Defaults to [android.R.layout.simple_spinner_dropdown_item].
     */
    private var mSuggestionLayout: Int = android.R.layout.simple_spinner_dropdown_item

    /** Delay in milliseconds to execute the listener or update the observable. */
    private var millis = 0L

    /** Job used to handle the delay. */
    private var requeryJob: Job? = null

    /** Flag to decide whether to clear focus and close keyboard when submitting a query. */
    private var clearFocusOnSubmit = true

    /** Listeners to call when the query changes. */
    private val listeners = ArrayList<OnQueryTextListener>()

    init {
        super.setOnQueryTextListener(object : OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                synchronized(listeners) {
                    listeners.forEach { it.onQueryTextSubmit(query) }
                }
                if (clearFocusOnSubmit) {
                    clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText ?: return true

                requeryJob?.cancel()
                if (millis > 0) {
                    requeryJob = launch {
                        delay(millis)
                        if (isActive) {
                            synchronized(listeners) {
                                listeners.forEach { it.onQueryTextChange(newText) }
                            }
                        }
                    }
                } else {
                    synchronized(listeners) {
                        listeners.forEach { it.onQueryTextChange(newText) }
                    }
                }

                return true
            }
        })
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) :
        this(context, attrs, R.attr.searchViewStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        super(context, attrs, defStyleAttr) {
            val attrSet = context.theme
                .obtainStyledAttributes(attrs, R.styleable.SamaSearchView, defStyleAttr, 0)
            clearFocusOnSubmit = attrSet
                .getBoolean(R.styleable.SamaSearchView_ssvClearFocusOnSubmit, clearFocusOnSubmit)
            millis = attrSet.getInt(R.styleable.SamaSearchView_ssvMillis, 0).toLong()
            mSuggestionLayout = attrSet.getInt(R.styleable.SamaSearchView_ssvSuggestionLayout, -1)
            val query = attrSet.getString(R.styleable.SamaSearchView_ssvQuery) ?: ""
            attrSet.recycle()
            setQuery(query, true)
        }

    /** Call the [addOnQueryTextListener]. */
    override fun setOnQueryTextListener(listener: OnQueryTextListener?) =
        addOnQueryTextListener(listener)

    /** Add a listener to be called when the query changes. */
    fun addOnQueryTextListener(listener: OnQueryTextListener?) {
        listener ?: return
        synchronized(listeners) { listeners.add(listener) }
    }

    /** Set the delay in milliseconds to execute the query listener. */
    fun setSsvMillis(millis: Int?) { this.millis = (millis ?: 0).toLong() }

    /** Get the delay in milliseconds used to execute the query listener. */
    fun getSsvMillis() = millis.toInt()

    /** Set the query. */
    fun setSsvQuery(query: String?) { if (query != getSsvQuery()) setQuery(query, true) }

    /** Get the current query. */
    fun getSsvQuery() = query.toString()

    /** Set the layout to use to show the suggestion list. */
    fun setSsvSuggestionLayout(suggestionLayoutId: Int?) {
        this.mSuggestionLayout = suggestionLayoutId ?: android.R.layout.simple_spinner_dropdown_item
        updateSuggestionsAdapter()
    }

    /** Get the layout used to show the suggestion list. */
    fun getSsvSuggestionLayout() = mSuggestionLayout

    /**
     * Sets the [suggestions] to show when writing.
     * When the user clicks on a suggestion, [f] will be called.
     */
    fun setSsvSuggestions(suggestions: List<String>?, f: (String) -> Unit) {
        mSuggestionsAdapter = mSuggestionsAdapter ?: ArrayAdapter(context, mSuggestionLayout)
        mSuggestionsAdapter?.clear()
        suggestions?.let { mSuggestionsAdapter?.addAll(it) }
        val searchAutoComplete = findViewById<SearchAutoComplete>(R.id.search_src_text)

        searchAutoComplete.setOnItemClickListener { _, _, position, _ ->
            mSuggestionsAdapter?.getItem(position)?.let { logVerbose("Clicked on $it"); f(it) }
        }
        post { searchAutoComplete.setAdapter(mSuggestionsAdapter) }
    }

    /** Gets the suggestions shown when writing. */
    fun getSsvSuggestions(): List<String> = (0 until (mSuggestionsAdapter?.count ?: 0)).mapNotNull {
        mSuggestionsAdapter?.getItem(it)
    }

    private fun updateSuggestionsAdapter() {
        if (mSuggestionsAdapter == null) return
        val oldItems = (0 until (mSuggestionsAdapter?.count ?: 0))
            .map { mSuggestionsAdapter?.getItem(it) }
        mSuggestionsAdapter = ArrayAdapter(context, mSuggestionLayout)
        mSuggestionsAdapter?.addAll(oldItems)
        val searchAutoComplete = findViewById<SearchAutoComplete>(R.id.search_src_text)
        post { searchAutoComplete.setAdapter(mSuggestionsAdapter) }
    }
}
