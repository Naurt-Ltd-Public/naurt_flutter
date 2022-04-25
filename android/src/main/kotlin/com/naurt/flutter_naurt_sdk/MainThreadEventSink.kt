package com.naurt.flutter_naurt_sdk

import android.os.Handler
import android.os.Looper

import kotlinx.coroutines.*

import io.flutter.plugin.common.EventChannel

internal class MainThreadEventSink internal constructor(eventSink: EventChannel.EventSink) : EventChannel.EventSink {
    private val eventSink: EventChannel.EventSink
    private val handler: Handler

    override
    fun success(p0: Any?) {
        handler.post(Runnable { eventSink.success(p0) })
    }

    override
    fun error(s: String?, s1: String?, o: Any?) {
        handler.post(Runnable { eventSink.error(s, s1, o) })
    }

    override
    fun endOfStream() {
        handler.post(Runnable { eventSink.endOfStream() })
    }

    init {
        this.eventSink = eventSink
        handler = Handler(Looper.getMainLooper())
    }
}