package de.polocloud.node.utils

import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.services.process.ServiceProcessRepository

object IndexGenerator {

    fun generateNode(): Int {
        val usedIndexes = NodeRepository.findAll()
            .map { it.index }
            .toSet()

        return generate(usedIndexes)
    }

    fun generateService(): Int {
        val usedIndexes = ServiceProcessRepository.findAll()
            .map { it.index }
            .toSet()

        return generate(usedIndexes)
    }

    private fun generate(usedIndexes: Iterable<Int>): Int {
        var index = 1
        while (index in usedIndexes) {
            index++
        }

        return index
    }
}