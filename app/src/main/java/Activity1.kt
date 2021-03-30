package sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.stefanosiano.powerful_libraries.sama.BR
import com.stefanosiano.powerful_libraries.sama.generatedextensions.defaultRestore
import com.stefanosiano.powerful_libraries.sama.utils.Msg
import com.stefanosiano.powerful_libraries.sama.utils.ObservableF
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.view.SamaDialogFragment
import com.stefanosiano.powerful_libraries.sama_annotations.ActivityIntent
import com.stefanosiano.powerful_libraries.sama_annotations.SamaExtensions
import com.stefanosiano.powerful_libraries.sama_sample.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@SamaExtensions
class Activity1 : SamaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = arrayListOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val m = Msg.adob("asd").show()//.dismiss()
        launch {
            delay(3000)
            m.dismiss()
        }
//        Perms.call(perms, R.string.app_name, R.string.app_name2) {
//            Uri.fromFile(File("")).toFileFromProviders(this, "a")
//            println("asd")
//        }
//        launch {
//            delay(2000)
//            withContext(Dispatchers.Main) {
//                SampleDF(R.layout.sample_df, BR.dialog).show(this@Activity1)
//            }
//        }
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
    private val dataBindingId: Int = -1,
    private val bindingData: Any? = null,
    private val fullWidth: Boolean = true,
    private val fullHeight: Boolean = false,
    private val uid: Int = -1
): SamaDialogFragment(layoutId, dataBindingId, bindingData, fullWidth, fullHeight, uid){

    val id = 0
    var asd = false
    var sss = ObservableF(0)

    override fun restore(oldDialog: SamaDialogFragment) = defaultRestore(oldDialog)

}