package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository

/**
 * Terminal argument that resolves an existing [NodeData] by its cluster name (e.g.
 * `node-1`, see [NodeData.name]).
 *
 * The raw input is the node name. The argument only matches when a known node has that
 * name and offers all known node names as tab-completion suggestions.
 */
class NodeArgument(key: String) : TerminalArgument<NodeData>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return NodeRepository.findAll().map { it.name() }.toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        return rawInput.isNotBlank() && NodeRepository.findAll().any { it.name().equals(rawInput, ignoreCase = true) }
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.node.notExists"
    }

    override fun buildResult(input: String, context: InputContext): NodeData {
        // safe: predication guarantees a matching node exists
        return NodeRepository.findAll().first { it.name().equals(input, ignoreCase = true) }
    }
}
