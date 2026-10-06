package com.kinplatform.common.event;

@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
}

