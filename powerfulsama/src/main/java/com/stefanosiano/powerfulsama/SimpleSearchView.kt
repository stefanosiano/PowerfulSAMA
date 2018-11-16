package com.stefanosiano.powerfulsama

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import androidx.appcompat.widget.SearchView
import androidx.databinding.Observable
import androidx.databinding.ObservableField

/**
 * Class that provides easy to use SearchView with data binding
 */
class SimpleSearchView : SearchView {

    /** Delay in milliseconds to execute the listener or update the observable */
    var millis = 0L

    /** Optional function to call when the query of the searchView changes */
    var onQueryChanged: ((line: String) -> Unit)? = null

    /** Handler used to handle the delay */
    private val requeryHandler = Handler()

    /** Current query */
    private var currentQuery = ""

    /** Set of observable strings to update when the query changes */
    private var obserablesSet: MutableSet<ObservableField<String>> = HashSet()

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
    private fun onQueryUpdated(){
        onQueryChanged?.invoke(currentQuery)
        obserablesSet.forEach { it.set(currentQuery) }
    }

    /**
     * Adds the observable strings to the internal list.
     * When the query changes, all registered observables are updated.
     * When one observable changes, the query is updated
     */
    fun bindQuery(queryObs : ObservableField<String>?) {
        if(queryObs == null) return

        obserablesSet.add(queryObs)

        queryObs.addOnPropertyChangedCallback ( object : Observable.OnPropertyChangedCallback() {

            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if(obserablesSet.contains(queryObs) && queryObs.get() != currentQuery)
                    setQuery(queryObs.get(), false)
            }
        } )
    }

    /** Removes the observable from the registered observables */
    fun unbindQuery(queryObs : ObservableField<String>) = obserablesSet.remove(queryObs)

}