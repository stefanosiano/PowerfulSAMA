package com.stefanosiano.powerful_libraries.sama.view

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.databinding.ViewDataBinding
import com.stefanosiano.powerful_libraries.sama.toWeakReference
import com.stefanosiano.powerful_libraries.sama.ui.SimpleSearchView
import java.lang.ref.WeakReference

/** Base Class that provides easy way to use data binding with a fragment without the need of other classes */
class SimpleFragment: SamaFragment() {

    private var layoutId: Int = 0
    private var menuId: Int = 0
    private var searchMenuId: Int = 0
    private var searchString: ObservableField<String>? = null
    private val bindingPairs: MutableList<Pair<Int, Any>> = ArrayList()
    private val menuFunctions: MutableList<Pair<Int, () -> Unit>> = ArrayList()
    private var title = ""
    private var defaultTitle = ""
    private var onOptionMenuCreated: ((menu: Menu?) -> Unit)? = null
    private var searchView: WeakReference<SimpleSearchView>? = null

    companion object {

        private const val ExtraLayoutId = "ExtraLayoutId"
        private const val ExtraMenuId = "ExtraMenuId"

        /**
         * Creates a new SimpleFragment
         * @param layoutId The id of the layout to use. (0 means no layout is shown)
         */
        fun newInstance(layoutId: Int): SimpleFragment = newInstance(layoutId, 0)

        /**
         * Creates a new SimpleFragment
         * @param layoutId The id of the layout to use. (0 means no layout is shown)
         * @param menuId The id of the menu to load. (0 means no menu is shown)
         */
        fun newInstance(layoutId: Int, menuId: Int): SimpleFragment {

            val fragment = SimpleFragment()
            val bundle = Bundle()
            bundle.putInt(ExtraLayoutId, layoutId)
            bundle.putInt(ExtraMenuId, menuId)
            fragment.arguments = bundle
            return fragment
        }

    }

    /**
     * Sets the data to work with data binding
     * Calling this method multiple times will associate the id to the last data passed.
     * Multiple dataBindingIds are allowed
     *
     * @param dataBindingId the id of the variable in the layout
     * @param bindingData the data to bind to the id
     */
    fun with(dataBindingId: Int, bindingData: Any): SimpleFragment {
        if(!this.bindingPairs.asSequence().map { it.first }.contains(dataBindingId))
            this.bindingPairs.add(Pair(dataBindingId, bindingData))
        return this
    }

    /** Sets the title to show on the toolbar (optional) */
    fun title(titleId: Int): SimpleFragment {
        this.title = getString(titleId)
        return this
    }

    /** Sets the title to show on the toolbar (optional) */
    fun title(title: String): SimpleFragment {
        this.title = title
        return this
    }

    /** Binds an observableString to a SimpleSearchView
     *
     * @param searchMenuId id of the searchView
     * @param searchString observableString to binds the searchView to
     */
    fun search(searchMenuId: Int, searchString: ObservableField<String>): SimpleFragment {
        this.searchMenuId = searchMenuId
        this.searchString = searchString
        return this
    }

    /**
     * Executes [function] when [menuId] is clicked
     *
     * @param menuId
     * @param function
     */
    fun onOptionSelected(menuId: Int, function: () -> Unit): SimpleFragment {
        menuFunctions.add(Pair(menuId, function))
        return this
    }

    /** Sets the function to call when creating the menu (optional) */
    fun onOptionMenuCreated(onOptionMenuCreated: (menu: Menu?) -> Unit): SimpleFragment {
        this.onOptionMenuCreated = onOptionMenuCreated
        return this
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        menuFunctions.firstOrNull { it.first == item?.itemId }?.second?.invoke() ?: return super.onOptionsItemSelected(item)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        if(menuId != 0)
            inflater?.inflate(menuId, menu)
        onOptionMenuCreated?.invoke(menu)

        searchView = if(searchMenuId != 0) (menu?.findItem(searchMenuId)?.actionView as? SimpleSearchView)?.toWeakReference() else null
        searchView?.get()?.bindQuery(searchString)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu()
        searchView?.get()?.unbindQuery(searchString ?: return)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (defaultTitle.isEmpty()) this.defaultTitle = activity?.title?.toString() ?: ""
    }

    override fun onDetach() {
        super.onDetach()
        searchView?.get()?.unbindQuery(searchString ?: return)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        this.layoutId = arguments?.getInt(ExtraLayoutId) ?: 0
        this.menuId = arguments?.getInt(ExtraMenuId) ?: 0
        this.setHasOptionsMenu(menuId != 0)

        if(layoutId == 0) return null

        val binding: ViewDataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        bindingPairs.forEach { binding.setVariable(it.first, it.second) }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(title.isNotEmpty()) activity?.title = title
        else if (defaultTitle.isNotEmpty()) activity?.title = defaultTitle
    }

    /** Clear references of dataBinding, searchView observable and menu functions */
    override fun clear() {
        bindingPairs.clear()
        menuFunctions.clear()
        onOptionMenuCreated = null
        searchView?.get()?.unbindQuery(searchString)
        searchView?.clear()
        searchView = null
        searchString = null
    }
}