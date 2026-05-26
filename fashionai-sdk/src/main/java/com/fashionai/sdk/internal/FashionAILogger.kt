package com.fashionai.sdk.internal

import android.util.Log
import com.fashionai.sdk.model.FashionAILogLevel

internal object FashionAILogger {
    private const val TAG = "FashionAI"
    internal var level: FashionAILogLevel = FashionAILogLevel.WARN

    fun v(msg: String) { if (level >= FashionAILogLevel.VERBOSE) Log.v(TAG, msg) }
    fun d(msg: String) { if (level >= FashionAILogLevel.DEBUG) Log.d(TAG, msg) }
    fun i(msg: String) { if (level >= FashionAILogLevel.INFO) Log.i(TAG, msg) }
    fun w(msg: String) { if (level >= FashionAILogLevel.WARN) Log.w(TAG, msg) }
    fun e(msg: String, t: Throwable? = null) {
        if (level >= FashionAILogLevel.ERROR) {
            if (t != null) Log.e(TAG, msg, t) else Log.e(TAG, msg)
        }
    }
}
