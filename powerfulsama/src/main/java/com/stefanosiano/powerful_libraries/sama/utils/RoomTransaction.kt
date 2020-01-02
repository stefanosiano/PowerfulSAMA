package com.stefanosiano.powerful_libraries.sama.utils

import androidx.room.RoomDatabase
import com.stefanosiano.powerful_libraries.sama.delayUntil
import kotlinx.coroutines.*

/** Utility class used to run a Room transaction with coroutines */
class RoomTransaction(
    private val db: RoomDatabase,
    private var onSuccess: () -> Unit,
    private var onError: (t: Throwable) ->  Unit
) : CoroutineScope {

    private var success: Boolean? = null

    override val coroutineContext = SupervisorJob() + CoroutineExceptionHandler { _, t -> success = false; onError.invoke(t) }

    companion object {

        @Synchronized
        /** Run a transaction using a coroutine */
        suspend fun run(db: RoomDatabase, f: suspend () -> Unit): Boolean {
            var success: Boolean? = null
            val transaction = RoomTransaction(db, { success = true }, { success = false })
            transaction.runTransaction { f() }
            while(success == null) delay(200)
            return success!!
        }

    }

    private fun runTransaction(f: suspend () -> Unit): RoomTransaction {
        launch {
            db.runInTransaction { launch { f(); success = true } }
            delayUntil { success != null }
            if(success == true)
                onSuccess()
        }
        return this
    }

}
