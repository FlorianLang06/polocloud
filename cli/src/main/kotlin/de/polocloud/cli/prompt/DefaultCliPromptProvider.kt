package de.polocloud.cli.prompt

class DefaultCliPromptProvider : CliPromptProvider {

    override fun default(): String {
        return "&bpolocloud&8@&7cli &8» &7"
    }

    override fun disconnected(): String {
        return default()
    }

    override fun connected(nodeName: String): String {
        return "&bpolocloud&8@&7$nodeName &8» &7"
    }
}