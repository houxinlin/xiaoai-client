package com.hxl.xiaoai.startup

class XiaoAi(private val hostName: String, private val port: Int, private val password: String) {
    private val xiaoAiSocket = XiaoAiSocket(hostName, port, password)

    fun start(startClass: Class<*>) {
        xiaoAiSocket.setStartClass(startClass)
        xiaoAiSocket.start()
    }

}