package sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.stefanosiano.powerful_libraries.sama.generated.startActivity2
import com.stefanosiano.powerful_libraries.sama.generated.startActivity2ForResult
import com.stefanosiano.powerful_libraries.sama.utils.Perms
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama_annotations.ActivityIntent
import com.stefanosiano.powerful_libraries.sama_annotations.SamaExtensions
import com.stefanosiano.powerful_libraries.sama_sample.R

@SamaExtensions
class Activity1 : SamaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = arrayListOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        Perms.call(perms, R.string.app_name, R.string.app_name2) {
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
//        startActivityForResult()
    }
}