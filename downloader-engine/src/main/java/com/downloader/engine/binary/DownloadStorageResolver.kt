package com.downloader.engine.binary

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

class DownloadStorageResolver(private val context: Context) {

    fun resolveOutputDirectory(downloadFolderUri: String?): String {
        if (downloadFolderUri.isNullOrBlank()) {
            return defaultDownloadDirectory().absolutePath
        }

        val uri = runCatching { Uri.parse(downloadFolderUri) }.getOrNull()
            ?: return defaultDownloadDirectory().absolutePath

        if (!DocumentsContract.isTreeUri(uri)) {
            return defaultDownloadDirectory().absolutePath
        }

        val treeId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return defaultDownloadDirectory().absolutePath

        val separatorIndex = treeId.indexOf(':')
        if (separatorIndex < 0) {
            return defaultDownloadDirectory().absolutePath
        }

        val rootType = treeId.substring(0, separatorIndex)
        val relativePath = treeId.substring(separatorIndex + 1)

        if (rootType != "primary") {
            return defaultDownloadDirectory().absolutePath
        }

        val basePath = Environment.getExternalStorageDirectory().absolutePath
        return if (relativePath.isBlank()) {
            basePath
        } else {
            File(basePath, relativePath).absolutePath
        }
    }

    private fun defaultDownloadDirectory(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }
}
