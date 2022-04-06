package com.hxl.xiaoai.msg

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class MessageEncode(private val socketChannel: SocketChannel) {

    private fun createVoiceMessage(msg: String): Message {
        return Message().apply {
            this.msg = msg
            this.msgSize = msg.toByteArray().size
            this.timer = System.currentTimeMillis()
            this.messageType = MessageType.VOICE.typeValue
        }
    }

    fun createVoiceMessageByteBuffer(msg: String): ByteBuffer {
        var message = createVoiceMessage(msg)
        var byteBuffer = ByteBuffer.allocate(1 + 4 + 8 + msg.toByteArray().size)
        byteBuffer.put(message.messageType)
        byteBuffer.putInt(message.msgSize)
        byteBuffer.put(msg.toByteArray())
        byteBuffer.putLong(message.timer)
        byteBuffer.flip()
        return byteBuffer
    }

    private fun createHeartbeatMessage(): Message {
        return Message().apply {
            this.timer = System.currentTimeMillis()
            this.messageType = MessageType.HEARTBEAT.typeValue
        }
    }

    private fun createHeartbeatRepMessage(): Message {
        return Message().apply {
            this.timer = System.currentTimeMillis()
            this.messageType = MessageType.HEARTBEAT_REP.typeValue
        }
    }

    fun getMessageFromSocket(): Message? {
        var typeValue = readMessageType()

        if (typeValue == MessageType.HEARTBEAT.typeValue) {
            return createHeartbeatMessage()
        }
        if (typeValue == MessageType.VOICE.typeValue) {
            return readMessageContent()
        }
        if (typeValue == MessageType.HEARTBEAT_REP.typeValue) {
            return createHeartbeatRepMessage()
        }
        return null
    }

    private fun readMessageContent(): Message {
        val messageSize = readMessageSize()
        val message = readMessage(messageSize)
        val time = readMessageTime()
        return Message().apply {
            this.msg = String(message.array(), 0, messageSize)
            this.msgSize = messageSize
            this.timer = time
            this.messageType = MessageType.VOICE.typeValue
        }

    }

    private fun readMessageTime(): Long {
        return readMessage(8).getLong(0)
    }

    private fun readMessageSize(): Int {
        return readMessage(4).getInt(0)
    }


    private fun readMessageType(): Byte {

        return readMessage(1).get()
    }

    private fun readMessage(size: Int): ByteBuffer {
        val typeByteBuffer = ByteBuffer.allocate(size)
        if (socketChannel.read(typeByteBuffer) < 0) {
            throw IOException("socket断开")
        }
        typeByteBuffer.flip()
        return typeByteBuffer
    }
}
