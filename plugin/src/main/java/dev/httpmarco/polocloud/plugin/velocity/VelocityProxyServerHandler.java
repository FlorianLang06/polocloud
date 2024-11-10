package dev.httpmarco.polocloud.plugin.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import dev.httpmarco.polocloud.plugin.ProxyServerHandler;
import lombok.AllArgsConstructor;

import java.net.InetSocketAddress;
import java.util.List;

@AllArgsConstructor
public final class VelocityProxyServerHandler implements ProxyServerHandler {

    private final ProxyServer proxyServer;

    @Override
    public void registerServer(String name, String hostname, int port) {
        proxyServer.registerServer(new ServerInfo(name, new InetSocketAddress(hostname, port)));
    }

    @Override
    public void unregisterServer(String name) {
        proxyServer.getServer(name).ifPresent(registeredServer -> proxyServer.unregisterServer(registeredServer.getServerInfo()));
    }

    @Override
    public List<String> services() {
        return proxyServer.getAllServers().stream().map(it -> it.getServerInfo().getName()).toList();
    }
}
