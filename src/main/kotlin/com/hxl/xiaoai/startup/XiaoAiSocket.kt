package com.hxl.xiaoai.startup

import com.hxl.xiaoai.msg.MessageEncode
import com.hxl.xiaoai.msg.MessageType
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.util.concurrent.RunnableScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.reflect.KFunction0

class XiaoAiSocket(private val hostName: String, private val port: Int, private val password: String) :
    XiaoAiHeartbeat.HeartbeatCallback {
    private var serverSocket: SocketChannel? = null
    private val scheduledExecutorService = ScheduledThreadPoolExecutor(2)
    private var messageEncode: MessageEncode? = null;
    private var log: Logger = Logger.getLogger(XiaoAiSocket::class.java.name)
    private val connectToServerTask = { connectToServer() }
    private var connectionScheduledFuture: RunnableScheduledFuture<*>? = null

    //心跳
    private val xiaoAiHeartbeat = XiaoAiHeartbeat(this)

    //消息处理器
    private val messageHandler = MessageHandler(xiaoAiHeartbeat)

    @Volatile
    private var doScheduled: Boolean = false

    private var isStart = false

    init {

    }

    fun stop() {
        connectionScheduledFuture?.run { cancelSchedule(this) }
        xiaoAiHeartbeat.stop()
        isStart = false
    }

    @Synchronized
    fun start() {
        if (isStart) {
            return
        }
        isStart = true
        startSchedule()
    }

    fun setStartClass(startClass: Class<*>) {
        messageHandler.setStartClass(startClass)
    }

    /**
     * 连接超时
     */
    override fun timeout() {
        log.info("长时间未收到心跳恢复，尝试重新连接")
        startSchedule()
    }

    private fun startSchedule(init: Long = 0) {
        xiaoAiHeartbeat.stop()
        if (doScheduled) {
            return
        }
        doScheduled = true
        connectionScheduledFuture =
            scheduledExecutorService.scheduleAtFixedRate(
                connectToServerTask,
                init,
                5,
                TimeUnit.SECONDS
            ) as RunnableScheduledFuture<*>?
    }


    @Synchronized
    private fun connectToServer() {
        serverSocket?.run { this.close() }
        log.info("正在尝试连接服务器 ${hostName}:${port}")
        try {
            serverSocket = SocketChannel.open(InetSocketAddress(hostName, port))
            connectSuccess()
        } catch (e: Exception) {
            log.warning(e.message)
        }
    }

    private fun connectSuccess() {
        sendPassword()
        log.info("服务器连接成功")
        connectionScheduledFuture?.run(this@XiaoAiSocket::cancelSchedule)
        xiaoAiHeartbeat.resetTimer()
        //开始心跳检测
        xiaoAiHeartbeat.start()
        doScheduled = false
        messageEncode = MessageEncode(serverSocket!!)
        createThread(this::beginReadVoiceFromServer)
    }

    private fun sendPassword() {
        serverSocket?.write(Charsets.UTF_8.encode(password))
    }

    private fun cancelSchedule(runnableScheduledFuture: RunnableScheduledFuture<*>) {
        runnableScheduledFuture.cancel(true)
        scheduledExecutorService.remove(runnableScheduledFuture)
    }

    private fun createThread(function: KFunction0<Unit>) {
        Thread(function).start()
    }

    private fun beginReadVoiceFromServer() {
        try {
            while (true) {
                val message = messageEncode!!.getMessageFromSocket()

                message?.run {
                    val result = messageHandler.handlerMessage(message)
                    if (this.messageType == MessageType.VOICE.typeValue) {
                        writeResult(result!!)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            log.warning(e.localizedMessage)
            startSchedule(5)
        }
    }


    private fun writeResult(msg: String) {
        serverSocket?.run { this.write(messageEncode!!.createVoiceMessageByteBuffer(msg)) }
    }


}