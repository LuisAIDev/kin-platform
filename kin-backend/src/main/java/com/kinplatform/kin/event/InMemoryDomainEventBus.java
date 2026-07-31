package com.kinplatform.kin.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class InMemoryDomainEventBus implements DomainEventBus {

    private final Map<Class<?>, List<Consumer<DomainEvent>>> subscribers = new ConcurrentHashMap<>();
    private final List<DomainEvent> publishedEvents = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public void publish(DomainEvent event) {
        publishedEvents.add(event);
        var handlers = subscribers.get(event.getClass());
        if (handlers != null) {
            handlers.forEach(h -> h.accept(event));
        }
    }

    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>())
                .add(e -> handler.handle((T) e));
    }

    public List<DomainEvent> publishedEvents() {
        return List.copyOf(publishedEvents);
    }

    public void clear() {
        publishedEvents.clear();
        subscribers.clear();
    }
}
