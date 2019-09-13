package com.stefanosiano.powerful_libraries.sama.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.stefanosiano.powerful_libraries.sama.removeWhen
import java.util.concurrent.atomic.AtomicLong


/** Extension of the Intent class. It allows to pass any value using static variables.
 * NOTE: There may be some problem when using its methods with multiple instances of the same activity at the same moment */
class SamaIntent : Intent {

    internal companion object {
        private const val extra = "com.stefanosiano.powerful_libraries.sama.view.SamaIntent.uid"
        private val uids = AtomicLong(0)
        private val variables = HashMap<String, Any>()
        private fun store(key: String, variable: Any?) = variable?.let { variables.put(key, it) }
        @Suppress("UNCHECKED_CAST")
        private fun <T> retrieve(key: String): T? = variables[key] as? T
        private fun removeExtra(key: String) { variables.remove(key) }
        private fun hasExtra(key: String) = variables.containsKey(key)
        internal fun clear(key: String) = variables.removeWhen { it.key.startsWith(key) }
    }

    internal val uid: Long

    constructor() : super() { this.uid = uids.incrementAndGet(); putExtra(extra, uid) }
    constructor(o: Intent?) : super(o) { this.uid = getLongExtra(extra, 0) }
    constructor(action: String?) : super(action) { this.uid = uids.incrementAndGet(); putExtra(extra, uid) }
    constructor(action: String?, uri: Uri?) : super(action, uri) { this.uid = uids.incrementAndGet(); putExtra(extra, uid) }
    constructor(packageContext: Context?, cls: Class<*>) : super(packageContext, cls) { this.uid = uids.incrementAndGet(); putExtra(extra, uid) }
    constructor(action: String?, uri: Uri?, packageContext: Context?, cls: Class<*>) : super(action, uri, packageContext, cls) { this.uid = uids.incrementAndGet(); putExtra(extra, uid) }


    /** Put a static variable associated to [key] */
    fun putExtraStatic(key: String, variable: Any?): SamaIntent { SamaIntent.store("$uid $key", variable); return this }

    /** Retrieve the static variable associated to [key], if it exists, or null */
    fun <T> getExtraStatic(key: String): T? = SamaIntent.retrieve("$uid $key")

    /** Removes the static variable associated to [key], if it exists */
    fun removeExtraStatic(key: String): Unit = SamaIntent.removeExtra("$uid $key")

    /** Check if a static variable associated to [key] exists */
    fun hasExtraStatic(key: String): Boolean = SamaIntent.hasExtra("$uid $key")

}
