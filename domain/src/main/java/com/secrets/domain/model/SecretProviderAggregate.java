package com.secrets.domain.model;

import com.secrets.domain.event.SecretRefreshed;
import com.secrets.domain.event.SecretRolloverDetected;
import com.secrets.domain.service.RefreshPolicy;
import com.secrets.domain.service.SecretAccessException;
import com.secrets.domain.service.SecretCache;
import com.secrets.domain.service.SecretProvider;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Aggregate root responsible for coordinating retrieval, refresh, and caching of secrets.
 * This class orchestrates the interaction between SecretProvider, SecretCache, and RefreshPolicy.
 */
public class SecretProviderAggregate {
    private final SecretReference reference;
    private final SecretProvider provider;
    private final SecretCache cache;
    private final RefreshPolicy refreshPolicy;
    private final AccessCredential credential;
    private final AtomicReference<Secret> lastRetrieved;
    private final Consumer<Object> eventPublisher;

    /**
     * Creates a new SecretProviderAggregate instance.
     *
     * @param reference     The reference to the secret managed by this aggregate
     * @param provider      The provider used to access the secret store
     * @param cache         The cache used to store the secret locally
     * @param refreshPolicy The policy for refreshing the secret
     * @param credential    The credentials used to access the secret
     * @param eventPublisher The publisher to notify of domain events
     */
    public SecretProviderAggregate(
            SecretReference reference,
            SecretProvider provider,
            SecretCache cache,
            RefreshPolicy refreshPolicy,
            AccessCredential credential,
            Consumer<Object> eventPublisher) {
        this.reference = Objects.requireNonNull(reference, "Secret reference cannot be null");
        this.provider = Objects.requireNonNull(provider, "Secret provider cannot be null");
        this.cache = Objects.requireNonNull(cache, "Secret cache cannot be null");
        this.refreshPolicy = Objects.requireNonNull(refreshPolicy, "Refresh policy cannot be null");
        this.credential = Objects.requireNonNull(credential, "Access credential cannot be null");
        this.eventPublisher = eventPublisher != null ? eventPublisher : event -> {};
        this.lastRetrieved = new AtomicReference<>();
        
        validateConfiguration();
        initializeRefreshPolicy();
    }
    
    private void validateConfiguration() {
        if (!provider.supportsStore(reference.getStoreType())) {
            throw new IllegalArgumentException(
                    "Provider " + provider.getClass().getSimpleName() +
                    " does not support store type " + reference.getStoreType());
        }
    }
    
    private void initializeRefreshPolicy() {
        refreshPolicy.apply(provider, cache);
        if (!refreshPolicy.isRunning()) {
            refreshPolicy.start();
        }
    }

    /**
     * Gets the current secret, either from cache or by fetching from the source.
     *
     * @return The secret
     * @throws SecretAccessException if the secret cannot be accessed
     */
    public Secret getSecret() throws SecretAccessException {
        // Try to get from cache first
        Optional<Secret> cachedSecret = cache.get(reference);
        
        // If cached and not stale, use it
        if (cachedSecret.isPresent() && !cache.isStale(cachedSecret.get().getMetadata().getSourceRef())) {
            Secret secret = cachedSecret.get();
            lastRetrieved.set(secret);
            return secret;
        }
        
        // Otherwise fetch and cache
        return refreshSecret();
    }

    /**
     * Forces a refresh of the secret from its source.
     *
     * @return The refreshed secret
     * @throws SecretAccessException if the secret cannot be accessed
     */
    public Secret refreshSecret() throws SecretAccessException {
        Secret oldSecret = lastRetrieved.get();
        String oldVersion = oldSecret != null ? oldSecret.getMetadata().getVersion() : null;
        
        // Fetch fresh secret from provider
        Secret freshSecret = provider.fetchSecret(reference, credential);
        lastRetrieved.set(freshSecret);
        
        // Check if this is a rollover (active/inactive switch)
        checkForRollover(freshSecret);
        
        // Cache the fresh secret
        cache.put(freshSecret);
        
        // Publish refresh event
        boolean valueChanged = oldSecret == null || 
                !java.util.Arrays.equals(oldSecret.getValue(), freshSecret.getValue());
        eventPublisher.accept(new SecretRefreshed(freshSecret, valueChanged));
        
        return freshSecret;
    }
    
    private void checkForRollover(Secret freshSecret) {
        // If this is the "active" in an active/inactive pair, check for rollover
        if (reference.getVersionHint().equalsIgnoreCase("active")) {
            SecretReference inactiveRef = new SecretReference(
                    reference.getStoreType(), 
                    reference.getName(), 
                    "inactive");
            
            // Check if we have the inactive version cached
            Optional<Secret> inactiveSecret = cache.get(inactiveRef);
            Secret oldActive = lastRetrieved.get();
            
            if (inactiveSecret.isPresent() && oldActive != null && 
                    !oldActive.getMetadata().getVersion().equals(freshSecret.getMetadata().getVersion())) {
                // A rollover has occurred, publish event
                eventPublisher.accept(new SecretRolloverDetected(
                        reference, 
                        inactiveRef, 
                        freshSecret.getMetadata().getVersion()));
            }
        }
    }

    public SecretReference getReference() {
        return reference;
    }

    public SecretProvider getProvider() {
        return provider;
    }

    public SecretCache getCache() {
        return cache;
    }

    public RefreshPolicy getRefreshPolicy() {
        return refreshPolicy;
    }

    public AccessCredential getCredential() {
        return credential;
    }
    
    /**
     * Stops any background processes started by this aggregate's refresh policy.
     */
    public void stop() {
        if (refreshPolicy.isRunning()) {
            refreshPolicy.stop();
        }
    }
}
