package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.databinding.ObservableList
import com.stefanosiano.powerful_libraries.sama.*
import kotlinx.coroutines.*

/** Class that provides easy to use SearchView with data binding */
open class SamaSearchView : SearchView, CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Adapter used to show suggestions while searching */
    private var mSuggestionsAdapter: ArrayAdapter<String>? = null

    private var mSuggestionLayout: Int = android.R.layout.simple_spinner_dropdown_item

    /** Delay in milliseconds to execute the listener or update the observable */
    private var millis = 0L

    /** Job used to handle the delay */
    private var requeryJob: Job? = null

    /** Flag to decide whether to clear focus and close keyboard when submitting a query */
    private var clearFocusOnSubmit = true

    /** Key to use after setting items (if Key was selected before items were available) */
    private val listeners = ArrayList<OnQueryTextListener>()

    /** Key to use after setting items (if Key was selected before items were available) */
    private val registeredObservers = ArrayList<Pair<ObservableField<String>, Observable.OnPropertyChangedCallback>>()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.searchViewStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val attrSet = context.theme.obtainStyledAttributes(attrs, R.styleable.SamaSearchView, defStyleAttr, 0)
        clearFocusOnSubmit = attrSet.getBoolean(R.styleable.SamaSearchView_ssvClearFocusOnSubmit, clearFocusOnSubmit)
        millis = attrSet.getInt(R.styleable.SamaSearchView_ssvMillis, 0).toLong()
        mSuggestionLayout = attrSet.getInt(R.styleable.SamaSearchView_ssvSuggestionLayout, -1)
        val query = attrSet.getString(R.styleable.SamaSearchView_ssvQuery) ?: ""
        attrSet.recycle()
        setQuery(query, true)
    }

    init {
        super.setOnQueryTextListener ( object : OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                synchronized(listeners) { listeners.forEach { it.onQueryTextSubmit(query) } }
                synchronized(registeredObservers) { registeredObservers.forEach { it.first.set(query ?: "") } }
                if(clearFocusOnSubmit) clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText ?: return true

                requeryJob?.cancel()
                if(millis > 0) {
                    requeryJob = launch {
                        delay(millis)
                        if(isActive) {
                            synchronized(listeners) { listeners.forEach { it.onQueryTextChange(newText) } }
                            synchronized(registeredObservers) { registeredObservers.forEach { it.first.set(newText ?: "") } }
                        }
                    }
                }
                else {
                    synchronized(listeners) { listeners.forEach { it.onQueryTextChange(newText) } }
                    synchronized(registeredObservers) { registeredObservers.forEach { it.first.set(newText ?: "") } }
                }

                return true
            }
        })
    }

    override fun setOnQueryTextListener(listener: OnQueryTextListener?) {
        listener ?: return
        synchronized(listeners) { listeners.add(listener) }
    }

    fun addOnQueryTextListener(listener: OnQueryTextListener) {
        synchronized(listeners) { listeners.add(listener) }
    }

    fun setSsvMillis(millis: Int?) { this.millis = (millis?:0).toLong() }
    fun getSsvMillis() = millis.toInt()

    fun setSsvSuggestionLayout(suggestionLayoutId: Int?) { this.mSuggestionLayout = suggestionLayoutId ?: android.R.layout.simple_spinner_dropdown_item; updateSuggestionsAdapter() }
    fun getSsvSuggestionLayout() = mSuggestionLayout

    fun setSsvQuery(query: String?) { if(query != getSsvQuery()) setQuery(query, true) }
    fun getSsvQuery() = query.toString()


    private fun updateSuggestionsAdapter() {
        if(mSuggestionsAdapter == null) return
        val oldItems = (0 until (mSuggestionsAdapter?.count ?: 0)).map { mSuggestionsAdapter?.getItem(it) }
        mSuggestionsAdapter = ArrayAdapter(context, mSuggestionLayout)
        mSuggestionsAdapter?.addAll(oldItems)
        val searchAutoComplete = findViewById<SearchAutoComplete>(R.id.search_src_text)
        runOnUi { searchAutoComplete.setAdapter(mSuggestionsAdapter) }
    }

    /** Sets the [suggestions] to show when writing. When the user clicks on a suggestion, [f] will be called */
    private fun setSuggestions(suggestions: List<String>, f: (String) -> Unit){
        mSuggestionsAdapter = mSuggestionsAdapter ?: ArrayAdapter(context, mSuggestionLayout)
        mSuggestionsAdapter?.clear()
        mSuggestionsAdapter?.addAll(suggestions)
        val searchAutoComplete = findViewById<SearchAutoComplete>(R.id.search_src_text)

        searchAutoComplete.setOnItemClickListener { _, _, position, _ ->
            mSuggestionsAdapter?.getItem(position)?.let { logVerbose("Clicked on $it"); f(it) }
        }
        runOnUi { searchAutoComplete.setAdapter(mSuggestionsAdapter) }
    }

    fun bindQuery(query: ObservableField<String>) {
        synchronized(registeredObservers) { registeredObservers.add(Pair(query, query.addOnChangedAndNow { if(it != getSsvQuery()) setSsvQuery(it) })) }
    }

    fun clearBoundQueries() {
        synchronized(registeredObservers) { registeredObservers.forEach { it.first.removeOnPropertyChangedCallback(it.second) } }
    }
}