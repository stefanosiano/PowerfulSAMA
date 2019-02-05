package com.stefanosiano.powerful_libraries.sama.view

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import java.util.*

/** Abstract DialogFragment for all DialogFragments to extend */
open class SimpleSamaDialogFragment: DialogFragment() {

    private var layoutId: Int = 0
    private var fullScreen: Boolean = false
    private val bindingPairs: MutableList<Pair<Int, Any>> = ArrayList()

    private lateinit var binding: ViewDataBinding

    companion object {

        private const val ExtraLayoutId = "ExtraLayoutId"
        private const val ExtraFullScreen = "ExtraFullScreen"

        /**
         * Creates a new SimpleSamaDialogFragment
         * @param layoutId The id of the layout to use. (0 means no layout is shown)
         */
        fun new(layoutId: Int): SimpleSamaDialogFragment = new(layoutId, false)

        /**
         * Creates a new SimpleSamaDialogFragment
         * @param layoutId The id of the layout to use. (0 means no layout is shown)
         * @param fullScreen Forces the dialog to be in full screen mode
         */
        fun new(layoutId: Int, fullScreen: Boolean): SimpleSamaDialogFragment {

            val fragment = SimpleSamaDialogFragment()
            val bundle = Bundle()
            bundle.putInt(ExtraLayoutId, layoutId)
            bundle.putBoolean(ExtraFullScreen, fullScreen)
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
    fun with(dataBindingId: Int, bindingData: Any): SimpleSamaDialogFragment {
        if(!this.bindingPairs.asSequence().map { it.first }.contains(dataBindingId))
            this.bindingPairs.add(Pair(dataBindingId, bindingData))
        return this
    }


    /**
     * Sets the dialog as data to work with data binding
     *
     * @param dataBindingId the id of the dialog variable in the layout
     */
    fun setDialogAsVariable(dataBindingId: Int): SimpleSamaDialogFragment {
        if(!this.bindingPairs.asSequence().map { it.first }.contains(dataBindingId))
            this.bindingPairs.add(Pair(dataBindingId, this))
        return this
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.layoutId = arguments?.getInt(ExtraLayoutId) ?: 0
        this.fullScreen = arguments?.getBoolean(ExtraFullScreen) ?: false
    }

    override fun onStart() {
        super.onStart()
        if(fullScreen) {
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        if(bindingPairs.isNotEmpty()) {
            binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
            for (pair in bindingPairs)
                binding.setVariable(pair.first, pair.second)
            return binding.root
        }

        return inflater.inflate(layoutId, container, false)
    }

    /** Function used to call [dismiss] from dataBinding */
    fun dismiss(view: View) = dismiss()

    /** Shows the dialog fragment, without using a tag */
    fun show(manager: FragmentManager?) = super.show(manager, tag)
}