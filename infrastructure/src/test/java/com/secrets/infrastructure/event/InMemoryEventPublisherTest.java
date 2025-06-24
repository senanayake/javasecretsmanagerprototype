package com.secrets.infrastructure.event;

import com.secrets.domain.event.SecretEvent;
import com.secrets.domain.event.SecretRefreshRequested;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryEventPublisherTest {

    private InMemoryEventPublisher publisher;
    private SecretReference testRef;

    @BeforeEach
    void setUp() {
        publisher = new InMemoryEventPublisher();
        testRef = new SecretReference(StoreType.AWS_SECRETS_MANAGER, "test-secret", "latest");
    }

    @Test
    void shouldNotifySubscriberOfEvent() {
        // Given
        AtomicReference<SecretRefreshRequested> receivedEvent = new AtomicReference<>();
        publisher.subscribe(SecretRefreshRequested.class, receivedEvent::set);
        
        // When
        SecretRefreshRequested event = new SecretRefreshRequested(testRef, "test reason");
        publisher.publish(event);
        
        // Then
        assertThat(receivedEvent.get()).isEqualTo(event);
    }
    
    @Test
    void shouldNotifyMultipleSubscribers() {
        // Given
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();
        
        publisher.subscribe(SecretRefreshRequested.class, e -> counter1.incrementAndGet());
        publisher.subscribe(SecretRefreshRequested.class, e -> counter2.incrementAndGet());
        
        // When
        SecretRefreshRequested event = new SecretRefreshRequested(testRef, "test reason");
        publisher.publish(event);
        
        // Then
        assertThat(counter1.get()).isEqualTo(1);
        assertThat(counter2.get()).isEqualTo(1);
    }
    
    @Test
    void shouldTrackPublishedEvents() {
        // Given
        SecretRefreshRequested event1 = new SecretRefreshRequested(testRef, "reason 1");
        SecretRefreshRequested event2 = new SecretRefreshRequested(testRef, "reason 2");
        
        // When
        publisher.publish(event1);
        publisher.publish(event2);
        
        // Then
        assertThat(publisher.getPublishedEvents()).hasSize(2);
        assertThat(publisher.getPublishedEvents().get(0)).isEqualTo(event1);
        assertThat(publisher.getPublishedEvents().get(1)).isEqualTo(event2);
    }
    
    @Test
    void shouldClearPublishedEvents() {
        // Given
        publisher.publish(new SecretRefreshRequested(testRef, "reason"));
        
        // When
        publisher.clearPublishedEvents();
        
        // Then
        assertThat(publisher.getPublishedEvents()).isEmpty();
    }
    
    @Test
    void shouldNotifyForSuperclassSubscriptions() {
        // Given
        AtomicInteger secretEventCounter = new AtomicInteger();
        AtomicInteger refreshRequestedCounter = new AtomicInteger();
        
        // Subscribe to the parent class
        publisher.subscribe(SecretEvent.class, e -> secretEventCounter.incrementAndGet());
        
        // Subscribe to the specific class
        publisher.subscribe(SecretRefreshRequested.class, e -> refreshRequestedCounter.incrementAndGet());
        
        // When
        SecretRefreshRequested event = new SecretRefreshRequested(testRef, "test reason");
        publisher.publish(event);
        
        // Then
        // Both subscribers should be notified (SecretRefreshRequested is a SecretEvent)
        assertThat(secretEventCounter.get()).isEqualTo(1);
        assertThat(refreshRequestedCounter.get()).isEqualTo(1);
    }
    
    @Test
    void shouldNotNotifyUnsubscribedEventTypes() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        publisher.subscribe(SecretRefreshRequested.class, e -> counter.incrementAndGet());
        
        // When
        // Create a different event type
        publisher.publish(new Object());
        
        // Then
        assertThat(counter.get()).isEqualTo(0);
    }
    
    @Test
    void shouldRemoveAllSubscribersForEventType() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        publisher.subscribe(SecretRefreshRequested.class, e -> counter.incrementAndGet());
        
        // When
        publisher.unsubscribeAll(SecretRefreshRequested.class);
        SecretRefreshRequested event = new SecretRefreshRequested(testRef, "test reason");
        publisher.publish(event);
        
        // Then
        assertThat(counter.get()).isEqualTo(0);
    }
    
    @Test
    void shouldIgnoreNullEvents() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        publisher.subscribe(Object.class, e -> counter.incrementAndGet());
        
        // When
        publisher.publish(null);
        
        // Then
        assertThat(counter.get()).isEqualTo(0);
    }
    
    @Test
    void shouldHandleSubscriberExceptions() {
        // Given
        AtomicInteger counter = new AtomicInteger();
        
        // First subscriber throws an exception
        publisher.subscribe(SecretRefreshRequested.class, e -> {
            throw new RuntimeException("Test exception");
        });
        
        // Second subscriber should still be called
        publisher.subscribe(SecretRefreshRequested.class, e -> counter.incrementAndGet());
        
        // When
        SecretRefreshRequested event = new SecretRefreshRequested(testRef, "test reason");
        publisher.publish(event);
        
        // Then
        assertThat(counter.get()).isEqualTo(1);
    }
}
