package dev.httpmarco.polocloud.agent.runtime.local.terminal.screen

import dev.httpmarco.polocloud.agent.runtime.local.terminal.JLine3Terminal
import dev.httpmarco.polocloud.agent.runtime.local.terminal.LoggingColor
import dev.httpmarco.polocloud.agent.services.AbstractService

/**
 * This class is responsible for managing the screen recording of a service.
 * It allows starting and stopping the recording of a service's screen.
 */
class ServiceScreenController(val terminal: JLine3Terminal) {

    private var displayedAbstractService: AbstractService? = null

    fun screenRecordingOf(abstractService: AbstractService) {
        displayedAbstractService = abstractService

        terminal.clearScreen()

        abstractService.logs(5000).forEach {
            terminal.display(it)
        }

        terminal.updatePrompt(LoggingColor.translate("&b${abstractService.name()} &8» &7"))
    }

    fun stopCurrentRecording() {

        if (!isRecoding()) {
            return
        }

        this.displayedAbstractService = null

        terminal.clearScreen()
        terminal.resetPrompt()
        // todo display the context before the recording
    }

    fun isServiceRecoding(abstractService: AbstractService): Boolean {
        return isRecoding() && displayedAbstractService!!.name() == abstractService.name()
    }

    fun isRecoding(): Boolean {
        return displayedAbstractService != null
    }

    fun redirectCommand(command: String) {
        if (!isRecoding()) {
            throw IllegalStateException("Cannot redirect command to service because no service is currently being recorded.")
        }
        this.displayedAbstractService?.executeCommand(command)
    }
}