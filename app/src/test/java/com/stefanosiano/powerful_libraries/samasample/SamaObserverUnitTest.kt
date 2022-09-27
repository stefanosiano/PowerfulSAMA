package com.stefanosiano.powerful_libraries.samasample

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableByte
import androidx.databinding.ObservableChar
import androidx.databinding.ObservableDouble
import androidx.databinding.ObservableField
import androidx.databinding.ObservableFloat
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableLong
import androidx.databinding.ObservableShort
import androidx.lifecycle.MutableLiveData
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserverImpl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class SamaObserverUnitTest : CoroutineScope {

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineJob + CoroutineExceptionHandler { _, t -> t.printStackTrace() }

    // Rule for Main Looper
    @get:Rule val taskExecutorRule = InstantTaskExecutorRule()

    /** Test if observing a variable calls the corresponding lambda functions on changes. */
    @Suppress("LongMethod")
    @Test
    fun `observeOnChange works only from startObserve to stopObserver`() {
        val observer: SamaObserver = SamaObserverImpl()

        var boolToSet = false
        var intToSet = 0
        var doubleToSet = 0.0
        var shortToSet = 0.toShort()
        var byteToSet = 0.toByte()
        var charToSet = '0'
        var stringToSet = ""
        var floatToSet = 0F
        var longToSet = 0L
        var liveDataToSet = "0"
        var flowToSet = "0"

        val boolToObserve = ObservableBoolean(boolToSet)
        val intToObserve = ObservableInt(intToSet)
        val doubleToObserve = ObservableDouble(doubleToSet)
        val shortToObserve = ObservableShort(shortToSet)
        val byteToObserve = ObservableByte(byteToSet)
        val charToObserve = ObservableChar(charToSet)
        val stringToObserve = ObservableField(stringToSet)
        val floatToObserve = ObservableFloat(floatToSet)
        val longToObserve = ObservableLong(longToSet)
        val liveDataToObserve = MutableLiveData(liveDataToSet)
        val flowToObserve = MutableStateFlow(flowToSet)

        observer.initObserver(this)

        observer.observe(boolToObserve) { boolToSet = it }
        observer.observe(intToObserve) { intToSet = it }
        observer.observe(doubleToObserve) { doubleToSet = it }
        observer.observe(shortToObserve) { shortToSet = it }
        observer.observe(byteToObserve) { byteToSet = it }
        observer.observe(charToObserve) { charToSet = it }
        observer.observe(stringToObserve) { stringToSet = it }
        observer.observe(floatToObserve) { floatToSet = it }
        observer.observe(longToObserve) { longToSet = it }
        observer.observe(liveDataToObserve) { liveDataToSet = it }
        observer.observe(flowToObserve) { flowToSet = it }

        boolToObserve.set(true)
        intToObserve.set(1)
        doubleToObserve.set(1.0)
        shortToObserve.set(1.toShort())
        byteToObserve.set(1.toByte())
        charToObserve.set('1')
        stringToObserve.set("1")
        floatToObserve.set(1F)
        longToObserve.set(1L)
        liveDataToObserve.postValue("1")
        runBlocking { flowToObserve.emit("1") }

        // Observing happens in background: should wait a very short delay for changes to happen
        // observer.startObserver() not called yet. Values shouldn't have changed
        runBlocking { delay(10) }
        Assert.assertEquals(false, boolToSet)
        Assert.assertEquals(0, intToSet)
        Assert.assertEquals(0.0, doubleToSet, 0.0)
        Assert.assertEquals(0.toShort(), shortToSet)
        Assert.assertEquals(0.toByte(), byteToSet)
        Assert.assertEquals('0', charToSet)
        Assert.assertEquals("", stringToSet)
        Assert.assertEquals(0F, floatToSet)
        Assert.assertEquals(0L, longToSet)
        Assert.assertEquals("0", liveDataToSet)
        Assert.assertEquals("0", flowToSet)

        observer.startObserver()

        // Just called observer.startObserver(). Values should change right now
        runBlocking { delay(10) }
        Assert.assertEquals(true, boolToSet)
        Assert.assertEquals(1, intToSet)
        Assert.assertEquals(1.0, doubleToSet, 0.0)
        Assert.assertEquals(1.toShort(), shortToSet)
        Assert.assertEquals(1.toByte(), byteToSet)
        Assert.assertEquals('1', charToSet)
        Assert.assertEquals("1", stringToSet)
        Assert.assertEquals(1F, floatToSet)
        Assert.assertEquals(1L, longToSet)
        Assert.assertEquals("1", liveDataToSet)
        Assert.assertEquals("1", flowToSet)

        boolToObserve.set(false)
        intToObserve.set(2)
        doubleToObserve.set(2.0)
        shortToObserve.set(2.toShort())
        byteToObserve.set(2.toByte())
        charToObserve.set('2')
        stringToObserve.set("2")
        floatToObserve.set(2F)
        longToObserve.set(2L)
        liveDataToObserve.postValue("2")
        runBlocking { flowToObserve.emit("2") }

        // Changed observables: variable values should change
        runBlocking { delay(10) }
        Assert.assertEquals(false, boolToSet)
        Assert.assertEquals(2, intToSet)
        Assert.assertEquals(2.0, doubleToSet, 0.0)
        Assert.assertEquals(2.toShort(), shortToSet)
        Assert.assertEquals(2.toByte(), byteToSet)
        Assert.assertEquals('2', charToSet)
        Assert.assertEquals("2", stringToSet)
        Assert.assertEquals(2F, floatToSet)
        Assert.assertEquals(2L, longToSet)
        Assert.assertEquals("2", liveDataToSet)
        Assert.assertEquals("2", flowToSet)

        boolToObserve.set(true)
        intToObserve.set(3)
        doubleToObserve.set(3.0)
        shortToObserve.set(3.toShort())
        byteToObserve.set(3.toByte())
        charToObserve.set('3')
        stringToObserve.set("3")
        floatToObserve.set(3F)
        longToObserve.set(3L)
        liveDataToObserve.postValue("3")
        runBlocking { flowToObserve.emit("3") }

        // Changed observables: variable values should change
        runBlocking { delay(10) }
        Assert.assertEquals(true, boolToSet)
        Assert.assertEquals(3, intToSet)
        Assert.assertEquals(3.0, doubleToSet, 0.0)
        Assert.assertEquals(3.toShort(), shortToSet)
        Assert.assertEquals(3.toByte(), byteToSet)
        Assert.assertEquals('3', charToSet)
        Assert.assertEquals("3", stringToSet)
        Assert.assertEquals(3F, floatToSet)
        Assert.assertEquals(3L, longToSet)
        Assert.assertEquals("3", liveDataToSet)
        Assert.assertEquals("3", flowToSet)

        observer.stopObserver()

        boolToObserve.set(false)
        intToObserve.set(4)
        doubleToObserve.set(4.0)
        shortToObserve.set(4.toShort())
        byteToObserve.set(4.toByte())
        charToObserve.set('4')
        stringToObserve.set("4")
        floatToObserve.set(4F)
        longToObserve.set(4L)
        liveDataToObserve.postValue("4")
        runBlocking { flowToObserve.emit("4") }

        // Just called observer.stopObserver(). Changed observables: variable values should NOT change
        runBlocking { delay(10) }
        Assert.assertEquals(true, boolToSet)
        Assert.assertEquals(3, intToSet)
        Assert.assertEquals(3.0, doubleToSet, 0.0)
        Assert.assertEquals(3.toShort(), shortToSet)
        Assert.assertEquals(3.toByte(), byteToSet)
        Assert.assertEquals('3', charToSet)
        Assert.assertEquals("3", stringToSet)
        Assert.assertEquals(3F, floatToSet)
        Assert.assertEquals(3L, longToSet)
        Assert.assertEquals("3", liveDataToSet)
        Assert.assertEquals("3", flowToSet)

        observer.destroyObserver()
    }

    /** Observe multiple variables changing at the same time should call the corresponding lambda function only once. */
    @Test
    fun `observe on multiple changes call the function only once`() {
        val observer: SamaObserver = SamaObserverImpl()
        observer.initObserver(this)
        var i = 0
        var j = 0
        var k = 0
        val ld1 = MutableLiveData(0)
        val flow1 = MutableStateFlow(0)
        val var1 = ObservableInt(0)
        val var2 = ObservableField(0)
        val var3 = ObservableField("0")
        observer.startObserver()
        observer.observe(var1, var2, var3) { i += 1 }
        observer.observe(flow1, var2, var3) { j += 1 }
        observer.observe(ld1, var2, var3) { k += 1 }
        runBlocking {
            flow1.emit(1)
            flow1.emit(2)
        }
        ld1.postValue(1)
        ld1.postValue(2)
        var1.set(1)
        var1.set(2)
        var2.set(2)
        var2.set(3)
        var3.set("1")
        var3.set("2")
        // Observing multiple variables happens in background after 50 milliseconds:
        //  should wait a short delay for changes to happen
        runBlocking { delay(100) }
        Thread.sleep(100)
        observer.stopObserver()
        observer.destroyObserver()
        Assert.assertEquals(1, i)
        Assert.assertEquals(1, j)
        Assert.assertEquals(1, k)
    }

    /** Run each tests 10 times to be sure everything works. */
//    @Test todo
    fun observeTestAll() {
        for (i in 0 until 5) {
            println("Test N. $i")
            `observeOnChange works only from startObserve to stopObserver`()
            `observe on multiple changes call the function only once`()
        }
    }
}
