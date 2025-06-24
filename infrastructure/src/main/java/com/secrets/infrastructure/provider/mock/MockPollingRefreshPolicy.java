package com.secrets.infrastructure.provider.mock;

import com.secrets.domain.event.SecretRefreshRequested;
import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.service.RefreshPolicy;
import com.secrets.domain.service.SecretCache;
import com.secrets.domain.service.SecretProvider;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A mock implementation of RefreshPolicy that refreshes secrets based on a polling schedule.
 */
public class MockPollingRefreshPolicy implements RefreshPolicy {
    
    private final Duration pollingInterval;
    private final Map<SecretReference, AccessCredential> credentials = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Consumer<Object> eventPublisher;
    
    private SecretProvider secretProvider;
    private SecretCache secretCache;
    
    /**
     * Creates a new MockPollingRefreshPolicy with a default polling interval.
     * 
     * @param eventPublisher Publisher for domain events
     */
    public MockPollingRefreshPolicy(Consumer<Object> eventPublisher) {
        this(Duration.ofMinutes(1), eventPublisher);
    }
    
    /**
     * Creates a new MockPollingRefreshPolicy with a specified polling interval.
     * 
     * @param pollingInterval The interval at which to poll for secret updates
     * @param eventPublisher  Publisher for domain events
     */
    public MockPollingRefreshPolicy(Duration pollingInterval, Consumer<Object> eventPublisher) {
        this.pollingInterval = pollingInterval;
        this.eventPublisher = eventPublisher != null ? eventPublisher : event -> {};
    }
    
    /**
     * Registers a secret to be managed by this refresh policy.
     * 
     * @param reference  The reference to the secret
     * @param credential The credential to use for accessing the secret
     */
    public void registerSecret(SecretReference reference, AccessCredential credential) {
        credentials.put(reference, credential);
    }
    
    /**
     * Unregisters a secret from this refresh policy.
     * 
     * @param reference The reference to the secret to unregister
     */
    public void unregisterSecret(SecretReference reference) {
        credentials.remove(reference);
    }

    @Override
    public void apply(SecretProvider secretProvider, SecretCache secretCache) {
        this.secretProvider = secretProvider;
        this.secretCache = secretCache;
    }

    @Override
    public boolean isRefreshNeeded(SecretReference reference, Secret cachedSecret) {
        if (cachedSecret == null) {
            return true;
        }
        
        // In a more sophisticated implementation, we might check if the cached secret
        // is stale or if there's a newer version available from the provider
        return secretCache.isStale(reference);
    }

    @Override
    public void triggerRefresh(SecretReference reference) {
        AccessCredential credential = credentials.get(reference);
        if (credential == null || secretProvider == null || secretCache == null) {
            return;
        }
        
        // Publish refresh requested event
        eventPublisher.accept(new SecretRefreshRequested(reference, "Manual refresh"));
        
        try {
            // Try to fetch the latest secret from the provider
            Secret secret = secretProvider.fetchSecret(reference, credential);
            
            // Update the cache
            secretCache.put(secret);
        } catch (Exception e) {
            // In a real implementation, we would log this error
            System.err.println("Failed to refresh secret " + reference + ": " + e.getMessage());
        }
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true) && secretProvider != null && secretCache != null) {
            // Schedule a periodic task to check for refreshes
            scheduler.scheduleAtFixedRate(() -> {
                credentials.forEach((reference, credential) -> {
                    Optional<Secret> cachedSecret = secretCache.get(reference);
                    if (cachedSecret.isEmpty() || isRefreshNeeded(reference, cachedSecret.get())) {
                        triggerRefresh(reference);
                    }
                });
            }, 0, pollingInterval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
