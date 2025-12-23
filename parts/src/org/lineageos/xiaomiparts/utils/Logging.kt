package org.lineageos.xiaomiparts.utils

import android.util.Log

private const val MAIN_TAG = "XMParts"

object Logging {
    fun d(tag: String, msg: String) {
        if (Log.isLoggable(MAIN_TAG, Log.DEBUG) || Log.isLoggable(tag, Log.DEBUG)) {
            Log.d("$MAIN_TAG-$tag", msg)
        }
    }
    
    fun i(tag: String, msg: String) {
        Log.i("$MAIN_TAG-$tag", msg)
    }
    
    fun w(tag: String, msg: String) {
        Log.w("$MAIN_TAG-$tag", msg)
    }
    
    fun w(tag: String, msg: String, tr: Throwable) {
        Log.w("$MAIN_TAG-$tag", msg, tr)
    }
    
    fun e(tag: String, msg: String) {
        Log.e("$MAIN_TAG-$tag", msg)
    }
    
    fun e(tag: String, msg: String, tr: Throwable) {
        Log.e("$MAIN_TAG-$tag", msg, tr)
    }
    
    fun log(tag: String, msg: String) = d(tag, msg)
}
