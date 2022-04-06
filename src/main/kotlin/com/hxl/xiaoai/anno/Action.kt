package com.hxl.xiaoai.anno

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Action(
    val name: String = ""
)