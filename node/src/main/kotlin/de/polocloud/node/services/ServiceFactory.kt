package de.polocloud.node.services

import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.node.services.control.ServiceControlPlan
import de.polocloud.node.services.process.ServiceProcess
import org.slf4j.LoggerFactory
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.UUID
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

object ServiceFactory {

    private val logger = LoggerFactory.getLogger(ServiceFactory::class.java)
    private val path = Path("local/services/cache")

    fun scanServices() : List<ServiceHolder> {
        if (!path.exists()) {
            Files.createDirectories(path)
            return listOf()
        }

        val serviceList = mutableListOf<ServiceHolder>()
        val jars = path.listDirectoryEntries("*.jar")

        for (jarPath in jars) {
            val jarFile = JarFile(jarPath.toFile())
            val urls = arrayOf(jarPath.toUri().toURL())

            URLClassLoader(urls, this::class.java.classLoader).use { classLoader ->

                val entries = jarFile.entries()

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.name.endsWith(".class")) continue
                    val className = entry.name.replace("/", ".")
                        .removeSuffix(".class")

                    try {
                        val clazz = classLoader.loadClass(className)

                        serviceList.add(ServiceHolder("test", "1.0.0", file = jarPath.toFile()))
                    } catch (_: Throwable) { }
                }
            }
        }
        return serviceList
    }

    fun bootService(plan : ServiceControlPlan) {
        val serviceProcess = ServiceProcess(UUID.randomUUID(), plan.name, NodeEnvironment.runtime.nodeId.get(),-1, -1, ServiceState.LOADING)
        val container = ServiceContainer(1, serviceProcess)
        val processBuilder = ProcessBuilder()

        logger.info("Starting service " + container.name() + " with plan " + plan.name + "...")

        serviceProcess.changeState(ServiceState.BOOTING)
        val process = processBuilder.command("java", "-jar", ).directory(container.path().toFile()).start()
        serviceProcess.withRuntime(process)
    }
}