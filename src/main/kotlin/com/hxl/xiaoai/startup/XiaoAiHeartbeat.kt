package com.hxl.xiaoai.startup

import java.util.concurrent.RunnableScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class XiaoAiHeartbeat(private val heartbeatCallback: HeartbeatCallback) {
    companion object {
        private var log: Logger = Logger.getLogger(XiaoAiSocket::class.java.name)
        private const val HEARTBEAT_SEND_INTERVAL: Long = 10 * 1000
    }

    private var lastHeartbeatTime: Long = 0L
    private var heartbeatScheduledFuture: RunnableScheduledFuture<*>? = null
    private val scheduledExecutorService = ScheduledThreadPoolExecutor(1)


    fun start() {
        stop()
        heartbeatScheduledFuture = scheduledExecutorService.scheduleAtFixedRate({
            if (System.currentTimeMillis() - lastHeartbeatTime > HEARTBEAT_SEND_INTERVAL) {
                heartbeatCallback?.timeout()
                stop()
            }
        }, 0, HEARTBEAT_SEND_INTERVAL, TimeUnit.MILLISECONDS) as RunnableScheduledFuture<*>?
    }

    fun resetTimer() {
        lastHeartbeatTime = System.currentTimeMillis()
    }

    fun stop() {
        heartbeatScheduledFuture?.cancel(true)
        scheduledExecutorService.remove(heartbeatScheduledFuture)
    }

    interface HeartbeatCallback {
        fun timeout()
    }
}