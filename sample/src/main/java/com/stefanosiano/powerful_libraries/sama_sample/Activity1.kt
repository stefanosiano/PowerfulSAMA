package com.stefanosiano.powerful_libraries.sama_sample

import android.os.Bundle
import com.stefanosiano.powerful_libraries.sama.annotations.SamaExtensions
import com.stefanosiano.powerful_libraries.sama.generatedextensions.defaultRestore
import com.stefanosiano.powerful_libraries.sama.utils.ObservableF
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.view.SamaDialogFragment

@SamaExtensions
internal class Activity1 : SamaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}

internal class SampleDF(
    private val layoutId: Int,
    private val dataBindingId: Int = -1,
    private val bindingData: Any? = null,
    private val fullWidth: Boolean = true,
    private val fullHeight: Boolean = false,
    private val uid: Int = -1
) : SamaDialogFragment(layoutId, dataBindingId, bindingData, fullWidth, fullHeight, uid) {

    var asd = false
    var sss = ObservableF(0)

    override fun restore(oldDialog: SamaDialogFragment) = defaultRestore(oldDialog)
}
