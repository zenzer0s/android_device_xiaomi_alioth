package org.lineageos.xiaomiparts.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "FUtils"

suspend fun readOneLine(fileName: String): String? = withContext(Dispatchers.IO) {
    runCatching { File(fileName).useLines { it.firstOrNull() } }
        .onFailure { e -> Logging.e(TAG, "Could not read from file $fileName", e) }
        .getOrNull()
}

suspend fun writeLine(fileName: String, value: String): Boolean = withContext(Dispatchers.IO) {
    runCatching { File(fileName).writeText(value) }
        .onFailure { e -> Logging.e(TAG, "Could not write to file $fileName", e) }
        .isSuccess
}

suspend fun fileExists(fileName: String): Boolean = withContext(Dispatchers.IO) {
    File(fileName).exists()
}

suspend fun isFileReadable(fileName: String): Boolean = withContext(Dispatchers.IO) {
    val file = File(fileName)
    file.exists() && file.canRead()
}

suspend fun isFileWritable(fileName: String): Boolean = withContext(Dispatchers.IO) {
    val file = File(fileName)
    file.exists() && file.canWrite()
}

suspend fun delete(fileName: String): Boolean = withContext(Dispatchers.IO) {
    runCatching { File(fileName).delete() }
        .onFailure { e -> Logging.w(TAG, "Failed to delete $fileName", e) }
        .getOrDefault(false)
}

suspend fun rename(srcPath: String, dstPath: String): Boolean = withContext(Dispatchers.IO) {
    runCatching { File(srcPath).renameTo(File(dstPath)) }
        .onFailure { e -> Logging.w(TAG, "Failed to rename $srcPath to $dstPath", e) }
        .getOrDefault(false)
}

suspend fun getFileValueAsBoolean(filename: String, defValue: Boolean): Boolean {
    return when (readOneLine(filename)) {
        "0" -> false
        null -> defValue
        else -> true
    }
}

suspend fun getFileValue(filename: String, defValue: String): String {
    return readOneLine(filename) ?: defValue
}