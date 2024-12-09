package dev.httpmarco.polocloud.node.terminal.impl;

import dev.httpmarco.polocloud.api.Closeable;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.NodeShutdown;
import dev.httpmarco.polocloud.node.terminal.utils.TerminalColorReplacer;
import lombok.AllArgsConstructor;
import org.jline.reader.UserInterruptException;

import java.util.Arrays;

@AllArgsConstructor
public final class JLineTerminalReadingThread extends Thread implements Closeable {

    private final String prompt = TerminalColorReplacer.replaceColorCodes("&9default&8@&7cloud &8» &7");
    private final JLineTerminalImpl terminal;

    @Override
    public void run() {
        while (!isInterrupted()) {

            try {
                var rawLine = terminal.lineReader().readLine(prompt).trim();


                if (rawLine.isEmpty()) {
                    continue;
                }

                final var line = rawLine.split(" ");

                if (line.length > 0) {
                    if (terminal.isInSetup()) {
                        if (rawLine.equalsIgnoreCase("exit")) {
                            terminal.setup().exit(false);
                            continue;
                        }

                        if (rawLine.equalsIgnoreCase("back")) {
                            terminal.setup().previousQuestion();
                            continue;
                        }

                        terminal.setup().answer(rawLine);
                        continue;
                    }

                    Node.instance().terminal().commandService().call(line[0], Arrays.copyOfRange(line, 1, line.length));
                }

            } catch (UserInterruptException exception) {
                // if a command user use strg + c
                NodeShutdown.nodeShutdownTotal(true);
            }
        }
    }

    @Override
    public void close() {
        this.interrupt();
    }
}
