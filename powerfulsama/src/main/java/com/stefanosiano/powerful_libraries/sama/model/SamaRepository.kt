package com.stefanosiano.powerful_libraries.sama.model

import androidx.databinding.BaseObservable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.stefanosiano.powerful_libraries.sama.addSourceLd
import com.stefanosiano.powerful_libraries.sama.onChange
import com.stefanosiano.powerful_libraries.sama.removeSourceLd
import kotlinx.coroutines.*

abstract class SamaRepository : CoroutineScope {

    override val coroutineContext = SupervisorJob() + CoroutineExceptionHandler { _, t -> t.printStackTrace() }

    /** Run [f] to get a [LiveData] every time any of [obs] changes. It return a [LiveData] of the same type as [f] */
    fun <T> observe(vararg obs: BaseObservable, f: suspend () -> LiveData<T>?): LiveData<T> {
        val mediatorLiveData = MediatorLiveData<T>()
        var lastLiveData: LiveData<T>? = null

        val onChanged = suspend {
            lastLiveData?.also { mediatorLiveData.removeSourceLd(it) }
            lastLiveData = f()
            if(lastLiveData != null) mediatorLiveData.addSourceLd(lastLiveData!!) {
                mediatorLiveData.postValue(it)
            }
        }

        //the first time this function is called nothing is changed, so i force the reload manually
        var lastJob = launch { onChanged() }
        obs.forEach {
            it.onChange(this) {
                lastJob.cancel()
                lastJob = launch { delay(50); if(!isActive) return@launch; onChanged() }
            }
        }

        return mediatorLiveData
    }

    /** Run [f] to get a value every time any of [obs] changes. It return a [LiveData] of the same type as [f] */
    fun <T> observeF(vararg obs: BaseObservable, f: suspend () -> T): LiveData<T> {
        val mediatorLiveData = MediatorLiveData<T>()

        val onChanged = suspend { mediatorLiveData.postValue(f()) }

        //the first time this function is called nothing is changed, so i force the reload manually
        var lastJob = launch { onChanged() }
        obs.forEach {
            it.onChange(this) {
                lastJob.cancel()
                lastJob = launch { delay(50); if(!isActive) return@launch; onChanged() }
            }
        }

        return mediatorLiveData
    }

}