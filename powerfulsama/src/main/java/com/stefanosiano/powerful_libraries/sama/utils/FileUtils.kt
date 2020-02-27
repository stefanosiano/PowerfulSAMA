package com.stefanosiano.powerful_libraries.sama.utils

import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.stefanosiano.powerful_libraries.sama.logDebug
import com.stefanosiano.powerful_libraries.sama.logError
import java.io.File

object FileUtils {

    /** Return the file from the uri using Android providers (if needed) or using uri's path as path. If scheme is not recognized, null is returned */
    fun Uri.toFileFromProviders(context: Context): File? {
        logDebug("File Uri: $this, with scheme: $scheme")
        val path = when {
            // MediaStore (and general)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, this) -> getForApi19(context, this)
            //isGooglePhotosUri
            "content".equals(scheme, ignoreCase = true) &&  authority == "com.google.android.apps.photos.content" -> lastPathSegment
            // Return the remote address
            "content".equals(scheme, ignoreCase = true) -> getDataColumn(context, this, null, null)
            "file".equals(scheme, ignoreCase = true) -> path
            else -> { logError("Uri's scheme unknown: $scheme"); null }
        }
        return path?.let { File(it) }
    }

    @TargetApi(19)
    private fun getForApi19(context: Context, uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri)
        logDebug("Document URI")
        return when (uri.authority) {
            //isExternalStorageDocument
            "com.android.externalstorage.documents" -> {
                logDebug("External Document URI")
                val docTypePath = docId.split(":").toTypedArray()
                val type = docTypePath[0]
                // TODO handle non-primary volumes
                if ("primary".equals(type, ignoreCase = true)) Environment.getExternalStorageDirectory().toString() + "/" + docTypePath[1] else null
            }
            //isDownloadsDocument
            "com.android.providers.downloads.documents" -> {
                logDebug("Downloads External Document URI")
                val contentUri: Uri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), docId.toLongOrNull() ?: -1)
                getDataColumn(context, contentUri, null, null)
            }
            //isMediaDocument -> {
            "com.android.providers.media.documents" -> {
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                logDebug("Media Document URI with uri type: $type")
                val contentUri: Uri = when (type) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> null
                } ?: return null
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                getDataColumn(context, contentUri, selection, selectionArgs)
            }
            else -> null
        }
    }


    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param uri The Uri to query.
     * @param sel (Optional) Filter used in the query.
     * @param args (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private fun getDataColumn(context: Context, uri: Uri, sel: String?, args: Array<String>?): String? {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, arrayOf("_data"), sel, args, null)
            return if (cursor?.moveToFirst() == true) cursor.getString(cursor.getColumnIndexOrThrow("_data"))
            else null
        }
        finally { cursor?.close() }
    }
}