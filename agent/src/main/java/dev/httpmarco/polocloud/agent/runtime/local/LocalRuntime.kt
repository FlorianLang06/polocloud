package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.runtime.Runtime
import dev.httpmarco.polocloud.agent.runtime.local.terminal.Jline3Terminal
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.GroupCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.PlatformCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.ServiceCommand
class LocalRuntime : Runtime {

    private val runtimeGroupStorage = LocalRuntimeGroupStorage()
    private val runtimeServiceStorage = LocalRuntimeServiceStorage()
    private val runtimeFactory = LocalRuntimeFactory(this)
    private val runtimeQueue = LocalRuntimeQueue()
    private val runtimeExpender = LocalRuntimeExpender()
    private val templates = LocalRuntimeTemplates()

    lateinit var terminal: Jline3Terminal

    override fun boot() {
        terminal = Jline3Terminal()

        terminal.commandService.registerCommand(GroupCommand(runtimeGroupStorage))
        terminal.commandService.registerCommand(ServiceCommand(runtimeServiceStorage, terminal))
        terminal.commandService.registerCommand(PlatformCommand())
    }

    override fun runnable(): Boolean {
        return true // LocalRuntime is always runnable
    }

    override fun serviceStorage() = runtimeServiceStorage

    override fun groupStorage() = runtimeGroupStorage

    override fun factory() = runtimeFactory

    override fun expender() = runtimeExpender

    override fun templates() = templates

    override fun shutdown() {
        this.terminal.shutdown()

        this.runtimeQueue.interrupt()
    }

    override fun postInitialize() {
        this.terminal.jLine3Reading.start()
        this.runtimeQueue.start()
    }
}