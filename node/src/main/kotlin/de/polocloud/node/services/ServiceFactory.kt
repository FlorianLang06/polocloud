package de.polocloud.node.services

import java.net.URLClassLoader
import java.nio.file.Files
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

object ServiceFactory {

    private val path = Path("local/services/cache")

    fun scanServices() : List<Service> {
        if (!path.exists()) {
            Files.createDirectories(path)
            return listOf()
        }

        val serviceList = mutableListOf<Service>()
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

                        if (Service::class.java.isAssignableFrom(clazz) && clazz != Service::class.java) {
                            println("Gefundener Service: ${clazz.name}")
                            //todo register service here
                        }

                    } catch (_: Throwable) { }
                }
            }
        }
        return serviceList
    }
}