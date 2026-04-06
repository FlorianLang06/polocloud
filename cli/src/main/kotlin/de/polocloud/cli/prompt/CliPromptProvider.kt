package de.polocloud.cli.prompt

interface CliPromptProvider {

    fun disconnected(): String

    fun connected(nodeName: String): String
}