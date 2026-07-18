package de.polocloud.common.os

import com.sun.management.OperatingSystemMXBean
import de.polocloud.common.math.convertBytesToMegabytes
import java.lang.management.ManagementFactory
import kotlin.math.roundToInt

object ApplicationResources : ResourceProvider {

    private val osBean by lazy {
        ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
    }

    override fun cpuUsage(): Double {
        val load = osBean.processCpuLoad
        if (load < 0) return 0.0
        return ((load * 10000).roundToInt() / 100.0).coerceIn(0.0, 100.0)
    }

    override fun usedMemory(): Double {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        return convertBytesToMegabytes(used)
    }

    override fun maxMemory(): Double {
        val runtime = Runtime.getRuntime()
        return convertBytesToMegabytes(runtime.maxMemory())
    }
}