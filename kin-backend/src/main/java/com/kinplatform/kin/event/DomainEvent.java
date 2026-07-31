package com.kinplatform.kin.event;

public interface DomainEvent {
    String type();
    Object aggregateId();
}
