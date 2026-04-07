package de.polocloud.cli.terminal

import de.polocloud.cli.command.CommandService
import de.polocloud.cli.exitPolocloud
import de.polocloud.cli.logger
import org.jline.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.UserInterruptException

/**
 * Background thread that manages the interactive CLI session lifecycle.
 *
 * This thread continuously reads user input from the terminal and acts as the
 * central interaction coordinator of the CLI. Depending on the current
 * terminal state, it may:
 *
 * - Forward input to an active setup controller
 * - Redirect commands to a screen/recording service
 * - Parse and dispatch standard commands to the [CommandService]
 *
 * It also maintains prompt state and ensures the console remains visually
 * consistent (e.g. handling blank input cleanup).
 *
 * On user interruption (`Ctrl+C`, [UserInterruptException]) the application
 * exits immediately without performing a clean shutdown. Any other exceptions
 * during input handling or command execution are caught and logged so that
 * the interactive loop can continue running.
 *
 * @param terminal The active [CliTerminal] instance responsible for prompt
 *                 and terminal state handling.
 * @param lineReader The JLine [LineReader] used for interactive input reading.
 * @param commandService The [CommandService] used to execute parsed commands
 *                       when no special interaction mode is active.
 */
class ReadingThread(
    private var terminal: CliTerminal,
    private val lineReader: LineReader,
    private val commandService: CommandService
) : Thread("reading-thread") {

    override fun run() {
        while (!isInterrupted) {
            try {
                val line = lineReader.readLine(this.terminal.prompt).trim()

                //val screenService = terminal.screenService
                //val setupController = terminal.setupController

//                if(setupController.active()) {
//                    setupController.currentSetup()!!.acceptAnswer(line)
//                    continue
//                }

                if (line.isBlank()) {
                    // we reset the terminal prompt as message -> we have a clean console
                    println(Ansi.ansi().cursorUpLine().eraseLine().toString() + Ansi.ansi().cursorUp(1).toString())
                    continue
                }

//                if (screenService.isRecording()) {
//                    if (line == "exit") {
//                        screenService.stopCurrentRecording()
//                        continue
//                    }
//                    screenService.redirectCommand(line)
//                    continue
//                }

                val tokens = line.split(" ").filter { it.isNotBlank() }
                val commandName = tokens.firstOrNull() ?: continue
                val args = tokens.drop(1).toTypedArray()

                commandService.call(commandName, args)
            } catch (_: UserInterruptException) {
                // pressing Ctrl+C or similar to interrupt reading
                exitPolocloud(cleanShutdown = false)
                break
            } catch (e: Throwable) {
                logger.error("Command execution exception: ", e)
            }
        }
    }
}