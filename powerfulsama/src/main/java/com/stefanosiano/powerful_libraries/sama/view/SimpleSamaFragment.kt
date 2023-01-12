package com.stefanosiano.powerful_libraries.sama.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.stefanosiano.powerful_libraries.sama.logVerbose

/** Base Class that provides easy way to use data binding with a fragment without the need of other classes. */
open class SimpleSamaFragment : SamaFragment() {

    private var layoutId: Int = 0
    private var menuId: Int = 0
    private val bindingPairs: MutableList<Pair<Int, Any>> = ArrayList()
    private val menuFunctions: MutableList<Pair<Int, () -> Unit>> = ArrayList()
    private var title = ""
    private var defaultTitle = ""
    private var onOptionMenuCreated: ((menu: Menu?) -> Unit)? = null
    private var onDetach: ((fragment: SimpleSamaFragment) -> Unit)? = null

    companion object {

        private const val ExtraLayoutId = "ExtraLayoutId"
        private const val ExtraMenuId = "ExtraMenuId"

        /**
         * Creates a new SimpleFragment
         * @param layoutId The id of the layout to use. (0 means no layout is shown)
         */
        fun new(layoutId: Int): SimpleSamaFragment = new(layoutId, 0)

        /**
         * Creates a new SimpleFragment
         * @param layoutId The id of the layout to use. (0 means no layout is shown)
         * @param menuId The id of the menu to load. (0 means no menu is shown)
         */
        fun new(layoutId: Int, menuId: Int): SimpleSamaFragment {
            val fragment = SimpleSamaFragment()
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
    fun with(dataBindingId: Int, bindingData: Any): SimpleSamaFragment {
        if (!this.bindingPairs.asSequence().map { it.first }.contains(dataBindingId)) {
            this.bindingPairs.add(Pair(dataBindingId, bindingData))
        }
        return this
    }

    /** Sets the title to show on the toolbar (optional). */
    fun title(titleId: Int): SimpleSamaFragment {
        this.title = getString(titleId)
        return this
    }

    /** Sets the title to show on the toolbar (optional). */
    fun title(title: String): SimpleSamaFragment {
        this.title = title
        return this
    }

    /** Sets the tag shown in logs. */
    fun logTag(logTag: String): SimpleSamaFragment {
        this.logTag = logTag
        return this
    }

    /**
     * Executes [function] when [menuId] is clicked
     *
     * @param menuId
     * @param function
     */
    fun onOption(menuId: Int, function: () -> Unit): SimpleSamaFragment {
        menuFunctions.add(Pair(menuId, function))
        return this
    }

    /** Sets the function to call when creating the menu (optional). */
    fun onOptionMenu(onOptionMenuCreated: (menu: Menu?) -> Unit): SimpleSamaFragment {
        this.onOptionMenuCreated = onOptionMenuCreated
        return this
    }

    /** Sets the function to call when this fragment is detached from the activity. */
    fun onDetach(onDetach: (fragment: SimpleSamaFragment) -> Unit): SimpleSamaFragment {
        this.onDetach = onDetach
        return this
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        logVerbose("$logTag: Selected item: ${item.title}")
        menuFunctions.firstOrNull { it.first == item.itemId }?.second?.invoke() ?: return super.onOptionsItemSelected(
            item
        )
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (menuId != 0) {
            inflater.inflate(menuId, menu)
        }
        onOptionMenuCreated?.invoke(menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (defaultTitle.isEmpty()) this.defaultTitle = activity?.title?.toString() ?: ""
    }

    override fun onDetach() {
        onDetach?.invoke(this)
        super.onDetach()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.layoutId = arguments?.getInt(ExtraLayoutId) ?: 0
        this.menuId = arguments?.getInt(ExtraMenuId) ?: 0
        this.setHasOptionsMenu(menuId != 0)

        if (layoutId == 0) return null

        val binding: ViewDataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        bindingPairs.forEach { binding.setVariable(it.first, it.second) }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (title.isNotEmpty()) {
            activity?.title = title
        } else if (defaultTitle.isNotEmpty()) activity?.title = defaultTitle
    }

    /** Clear references of dataBinding, searchView observable and menu functions. */
    override fun clear() {
        bindingPairs.clear()
        menuFunctions.clear()
        onOptionMenuCreated = null
    }
}
