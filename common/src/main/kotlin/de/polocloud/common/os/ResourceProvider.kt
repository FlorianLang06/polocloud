package de.polocloud.common.os

interface ResourceProvider {
    fun cpuUsage(): Double
    fun usedMemory(): Double
    fun maxMemory(): Double
}