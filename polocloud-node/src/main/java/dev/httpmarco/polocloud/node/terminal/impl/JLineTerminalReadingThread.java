package dev.httpmarco.polocloud.node.terminal.impl;

import dev.httpmarco.polocloud.api.Closeable;
import dev.httpmarco.polocloud.node.NodeShutdown;
import dev.httpmarco.polocloud.node.terminal.utils.TerminalColorReplacer;
import lombok.AllArgsConstructor;
import org.jline.reader.UserInterruptException;

@AllArgsConstructor
public final class JLineTerminalReadingThread extends Thread implements Closeable {

    private final String prompt = TerminalColorReplacer.replaceColorCodes("&9default&8@&7cloud &8» &7");
    private final JLineTerminalImpl terminal;

    @Override
    public void run() {
        while (!isInterrupted()) {

            try {
            var rawLine = terminal.lineReader().readLine(prompt).trim();

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
