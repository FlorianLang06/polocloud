package de.polocloud.node.generator

import de.polocloud.common.generator.Generator
import de.polocloud.node.repositories.NodeRepository

object NodeIndexGenerator : Generator<Int> {

    override fun generate(): Int {
        val usedIndexes = NodeRepository.findAll()
            .map { it.index }
            .toSet()

        var index = 1
        while (index in usedIndexes) {
            index++
        }

        return index
    }
}