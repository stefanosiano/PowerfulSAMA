package com.stefanosiano.powerful_libraries.sama.view

import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.stefanosiano.powerful_libraries.sama.viewModel.VmResponse
import kotlinx.coroutines.*

/** Abstract Activity for all Activities to extend */
abstract class SamaActivity : AppCompatActivity(), CoroutineScope {
    private val loggingExceptionHandler = CoroutineExceptionHandler { _, t -> t.printStackTrace() }
    override val coroutineContext = SupervisorJob() + loggingExceptionHandler

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel()
    }

    /** Initializes the toolbar leaving the default title */
    protected fun initActivity() = initActivity("")

    /** Initializes the toolbar with the title provided */
    protected fun initActivity(titleId: Int){
        if(titleId != 0) initActivity(getString(titleId))
        else initActivity("")
    }

    /** Initializes the toolbar with the title provided */
    protected fun initActivity(title: String){
        if(title.isNotEmpty()) supportActionBar?.title = title
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true

        }
        return super.onOptionsItemSelected(item)
    }

    /** Observes the liveData using this activity as lifecycle owner */
    protected fun <T> observe(liveData: LiveData<T>, observerFunction: (data: T?) -> Unit) = liveData.observe(this, Observer { observerFunction.invoke(it) })


    /**
     * Handles the response of the ViewModel, in case everything went alright.
     *
     * @param vmAction Action sent from the ViewModel. It will never be null.
     * @param vmData Data sent from the ViewModel. It can be null.
     * @return True to clear the response after being sent to the observer. False to retain it.
     * If false, the response should be cleared using [com.stefanosiano.powerful_libraries.sama.viewModel.SamaViewModel.clearVmResponse] method.
     */
    open fun handleVmResponse(vmAction: VmResponse.VmAction, vmData: Any?): Boolean {
        //This method does nothing. It's here just to have a reference to the javadoc used by extending activities
        return true
    }



    val samaIntent
        /** Returns the intent that started this activity as [SamaIntent], allowing the use of [SamaIntent.getExtraStatic] */
        get() = SamaIntent(super.getIntent())
}
