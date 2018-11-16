package com.stefanosiano.powerfulsama.view

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.stefanosiano.powerfulsama.VmResponse
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel


/**
 * Abstract Activity for all Activities to extend.
 */
abstract class BaseActivity : AppCompatActivity(), CoroutineScope {
    private val loggingExceptionHandler = CoroutineExceptionHandler { _, t -> t.printStackTrace() }
    override val coroutineContext = Job() + loggingExceptionHandler


    override fun onStop() {
        super.onStop()
        coroutineContext.cancel()
    }


    /** Observes the liveData using this activity as lifecycle owner */
    protected fun <T> observe(liveData: LiveData<T>, observerFunction: (data: T?) -> Unit) = liveData.observe(this, Observer { observerFunction.invoke(it) })


    /**
     * Handles the response of the ViewModel, in case everything went alright.
     *
     * @param vmAction Action sent from the ViewModel. It will never be null.
     * @param vmData Data sent from the ViewModel. It can be null.
     * @return True to clear the response after being sent to the observer. False to retain it.
     * If false, the response should be cleared using [com.stefanosiano.permissionruler.viewmodel.BaseViewModel.clearVmResponse] method.
     */
    open fun handleVmResponse(vmAction: VmResponse.VmAction, vmData: Any?): Boolean {
        //This method does nothing. It's here just to have a reference to the javadoc used by extending activities
        return true
    }

    /**
     * Handles the error of the ViewModel, in case something went wrong.
     *
     *
     *
     * NOTE: Here you should check the error of the response, because there was an error.
     * <br></br> <br></br>
     * It's perfectly safe to check only the error and not the action: the whole response is returned
     * just to access more info, or to use action to group different errors together.
     * <br></br> <br></br>
     * The error and the action of the response will never be null!
     * <br></br>
     * The data of the response can be null!
     *
     *
     * @param vmResponse Response sent from the ViewModel. It will never be null.
     * @return True to clear the response after being sent to the observer. False to retain it.
     * If false, the response should be cleared using [com.stefanosiano.permissionruler.viewmodel.BaseViewModel.clearVmResponse] method.
     *//*
    protected fun handleVmResponseError(vmResponse: VmResponse<out VmResponse.VmAction, out VmResponse.VmError, Any>): Boolean {
        //This method does nothing. It's here just to have a reference to the javadoc used by extending activities
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        //Dismisses any message shown by this activity using the Message show() method. Used to avoid context leaks.
        Messages.dismissAll(this)
    }*/
}
