package de.polocloud.node.bootstrap

object StartupTimer {

    private val startupTime: Long?
        get() = System.getProperty("polocloud.startup")?.toLongOrNull()

    val elapsed: Long?
        get() = startupTime?.let { System.currentTimeMillis() - it }

    val formatted: String
        get() = elapsed?.let {
            if (it >= 1000) "%.2fs".format(it / 1000.0) else "${it}ms"
        } ?: "unknown"
}