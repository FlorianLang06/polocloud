package de.polocloud.common.os

import de.polocloud.common.math.convertBytesToMegabytes
import oshi.SystemInfo

object SystemResources : ResourceProvider {

    private val sysInfo = SystemInfo()
    private val processor = sysInfo.hardware.processor
    private val memory = sysInfo.hardware.memory
    private var prevTicks = processor.systemCpuLoadTicks

    override fun cpuUsage(): Double {
        val load = processor.getSystemCpuLoadBetweenTicks(prevTicks)
        prevTicks = processor.systemCpuLoadTicks
        return load * 100.0
    }

    override fun usedMemory(): Double {
        val used = memory.total - memory.available
        return convertBytesToMegabytes(used)
    }

    override fun maxMemory(): Double {
        return convertBytesToMegabytes(memory.total)
    }
}