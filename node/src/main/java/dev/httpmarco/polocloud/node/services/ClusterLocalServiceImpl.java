package dev.httpmarco.polocloud.node.services;

import dev.httpmarco.osgan.networking.channel.ChannelTransmit;
import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.polocloud.api.event.EventPoolRegister;
import dev.httpmarco.polocloud.api.event.EventSubscribePool;
import dev.httpmarco.polocloud.api.event.impl.services.ServiceStoppedEvent;
import dev.httpmarco.polocloud.api.event.impl.services.ServiceStoppingEvent;
import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.api.properties.PropertiesPool;
import dev.httpmarco.polocloud.api.services.ClusterServiceState;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.packets.resources.services.ClusterSyncUnregisterServicePacket;
import dev.httpmarco.polocloud.node.platforms.Platform;
import dev.httpmarco.polocloud.node.util.DirectoryActions;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Getter
@Setter
@Accessors(fluent = true)
public final class ClusterLocalServiceImpl extends ClusterServiceImpl {

    private @Nullable Process process;
    private @Nullable Thread processTracking;
    private @Nullable ChannelTransmit transmit;

    private final Path runningDir;
    private final List<String> logs = new CopyOnWriteArrayList<>();

    private final EventSubscribePool eventSubscribePool = new EventSubscribePool(this.name());

    public ClusterLocalServiceImpl(ClusterGroup group, int orderedId, UUID id, int port, String hostname, String runningNode) {
        super(group, orderedId, id, port, hostname, runningNode, new PropertiesPool());

        // set the default max players value to the parent value
        maxPlayers(group.maxPlayers());

        this.runningDir = group.staticService() ? Path.of("static/" + group.name() + "-" + orderedId) : Path.of("running/" + name() + "-" + id);
        this.runningDir.toFile().mkdirs();
    }

    @Override
    public void shutdown() {
        Node.instance().serviceProvider().factory().shutdownGroupService(this);
    }

    @Override
    public void sendPacket(Packet packet) {
        if(transmit == null) {
            log.error("Trying to send packet to service {} but the transmit is null", name());
            return;
        }
        this.transmit.sendPacket(packet);
    }

    @SneakyThrows
    public void start(@NotNull ProcessBuilder builder) {
        this.process = builder.start();

        this.processTracking = new Thread(() -> {
            // if player send a stop command from game command system
            try {
                this.process.waitFor();
            } catch (InterruptedException ignore) {
            }

            if (state() != ClusterServiceState.STOPPING) {
                Node.instance().eventProvider().factory().call(new ServiceStoppingEvent(this));
                state(ClusterServiceState.STOPPING);
                if (process != null) {
                    this.process.exitValue();
                }
                this.postShutdownProcess();
            }
        });

        this.processTracking.start();
    }

    @Override
    public CompletableFuture<Integer> currentMemoryAsync() {
        return CompletableFuture.completedFuture(0); //TODO
    }

    @Override
    @SneakyThrows
    public void executeCommand(String command) {
        if (!hasProcess() || command == null || command.isEmpty()) {
            return;
        }
        var writer = new BufferedWriter(new OutputStreamWriter(this.process.getOutputStream()));
        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    public void destroyService() {
        if (process != null) {
            state(ClusterServiceState.STOPPING);
            this.process.toHandle().destroyForcibly();
            this.postShutdownProcess();
        }
    }

    @SneakyThrows
    public void postShutdownProcess() {
        this.process = null;

        if (this.processTracking != null) {
            this.processTracking.interrupt();
            this.processTracking = null;
        }

        var screenedProvider = Node.instance().screenProvider();
        if(screenedProvider.isUsed()) {
            if(screenedProvider.current().equals(this)){
                screenedProvider.redraw();
            }
        }

        if (!group().staticService()) {
            synchronized (this) {
                // todo check for windows directory not empty exception
                try {
                    Thread.sleep(200);
                }catch (InterruptedException ignore) {}
                try {
                    if (DirectoryActions.delete(runningDir)) {
                        Files.deleteIfExists(runningDir);
                    } else {
                        log.info("Cannot shutdown {} cleanly! Files are already exists", name());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // unregister event pool
        EventPoolRegister.remove(eventSubscribePool);

        log.info("The service &8'&f{}&8' &7is stopped now&8!", name());

        // alert the event before remove it from the local cache
        Node.instance().eventProvider().factory().call(new ServiceStoppedEvent(this));

        Node.instance().clusterProvider().broadcastAll(new ClusterSyncUnregisterServicePacket(id()));
    }

    public boolean hasProcess() {
        return this.process != null;
    }

    public Platform platform() {
        return Node.instance().platformService().find(this.group().platform().platform());
    }
}
