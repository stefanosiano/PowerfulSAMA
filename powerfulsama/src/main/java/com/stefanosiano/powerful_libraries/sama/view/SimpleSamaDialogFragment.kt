package com.stefanosiano.powerful_libraries.sama.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.stefanosiano.powerful_libraries.sama.logVerbose
import java.util.*

/** Abstract DialogFragment for all DialogFragments to extend. */
open class SimpleSamaDialogFragment : DialogFragment() {

    private var layoutId: Int = 0
    private var fullScreen: Boolean = false
    private var fullHeight: Boolean = false
    private val bindingPairs: MutableList<Pair<Int, Any>> = ArrayList()
    private var onViewCreated: ((view: View) -> Unit)? = null
    private var onActivityCreated: (() -> Unit)? = null

    /**
     * Sets the data to work with data binding
     * Calling this method multiple times will associate the id to the last data passed.
     * Multiple dataBindingIds are allowed
     *
     * @param dataBindingId the id of the variable in the layout
     * @param bindingData the data to bind to the id
     */
    fun with(dataBindingId: Int, bindingData: Any): SimpleSamaDialogFragment {
        if (!this.bindingPairs.asSequence().map { it.first }.contains(dataBindingId)) {
            this.bindingPairs.add(Pair(dataBindingId, bindingData))
        }
        return this
    }

    /**
     * Sets the dialog as data to work with data binding.
     *
     * @param dataBindingId the id of the dialog variable in the layout.
     */
    fun setDialogAsVariable(dataBindingId: Int): SimpleSamaDialogFragment {
        if (!this.bindingPairs.asSequence().map { it.first }.contains(dataBindingId)) {
            this.bindingPairs.add(Pair(dataBindingId, this))
        }
        return this
    }

    /**
     * Sets a function to be called when the view is created:
     * the dialog is fully shown, but not yet attached to its parent.
     *
     * @param onViewCreated function to call when the view is created
     */
    fun setOnViewCreated(onViewCreated: (view: View) -> Unit): SimpleSamaDialogFragment {
        this.onViewCreated = onViewCreated
        return this
    }

    /**
     * Sets a function to be called when the activity is created (the dialog is fully shown and attached to its parent).
     *
     * @param onActivityCreated function to call when the view is created.
     */
    fun setOnActivityCreated(onActivityCreated: () -> Unit): SimpleSamaDialogFragment {
        this.onActivityCreated = onActivityCreated
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logVerbose("onCreate")
        this.layoutId = arguments?.getInt(ExtraLayoutId) ?: 0
        this.fullScreen = arguments?.getBoolean(ExtraFullScreen) ?: false
        this.fullHeight = arguments?.getBoolean(ExtraFullHeight) ?: false
    }

    override fun onResume() {
        super.onResume()
        logVerbose("onResume")
    }

    override fun onStart() {
        super.onStart()
        logVerbose("onStart")
        if (fullScreen || fullHeight) {
            dialog?.window?.setLayout(
                if (fullScreen) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
                if (fullHeight) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onPause() {
        super.onPause()
        logVerbose("onPause")
    }

    override fun onStop() {
        super.onStop()
        logVerbose("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        logVerbose("onDestroy")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        if (bindingPairs.isNotEmpty()) {
            val binding: ViewDataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)
            for (pair in bindingPairs)
                binding.setVariable(pair.first, pair.second)
            return binding.root
        }

        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewCreated?.invoke(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        onActivityCreated?.invoke()
    }

    /** Function used to call [dismiss] from dataBinding. */
    @Suppress("UNUSED_PARAMETER")
    fun dismiss(view: View) = dismiss()

    /** Shows the dialog fragment, without using a tag. */
    fun show(manager: FragmentManager) = super.show(manager, tag)

    companion object {

        private const val ExtraLayoutId = "ExtraLayoutId"
        private const val ExtraFullScreen = "ExtraFullScreen"
        private const val ExtraFullHeight = "ExtraFullHeight"

        /**
         * Creates a new SimpleSamaDialogFragment.
         * @param layoutId The id of the layout to use. (0 means no layout is shown).
         * @param fullScreen Forces the dialog to be in full width mode.
         * @param fullHeight Forces the dialog to be in full height mode.
         */
        fun new(layoutId: Int, fullScreen: Boolean = false, fullHeight: Boolean = false): SimpleSamaDialogFragment {
            val fragment = SimpleSamaDialogFragment()
            val bundle = Bundle()
            bundle.putInt(ExtraLayoutId, layoutId)
            bundle.putBoolean(ExtraFullScreen, fullScreen)
            bundle.putBoolean(ExtraFullHeight, fullHeight)
            fragment.arguments = bundle
            return fragment
        }
    }
}
