package dev.httpmarco.polocloud.node.node

object NodeIndexGenerator {

    fun findNextFreeIndex(repository: dev.httpmarco.polocloud.node.repository.NodeRepository): Int {
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