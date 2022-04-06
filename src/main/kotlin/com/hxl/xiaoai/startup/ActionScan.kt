package com.hxl.xiaoai.startup

import com.hxl.xiaoai.anno.Action
import com.hxl.xiaoai.anno.ActionDefault
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.lang.reflect.Method
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.JarFile
import java.util.logging.Logger

class ActionScan(private val startClass: Class<*>) {
    private val actionMethodMap: MutableMap<String, ActionItem> = mutableMapOf()

    companion object {
        private val log = Logger.getLogger(ActionScan::class.java.name)
    }

    private val defaultAction: MutableList<ActionItem> = mutableListOf()
    private fun getApplicationHome(): String {
        return findDefaultHomeDir()
    }

    private fun isJar(location: String): Boolean {
        return URL(location).openConnection() is JarURLConnection
    }


    private fun scanClassFromJar(result: MutableList<Class<*>>) {
        val applicationHome = getApplicationHome()
        val jarFile = JarFile(applicationHome.substring(applicationHome.indexOf("/"), applicationHome.indexOf("!/")))
        jarFile.stream().forEach {
            if (it.name.startsWith(getStartClassPackage()) && (!it.isDirectory)) {
                addIfActionClass(jarFile.getInputStream(it).readBytes(), result)
            }
        }
    }

    private fun getStartClassPackage(): String {
        return startClass.`package`.name.replace(".", "/")
    }

    private fun scanClassFromLocation(result: MutableList<Class<*>>) {
        val rootPath = URL(getApplicationHome())
        val targetDir = File(rootPath.file, "")
        Files.walkFileTree(Paths.get(targetDir.absolutePath), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                addIfActionClass(Files.readAllBytes(file), result)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun addIfActionClass(bytez: ByteArray, result: MutableList<Class<*>>) {
        getNode(bytez)?.run {
            if (isActionClass(this)) {
                result.add(Class.forName(this.name.replace("/", ".")))
            }
        }
    }

    private fun findDefaultHomeDir(): String {
        val userDir = startClass.getResource("")
        return userDir.toString()
    }

    private fun getScanMethod(): (MutableList<Class<*>>) -> Unit {
        val rootHome = getApplicationHome()
        log.info("root路径$rootHome")
        if (isJar(rootHome)) {
            return this::scanClassFromJar
        }
        return this::scanClassFromLocation
    }

    fun scan() {
        val actionClassList = mutableListOf<Class<*>>()
        getScanMethod()(actionClassList)
        log.info("共有${actionClassList.size}个Action类")
        actionClassList.forEach(this::loadActionMethod)
    }

    fun getMethod(actionName: String): ActionItem? {
        return actionMethodMap[actionName]
    }

    fun getDefaultAction(): List<ActionItem>? {
        return defaultAction
    }

    private fun putAction(actionName: String, actionItem: ActionItem) {
        actionMethodMap[actionName] = actionItem
    }

    private fun loadActionMethod(classNode: Class<*>) {
        log.info("扫描从${classNode.name}")
        var declaredMethods = classNode.declaredMethods
        declaredMethods.forEach { method ->
            val actions = method.getDeclaredAnnotationsByType(Action::class.java)
            var defaultAction = method.getDeclaredAnnotationsByType(ActionDefault::class.java)
            actions.forEach { loadAction(it, classNode, method) }
            defaultAction.forEach { loadDefaultAction(it, classNode, method) }
        }
    }

    private fun loadDefaultAction(actionDefault: ActionDefault, clazz: Class<*>, method: Method) {
        if (method.parameterCount == 1 && method.parameterTypes[0] == String::class.java && method.returnType === String::class.java) {
            defaultAction.add(ActionItem(instance(clazz), method))
            return
        }
        log.info("方法[${method.declaringClass}${method.name}]需要一个参数，并且为String类型,且返回值为String")
    }

    private fun loadAction(action: Action, clazz: Class<*>, method: Method) {
        putAction(action.name, ActionItem(instance(clazz), method))
    }

    private fun instance(clazz: Class<*>): Any {
        return clazz.newInstance()
    }

    private fun isActionClass(classNode: ClassNode): Boolean {
        try {
            var visibleAnnotations = classNode.visibleAnnotations
            for (visibleAnnotation in visibleAnnotations) {
                if (visibleAnnotation.desc == "Lcom/hxl/xiaoai/anno/XiaoAiAction;") {
                    return true
                }
            }
        } catch (e: Exception) {
        }
        return false
    }


    private fun getNode(bytez: ByteArray): ClassNode? {
        val classReader: ClassReader = ClassReader(bytez)
        val cn = ClassNode()
        try {
            classReader!!.accept(cn, ClassReader.EXPAND_FRAMES)
        } catch (e: Exception) {
            try {
                classReader!!.accept(cn, ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
            } catch (e2: Exception) {
            }
        }
        return cn
    }

    class ActionItem(private val instance: Any, val method: Method) {
        fun invoke(): Any? {
            return method.invoke(instance)
        }

        fun invoke(arg: Any): Any? {
            return method.invoke(instance, arg)
        }
    }

}