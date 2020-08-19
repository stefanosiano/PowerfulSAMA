package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.util.SparseArray
import androidx.documentfile.provider.DocumentFile
import com.stefanosiano.powerful_libraries.sama.tryOrPrint
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import kotlinx.coroutines.*
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

object Saf : CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineJob + CoroutineExceptionHandler { _, t -> t.printStackTrace() }

    /** Array of helpers for asking permissions */
    private val safHelperMap = SparseArray<SafHelper>()


    /** Manages the permissions request results. Activities extending [SamaActivity] already call it */
    internal fun onRequestSafResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val safHelper = safHelperMap[requestCode] ?: return false
        if(resultCode != Activity.RESULT_OK) return false
        safHelperMap.remove(requestCode)
        data?.data?.let { uri ->
            val fDf = safHelper.fDf
            if(fDf != null) {
                val df = if(safHelper.isSingleDocument) getDocument(uri, activity) else getDocumentTree(uri, activity)
                df?.let { launch { withContext(Dispatchers.IO) { fDf(it) } }; return true }
            }
            launch { withContext(Dispatchers.IO) {
                val stream = tryOrPrint {
                    if(safHelper.isInput) {
                        if(safHelper.persistableUriPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                            activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        activity.contentResolver.openInputStream(uri)
                    }
                    else {
                        if(safHelper.persistableUriPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                            activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        activity.contentResolver.openOutputStream(uri)
                    }
                }
                stream?.use { safHelper.f!!.invoke(it) }
            } }
        } ?: let { return false }
        return true
    }


    fun releasePersistedUri(uri: Uri) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return
        val persisted = persistedUriPermissions().firstOrNull { it.uri == uri }
            ?: let { "Provided uri ${uri} is not persisted by the app: ignoring release request"; return }
        var flags = 0
        if(persisted.isReadPermission) flags = flags.or(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if(persisted.isWritePermission) flags = flags.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        PowerfulSama.applicationContext.contentResolver.releasePersistableUriPermission(uri, flags)
    }

    fun persistedUriPermissions(): List<UriPermission> = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) PowerfulSama.applicationContext.contentResolver.persistedUriPermissions else emptyList()

    fun getDocument(uri: Uri, activity: Activity? = null): DocumentFile? =
        tryOrPrint { DocumentFile.fromSingleUri(activity ?: PowerfulSama.applicationContext, uri) } ?:
        tryOrPrint { if(uri.scheme == "file" && uri.path != null) DocumentFile.fromFile(File(uri.path)) else null }

    fun getDocumentTree(uri: Uri, activity: Activity? = null): DocumentFile? =
        tryOrPrint { DocumentFile.fromTreeUri(activity ?: PowerfulSama.applicationContext, uri) } ?:
        tryOrPrint { if(uri.scheme == "file" && uri.path != null) DocumentFile.fromFile(File(uri.path)) else null }


    //Ask the user to choose an existing file or content
    // ACTION_OPEN_DOCUMENT)
    fun openDocumentIs(persistableUriPermission: Boolean, mimeType: String = "*/*", f: suspend (inputStream: InputStream?) -> Unit) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType(mimeType)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if(persistableUriPermission) intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper = SafHelper(reqCode, persistableUriPermission, true, true, f as (Closeable?) -> Unit, null)
        safHelperMap.put(reqCode, permHelper)

        PowerfulSama.getCurrentActivity()?.startActivityForResult(intent, reqCode)
    }
    //Ask the user to choose an existing file or content
    // ACTION_OPEN_DOCUMENT)
    fun openDocument(persistableUriPermission: Boolean, mimeType: String = "*/*", f: (documentFile: DocumentFile) -> Unit) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType(mimeType)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if(persistableUriPermission) intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper = SafHelper(reqCode, persistableUriPermission, true, true, null, f)
        safHelperMap.put(reqCode, permHelper)

        PowerfulSama.getCurrentActivity()?.startActivityForResult(intent, reqCode)
    }

    //Ask the user to choose where to place a file or content
    // ACTION_CREATE_DOCUMENT)
    fun createDocumentOs(persistableUriPermission: Boolean, mimeType: String, f: (outputStream: OutputStream?) -> Unit) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType(mimeType)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if(persistableUriPermission) intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper = SafHelper(reqCode, persistableUriPermission, false, true, f as (Closeable?) -> Unit, null)
        safHelperMap.put(reqCode, permHelper)

        PowerfulSama.getCurrentActivity()?.startActivityForResult(intent, reqCode)
    }

    //Ask the user to choose where to place a file or content
    // ACTION_CREATE_DOCUMENT)
    fun createDocument(persistableUriPermission: Boolean, mimeType: String, f: (documentFile: DocumentFile) -> Unit) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType(mimeType)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if(persistableUriPermission) intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper = SafHelper(reqCode, persistableUriPermission, false, true, null, f)
        safHelperMap.put(reqCode, permHelper)

        PowerfulSama.getCurrentActivity()?.startActivityForResult(intent, reqCode)
    }


    fun openDocumentTree(persistableUriPermission: Boolean, f: (documentFile: DocumentFile) -> Unit) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//            .addCategory(Intent.CATEGORY_OPENABLE)
        if(persistableUriPermission) intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper = SafHelper(reqCode, persistableUriPermission, true, false, null, f)
        safHelperMap.put(reqCode, permHelper)

        PowerfulSama.getCurrentActivity()?.startActivityForResult(intent, reqCode)
    }





    private class SafHelper (val reqCode: Int, val persistableUriPermission: Boolean, val isInput: Boolean, val isSingleDocument: Boolean,
                             val f: ((stream: Closeable?) -> Unit)?, val fDf: ((documentFile: DocumentFile) -> Unit)?)
}