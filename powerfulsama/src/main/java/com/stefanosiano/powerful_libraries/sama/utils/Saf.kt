package com.stefanosiano.powerful_libraries.sama.utils

import android.app.Activity
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.util.SparseArray
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.stefanosiano.powerful_libraries.sama.logDebug
import com.stefanosiano.powerful_libraries.sama.tryOrPrint
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** Utility class to simplify the usage of Storage Access Framework. */
object Saf : CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext =
        coroutineJob + CoroutineExceptionHandler { _, t -> t.printStackTrace() }

    /** Array of helpers for managing saf results. */
    private val safHelperMap = SparseArray<SafHelper>()

    /** Manages the Saf request results. Activities extending [SamaActivity] already call it. */
    @Suppress("ReturnCount")
    internal fun onRequestSafResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        val safHelper = safHelperMap[requestCode]
        val uri = data?.data
        if (safHelper == null || resultCode != Activity.RESULT_OK || uri == null) {
            return false
        }
        safHelperMap.remove(requestCode)
        takePersistableUriPermission(safHelper, activity, uri)
        val fDf = safHelper.fDf
        if (fDf != null) {
            val df = if (safHelper.isSingleDocument) {
                getDocument(uri, activity)
            } else {
                getDocumentTree(uri, activity)
            }
            df?.let {
                launch {
                    withContext(Dispatchers.IO) { fDf(it) }
                }
                return true
            }
        }
        launch {
            withContext(Dispatchers.IO) {
                val stream = tryOrPrint {
                    if (safHelper.isInput) {
                        activity.contentResolver.openInputStream(uri)
                    } else {
                        activity.contentResolver.openOutputStream(uri)
                    }
                }
                stream?.use { safHelper.f!!.invoke(it) }
            }
        }
        return true
    }

    /**
     * If persisted permission is required:
     *  for single documents we ask read permission, while for folders we need read/write permissions.
     */
    private fun takePersistableUriPermission(safHelper: SafHelper, activity: Activity, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
            safHelper.persistableUriPermission
        ) {
            when {
                safHelper.isSingleDocument && safHelper.isInput ->
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                safHelper.isSingleDocument && !safHelper.isInput -> {
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                else -> {
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
        }
    }

    /** Release persisted uris held by this app. */
    fun releasePersistedUri(uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return
        val persisted = persistedUriPermissions().firstOrNull { it.uri == uri }
            ?: let {
                logDebug("Provided uri $uri is not persisted by the app: ignoring release request")
                return
            }
        var flags = 0
        if (persisted.isReadPermission) {
            flags = flags.or(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (persisted.isWritePermission) {
            flags = flags.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        PowerfulSama.applicationContext.contentResolver.releasePersistableUriPermission(uri, flags)
    }

    /** Lists persisted uris held by this app. */
    fun persistedUriPermissions(): List<UriPermission> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            PowerfulSama.applicationContext.contentResolver.persistedUriPermissions
        } else {
            emptyList()
        }

    /** Returns a [DocumentFile] from passed [uri]. */
    fun getDocument(uri: Uri, activity: Activity? = null): DocumentFile? =
        tryOrPrint {
            DocumentFile.fromSingleUri(activity ?: PowerfulSama.applicationContext, uri)
        } ?: tryOrPrint {
            if (uri.scheme == "file" && uri.path != null) {
                DocumentFile.fromFile(File(uri.path))
            } else {
                null
            }
        }

    /** Returns a [DocumentFile]Tree from passed [uri]. */
    fun getDocumentTree(uri: Uri, activity: Activity? = null): DocumentFile? =
        tryOrPrint {
            DocumentFile.fromTreeUri(activity ?: PowerfulSama.applicationContext, uri)
        } ?: tryOrPrint {
            if (uri.scheme == "file" && uri.path != null) {
                DocumentFile.fromFile(File(uri.path))
            } else {
                null
            }
        }

    /**
     * Ask the user to choose an existing file or content,
     *  optionally ask persistent access through [persistableUriPermission].
     * Call [f] with an input stream of the content selected by the user and then closes it down.
     */
    @Suppress("UNCHECKED_CAST")
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun openDocumentIs(
        persistableUriPermission: Boolean,
        mimeType: String = "*/*",
        f: suspend (inputStream: InputStream?) -> Unit
    ) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType(mimeType)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (persistableUriPermission) {
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper = SafHelper(
            reqCode,
            persistableUriPermission,
            true,
            true,
            f as (Closeable?) -> Unit,
            null
        )
        safHelperMap.put(reqCode, permHelper)

        PowerfulSama.getCurrentActivity()?.startActivityForResult(intent, reqCode)
    }

    /**
     * Ask the user to choose an existing file or content,
     *  optionally ask persistent access through [persistableUriPermission].
     * Call [f] with the content selected by the user as DocumentFile.
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun openDocument(
        persistableUriPermission: Boolean,
        mimeType: String = "*/*",
        f: (documentFile: DocumentFile) -> Unit
    ) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType(mimeType)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (persistableUriPermission) {
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper = SafHelper(reqCode, persistableUriPermission, true, true, null, f)
        safHelperMap.put(reqCode, permHelper)

        PowerfulSama.getCurrentActivity()?.startActivityForResult(intent, reqCode)
    }

    /**
     * Ask the user to choose where to place a file or content,
     *  optionally ask persistent access through [persistableUriPermission].
     * Call [f] with an output stream of the content selected by the user and then closes it down.
     */
    @Suppress("UNCHECKED_CAST")
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun createDocumentOs(
        persistableUriPermission: Boolean,
        mimeType: String,
        f: (outputStream: OutputStream?) -> Unit
    ) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType(mimeType)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (persistableUriPermission) intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper = SafHelper(
            reqCode,
            persistableUriPermission,
            false,
            true,
            f as (Closeable?) -> Unit,
            null
        )
        safHelperMap.put(reqCode, permHelper)

        PowerfulSama.getCurrentActivity()?.startActivityForResult(intent, reqCode)
    }

    /**
     * Ask the user to choose where to place a file or content,
     *  optionally ask persistent access through [persistableUriPermission].
     * Call [f] with the content selected by the user as DocumentFile.
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun createDocument(persistableUriPermission: Boolean, mimeType: String, f: (documentFile: DocumentFile) -> Unit) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType(mimeType)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (persistableUriPermission) intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper = SafHelper(reqCode, persistableUriPermission, false, true, null, f)
        safHelperMap.put(reqCode, permHelper)

        PowerfulSama.getCurrentActivity()?.startActivityForResult(intent, reqCode)
    }

    /**
     * Ask the user to choose a directory where to place one or more files or contents,
     *  optionally ask persistent access through [persistableUriPermission].
     * Call [f] with the content selected by the user as DocumentFileTree.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun openDocumentTree(
        persistableUriPermission: Boolean,
        f: (documentFile: DocumentFile) -> Unit
    ) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//            .addCategory(Intent.CATEGORY_OPENABLE)
        if (persistableUriPermission) intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

        val reqCode = SamaActivity.samaRequestCodes.incrementAndGet()
        val permHelper = SafHelper(reqCode, persistableUriPermission, false, false, null, f)
        safHelperMap.put(reqCode, permHelper)

        PowerfulSama.getCurrentActivity()?.startActivityForResult(intent, reqCode)
    }

    /** Internal class used to manage Saf request results. */
    private class SafHelper(
        val reqCode: Int,
        val persistableUriPermission: Boolean,
        val isInput: Boolean,
        val isSingleDocument: Boolean,
        val f: ((stream: Closeable?) -> Unit)?,
        val fDf: ((documentFile: DocumentFile) -> Unit)?
    )
}
