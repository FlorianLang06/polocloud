package dev.httpmarco.polocloud.instance.events;

import dev.httpmarco.polocloud.api.event.Event;
import dev.httpmarco.polocloud.api.event.EventFactory;
import dev.httpmarco.polocloud.api.event.EventProvider;
import dev.httpmarco.polocloud.api.event.EventSubscribePool;
import dev.httpmarco.polocloud.api.packet.resources.event.EventCallPacket;
import dev.httpmarco.polocloud.api.packet.resources.event.EventSubscribePacket;
import dev.httpmarco.polocloud.instance.ClusterInstance;

import java.util.function.Consumer;

public final class EventProviderImpl implements EventProvider, EventFactory {

    // local subscribe pool
    private final EventSubscribePool pool = new EventSubscribePool("local");

    public EventProviderImpl() {
        ClusterInstance.instance().client().listen(EventCallPacket.class, (transmit, packet) -> {
            try {
                pool.acceptActor(packet.buildEvent());
            } catch (ClassNotFoundException ignored) {
            }
        });
    }

    @Override
    public void call(Event event) {
        ClusterInstance.instance().client().sendPacket(new EventCallPacket(event));
    }

    @Override
    public EventFactory factory() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> void listen(Class<T> eventClazz, Consumer<T> event) {
        ClusterInstance.instance().client().sendPacket(new EventSubscribePacket(eventClazz));

        pool.subscribe(eventClazz.getName(), event1 -> event.accept((T) event1));
    }
}
