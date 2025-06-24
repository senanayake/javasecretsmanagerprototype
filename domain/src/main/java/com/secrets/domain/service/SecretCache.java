package com.secrets.domain.service;

import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretReference;

import java.time.Duration;
import java.util.Optional;

/**
 * Interface defining operations for secret caching.
 * Implementations will handle local storage of secrets to avoid excessive calls to secret stores.
 */
public interface SecretCache {
    
    /**
     * Gets a secret from the cache if available.
     *
     * @param ref The reference to the secret to retrieve
     * @return An Optional containing the secret if found, or empty if not in cache
     */
    Optional<Secret> get(SecretReference ref);
    
    /**
     * Puts a secret into the cache.
     *
     * @param secret The secret to cache
     */
    void put(Secret secret);
    
    /**
     * Invalidates a cached secret, forcing it to be fetched from the source on next access.
     *
     * @param ref The reference to the secret to invalidate
     */
    void invalidate(SecretReference ref);
    
    /**
     * Clears all secrets from the cache.
     */
    void clear();
    
    /**
     * Sets the default time-to-live for secrets in this cache.
     * After this duration, secrets should be considered stale and refreshed.
     *
     * @param ttl The time-to-live duration
     */
    void setDefaultTTL(Duration ttl);
    
    /**
     * Gets the current default time-to-live for secrets in this cache.
     *
     * @return The current TTL duration
     */
    Duration getDefaultTTL();
    
    /**
     * Sets a specific time-to-live for a particular secret.
     *
     * @param ref The reference to the secret
     * @param ttl The time-to-live duration for this specific secret
     */
    default void setTTL(SecretReference ref, Duration ttl) {
        // Default implementation does nothing
    }
    
    /**
     * Checks if a secret in the cache is stale based on its TTL.
     *
     * @param ref The reference to the secret to check
     * @return true if the secret is stale or not in cache, false otherwise
     */
    boolean isStale(SecretReference ref);
}
