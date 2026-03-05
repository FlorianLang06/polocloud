package dev.httpmarco.polocloud.node.cluster.node

import dev.httpmarco.polocloud.node.cluster.repository.NodeRepository

object NodeIndexGenerator {

    fun findNextFreeIndex(repository: NodeRepository): Int {
        val usedIndexes = repository.findAll()
            .map { it.index }
            .toSet()

        var index = 1
        while (index in usedIndexes) {
            index++
        }

        return index
    }
}