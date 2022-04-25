package com.naurt.flutter_naurt_sdk

import android.os.Handler
import android.os.Looper

import kotlinx.coroutines.*

import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result

internal class MainThreadResult internal constructor(result: MethodChannel.Result) : MethodChannel.Result {
    private val result: MethodChannel.Result
    private val handler: Handler

    override
    fun success(p0: Any?) {
        handler.post( Runnable { result.success(p0) })
    }

    override
    fun error(
        errorCode: String?, errorMessage: String?, errorDetails: Any?
    ) {
        handler.post(
            Runnable { result.error(errorCode, errorMessage, errorDetails) })
    }

    override
    fun notImplemented() {
        handler.post(
            Runnable { result.notImplemented() })
    }

    init {
        this.result = result
        handler = Handler(Looper.getMainLooper())
    }
}