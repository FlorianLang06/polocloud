package de.polocloud.node.generator

import de.polocloud.node.repositories.NodeRepository

class NodeIndexGenerator(val nodeRepository: NodeRepository) : Generator<Int> {

    override fun generate(): Int {
        val usedIndexes = nodeRepository.findAll()
            .map { it.index }
            .toSet()

        var index = 1
        while (index in usedIndexes) {
            index++
        }

        return index
    }
}