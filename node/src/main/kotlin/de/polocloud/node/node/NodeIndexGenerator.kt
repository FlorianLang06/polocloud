package de.polocloud.node.node

import de.polocloud.node.repository.NodeRepository

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