package com.hxl.xiaoai.startup

import com.hxl.xiaoai.msg.Message
import com.hxl.xiaoai.msg.MessageType

class MessageHandler(private val xiaoAiHeartbeat: XiaoAiHeartbeat) {
    private lateinit var actionScan: ActionScan

    companion object {
        private const val DEFAULT_REP = "好的"
        private const val NO_HANDLE_METHOD = "没有处理这条语音的方法"
    }

    fun handlerMessage(message: Message): String {
        message?.run {
            if (message.messageType == MessageType.VOICE.typeValue) {
                return invoke(message.msg)
            }
            //心跳回复
            if (message.messageType == MessageType.HEARTBEAT_REP.typeValue) {
                xiaoAiHeartbeat.resetTimer()
            }
        }
        return DEFAULT_REP
    }

    fun invoke(voice: String): String {
        val actionItem = actionScan.getMethod(voice)
        if (actionItem == null) {
            var result = NO_HANDLE_METHOD
            actionScan.getDefaultAction()?.forEach { it ->
                result = it.invoke(voice) as String
            }
            return result
        }
        if (actionItem!!.method.returnType == Void.TYPE) {
            actionItem.invoke()
            return DEFAULT_REP
        }
        actionItem.invoke()?.run { return this.toString() }
        return DEFAULT_REP

    }

    fun setStartClass(startClass: Class<*>) {
        actionScan = ActionScan(startClass).apply { this.scan() }

    }
}