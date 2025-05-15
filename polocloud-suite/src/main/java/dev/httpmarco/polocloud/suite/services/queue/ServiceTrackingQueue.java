package dev.httpmarco.polocloud.suite.services.queue;

import build.buf.gen.minekube.gate.v1.GateServiceGrpc;
import build.buf.gen.minekube.gate.v1.RegisterServerRequest;
import dev.httpmarco.polocloud.api.platform.PlatformType;
import dev.httpmarco.polocloud.api.services.ClusterService;
import dev.httpmarco.polocloud.api.services.ClusterServiceState;
import dev.httpmarco.polocloud.suite.PolocloudSuite;
import dev.httpmarco.polocloud.suite.services.ClusterLocalServiceImpl;
import dev.httpmarco.polocloud.suite.services.ClusterServiceProviderImpl;
import io.grpc.ManagedChannelBuilder;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.net.Socket;
import java.util.Objects;

@Log4j2
public final class ServiceTrackingQueue extends Thread {

    private final ClusterServiceProviderImpl serviceProvider;

    public ServiceTrackingQueue(ClusterServiceProviderImpl serviceProvider) {
        super("ServiceTrackingQueue");

        this.serviceProvider = serviceProvider;
    }

    @SneakyThrows
    @Override
    public void run() {
        while (!isInterrupted()) {
            for (ClusterService service : serviceProvider.findAll()) {
                if (service instanceof ClusterLocalServiceImpl localService) {
                    if(service.state() == ClusterServiceState.STARTING) {
                        if(pingService(localService.port())) {
                            localService.changeState(ClusterServiceState.ONLINE);
                            if (Objects.equals(localService.group().platform().name(), "gate")) {
                                sendAllServers();
                            }
                            log.info(PolocloudSuite.instance().translation().get("suite.cluster.service.nowOnline", localService.name()));
                        }
                    }
                }
            }
            try {
                sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
    }

    private boolean pingService(int port) {
        try (var socket = new Socket("localhost", port)) {
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private void sendAllServers() {
        var gateChannel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();
        var stub = GateServiceGrpc.newBlockingStub(gateChannel);
        for (ClusterService server : serviceProvider.findAll()) {
            if (server.group().platform().type() == PlatformType.SERVER) {
                var response = stub.registerServer(RegisterServerRequest.newBuilder().setAddress("localhost:" + server.port()).setName(server.name()).build());
            }
        }
        gateChannel.shutdownNow();
    }
}
