package com.secrets.application.port;

/**
 * Port interface for publishing domain events.
 * This interface provides a way for the domain to publish events
 * without depending on specific messaging technologies.
 */
public interface EventPublisher {
    
    /**
     * Publishes an event.
     *
     * @param event The event to publish
     */
    void publish(Object event);
}
