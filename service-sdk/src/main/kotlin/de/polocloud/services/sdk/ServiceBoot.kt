package de.polocloud.services.sdk

import de.polocloud.common.Address
import de.polocloud.common.communication.tls.MtlsConfig
import de.polocloud.services.sdk.communication.NodeConnection
import de.polocloud.services.sdk.communication.registration.ServiceRegistrationClient
import de.polocloud.services.sdk.communication.security.ServiceCertificateStorage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class ServiceBoot

private val logger: Logger = LogManager.getLogger(ServiceBoot::class.java)

/**
 * Entry point for every service instance JVM process.
 *
 * Boot sequence:
 * 1. Load / generate RSA key pair via [ServiceCertificateStorage]
 * 2. Register with the node — send CSR, receive signed cert + CA cert
 * 3. Open mTLS [NodeConnection] to the node's main gRPC port
 * 4. Install a shutdown hook that closes the channel cleanly
 *
 * All configuration arrives via system properties set by
 * [de.polocloud.node.services.ServiceFactory]:
 *
 * | Property                    | Example value                            |
 * |-----------------------------|------------------------------------------|
 * | `service.id`                | `550e8400-…`                             |
 * | `service.name`              | `event-service-1`                        |
 * | `service.token`             | `<random 60s single-use token>`          |
 * | `service.identity.dir`      | `/…/instances/event-service-1/.identity` |
 * | `node.address`              | `127.0.0.1:4239`                         |
 * | `node.registration.address` | `127.0.0.1:4240`                         |
 */
fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        logger.error("Fatal service bootstrap error in thread '${thread.name}'", throwable)
    }

    System.setProperty("PID", ProcessHandle.current().pid().toString())

    val serviceId = System.getProperty("service.id")
    val serviceName = System.getProperty("service.name")

    logger.info("Starting service: $serviceName (id: $serviceId)")

    val storage = ServiceCertificateStorage.fromSystemProperties()
    storage.initialize()

    val registrationAddress = parseAddress("node.registration.address")
    val token = prop("service.token")

    runCatching {
        ServiceRegistrationClient(storage).register(registrationAddress, serviceId, token)
    }.onFailure { ex ->
        logger.error("Registration failed for service '{}' — aborting boot", serviceName, ex)
        return
    }

    val nodeAddress = parseAddress("node.address")
    val nodeConnection = NodeConnection(
        nodeAddress,
        MtlsConfig.mutual(
            cert   = storage.certificateFile(),
            key    = storage.privateKeyFile(),
            caCert = storage.caCertificateFile(),
        ),
    )

    logger.info("mTLS channel to node established ({})", nodeAddress)

    // Keep the process alive
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down service '$serviceName'")
        nodeConnection.close()
    })

    bootServices()

    while (true) {
        Thread.sleep(1000)
    }
}

private fun bootServices() {
    val classLoader = Thread.currentThread().contextClassLoader

    val serviceClasses = classLoader.getResources("").asSequence()
        .flatMap { url ->
            val root = java.io.File(url.toURI())
            root.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".class") }
                .map { file ->
                    file.relativeTo(root).path
                        .removeSuffix(".class")
                        .replace(java.io.File.separatorChar, '.')
                }
        }
        .mapNotNull { className ->
            runCatching { Class.forName(className, false, classLoader) }.getOrNull()
        }
        .filter { clazz ->
            !clazz.isInterface && !clazz.isAnnotation &&
                Service::class.java.isAssignableFrom(clazz) &&
                clazz != Service::class.java
        }
        .toList()

    serviceClasses.forEach { clazz ->
        val instance = runCatching { clazz.getDeclaredConstructor().newInstance() as Service }
            .onFailure { logger.error("Failed to instantiate service class: ${clazz.name}", it) }
            .getOrNull() ?: return@forEach

        clazz.methods
            .filter { it.isAnnotationPresent(BootMethod::class.java) }
            .sortedByDescending { it.getAnnotation(BootMethod::class.java).priority }
            .forEach { method ->
                runCatching { method.invoke(instance) }
                    .onFailure { logger.error("Failed to invoke @BootMethod ${method.name} on ${clazz.name}", it) }
            }
    }
}

private fun prop(name: String): String =
    System.getProperty(name) ?: error("Required system property '$name' is not set")

private fun parseAddress(propertyName: String): Address {
    val raw   = prop(propertyName)
    val parts = raw.split(":")
    check(parts.size == 2) {
        "Invalid address format in '$propertyName': expected 'host:port', got '$raw'"
    }
    return Address(parts[0], parts[1].toInt())
}