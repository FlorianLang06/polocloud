package de.polocloud.cli.prompt

interface CliPromptProvider {

    fun default(): String

    fun disconnected(): String

    fun connected(nodeName: String): String
}