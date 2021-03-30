package com.stefanosiano.powerful_libraries.sama.model

import androidx.databinding.BaseObservable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.stefanosiano.powerful_libraries.sama.onChange
import kotlinx.coroutines.*

abstract class SamaRepository {

    @Deprecated("Use [SamaObserver.observeAndReloadLiveData]", ReplaceWith("SamaObserver.observeAndReloadLiveData"))
    /** Run [f] to get a [LiveData] every time any of [obs] changes. It return a [LiveData] of the same type as [f] */
    suspend fun <T> observe(vararg obs: BaseObservable, f: suspend () -> LiveData<T>?): LiveData<T> {
        val mediatorLiveData = MediatorLiveData<T>()
        var lastLiveData: LiveData<T>? = null

        val onChanged = suspend {
            withContext(Dispatchers.Main) { lastLiveData?.also { mediatorLiveData.removeSource(it) } }
            lastLiveData = f()
            withContext(Dispatchers.Main) {
                if(lastLiveData != null) mediatorLiveData.addSource(lastLiveData!!) {
                    mediatorLiveData.postValue(it)
                }
            }
        }

        //the first time this function is called nothing is changed, so i force the reload manually
        var lastJob = runBlocking { launch { onChanged() } }
        obs.forEach {
            it.onChange {
                lastJob.cancel()
                lastJob = runBlocking { launch { delay(50); if(!isActive) return@launch; onChanged() } }
            }
        }

        return mediatorLiveData
    }

    @Deprecated("Use [SamaObserver.observeAndReloadLiveData]", ReplaceWith("SamaObserver.observeAndReloadLiveData"))
    /** Run [f] to get a value every time any of [obs] changes. It return a [LiveData] of the same type as [f] */
    suspend fun <T> observeF(vararg obs: BaseObservable, f: suspend () -> T): LiveData<T> {
        val mediatorLiveData = MediatorLiveData<T>()

        val onChanged = suspend { mediatorLiveData.postValue(f()) }

        //the first time this function is called nothing is changed, so i force the reload manually
        var lastJob = runBlocking { launch { onChanged() } }
        obs.forEach {
            it.onChange {
                lastJob.cancel()
                lastJob = runBlocking { launch { delay(50); if(!isActive) return@launch; onChanged() } }
            }
        }

        return mediatorLiveData
    }

}