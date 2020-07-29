package sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.databinding.ObservableInt
import com.stefanosiano.powerful_libraries.sama.generatedextensions.defaultRestore
import com.stefanosiano.powerful_libraries.sama.toFileFromProviders
import com.stefanosiano.powerful_libraries.sama.utils.ObservableF
import com.stefanosiano.powerful_libraries.sama.utils.Perms
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.view.SamaDialogFragment
import com.stefanosiano.powerful_libraries.sama_annotations.ActivityIntent
import com.stefanosiano.powerful_libraries.sama_annotations.SamaExtensions
import com.stefanosiano.powerful_libraries.sama_sample.R
import java.io.File

@SamaExtensions
class Activity1 : SamaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = arrayListOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        Perms.call(perms, R.string.app_name, R.string.app_name2) {
            Uri.fromFile(File("")).toFileFromProviders(this, "a")
            println("asd")
        }

//        startActivity2ForResult(true, 1, null)
    }
}

class Activity2 : Activity() {

    companion object {
        private const val ExtraSync = "sync"
        private const val ExtraIsTempPassword = "isTempPassword"

        @ActivityIntent
        /** asd */
        fun getIntent(context: Context, sync: Boolean?, isTempPassword: Boolean = true): Intent =
            Intent(context, Activity2::class.java).apply {
                putExtra(ExtraSync, sync)
                putExtra(ExtraIsTempPassword, isTempPassword)
            }

        /** dsa */
        @ActivityIntent
        fun getIntent(context: Activity, sync: Boolean): Intent =
            Intent(context, Activity2::class.java).apply {
                putExtra(ExtraSync, sync)
            }

        /** */
//        @ActivityIntent("A2")
        fun getIntent2(context: Activity, sync: Boolean): Intent =
            Intent(context, Activity2::class.java).apply {
                putExtra(ExtraSync, sync)
            }



    }

    fun a() {
        val a = SampleDF(0,0,0)
        a.id
//        startActivityForResult()
    }
}

class SampleDF(
    private val layoutId: Int,
    private val dataBindingId: Int,
    private val uid: Int
): SamaDialogFragment<SampleDF>(layoutId, dataBindingId, uid){

    val id = 0
    var asd = false
    var sss = ObservableF(0)

    override fun restore(oldDialog: SampleDF) = defaultRestore(oldDialog)

}