package com.secrets.infrastructure.event;

import com.secrets.application.port.EventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A simple in-memory implementation of EventPublisher.
 * This publisher keeps subscriptions in memory and dispatches events synchronously.
 */
public class InMemoryEventPublisher implements EventPublisher {

    // Map from event type to list of subscribers for that event type
    private final Map<Class<?>, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();
    
    // For testing and debugging: stores all published events
    private final List<Object> publishedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(Object event) {
        if (event == null) {
            return;
        }

        // Store event for inspection
        publishedEvents.add(event);
        
        // Get all subscribers for this event type and its superclasses
        getSubscribers(event.getClass()).forEach(subscriber -> {
            try {
                subscriber.accept(event);
            } catch (Exception e) {
                // In a real implementation, we would log this error
                System.err.println("Error publishing event to subscriber: " + e.getMessage());
            }
        });
    }
    
    /**
     * Subscribes to events of a specific type.
     *
     * @param eventType  The class of events to subscribe to
     * @param subscriber The consumer to receive events
     * @param <T>        The event type
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> subscriber) {
        List<Consumer<Object>> eventSubscribers = subscribers.computeIfAbsent(
                eventType, k -> new CopyOnWriteArrayList<>());
        
        @SuppressWarnings("unchecked")
        Consumer<Object> typedSubscriber = event -> subscriber.accept((T) event);
        eventSubscribers.add(typedSubscriber);
    }
    
    /**
     * Unsubscribes all subscribers for a specific event type.
     *
     * @param eventType The event type to unsubscribe from
     */
    public void unsubscribeAll(Class<?> eventType) {
        subscribers.remove(eventType);
    }
    
    /**
     * Gets all published events for inspection.
     * This is primarily useful for testing.
     *
     * @return An unmodifiable list of all published events
     */
    public List<Object> getPublishedEvents() {
        return List.copyOf(publishedEvents);
    }
    
    /**
     * Clears the history of published events.
     */
    public void clearPublishedEvents() {
        publishedEvents.clear();
    }
    
    /**
     * Get all subscribers for a specific event type and its superclasses.
     *
     * @param eventType The event type
     * @return List of subscribers
     */
    private List<Consumer<Object>> getSubscribers(Class<?> eventType) {
        List<Consumer<Object>> result = new ArrayList<>();
        
        // Add subscribers for this exact type
        List<Consumer<Object>> exactSubscribers = subscribers.get(eventType);
        if (exactSubscribers != null) {
            result.addAll(exactSubscribers);
        }
        
        // Add subscribers for superclasses and interfaces
        for (Class<?> subscribedType : subscribers.keySet()) {
            if (subscribedType != eventType && subscribedType.isAssignableFrom(eventType)) {
                result.addAll(subscribers.get(subscribedType));
            }
        }
        
        return result;
    }
}
