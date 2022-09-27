package com.stefanosiano.powerful_libraries.samasample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.databinding.ObservableField
import com.stefanosiano.powerful_libraries.sama.generatedextensions.defaultRestore
import com.stefanosiano.powerful_libraries.sama.utils.Msg
import com.stefanosiano.powerful_libraries.sama.utils.ObservableF
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.view.SamaDialogFragment
import com.stefanosiano.powerful_libraries.sama_annotations.SamaExtensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SamaExtensions
internal class Activity1 : SamaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ob = ObservableField("aaa")
        observe(ob) { Log.e("AAAA", it) }
        val m = Msg.adob("asd").show()
        launch {
            delay(100)
            m.dismiss()
            ob.set("10")
        }
    }
}

internal class Activity2 : Activity() {

    fun function() {
        val a = SampleDF(0, 0, 0)
        a.id
    }

    companion object {
        private const val ExtraSync = "sync"

        fun getIntent2(context: Activity, sync: Boolean): Intent =
            Intent(context, Activity2::class.java).apply {
                putExtra(ExtraSync, sync)
            }
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

    val id = 0
    var asd = false
    var sss = ObservableF(0)

    override fun restore(oldDialog: SamaDialogFragment) = defaultRestore(oldDialog)
}
