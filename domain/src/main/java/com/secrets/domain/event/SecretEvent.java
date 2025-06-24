package com.secrets.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all secret-related domain events.
 */
public abstract class SecretEvent {
    private final String eventId;
    private final Instant timestamp;

    protected SecretEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
