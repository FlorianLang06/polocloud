package de.polocloud.cli.prompt

class DefaultCliPromptProvider : CliPromptProvider {

    override fun disconnected(): String {
        return "&bpolocloud&8@&7cli &8» &7"
    }

    override fun connected(nodeName: String): String {
        return "&bpolocloud&8@&7$nodeName &8» &7"
    }
}