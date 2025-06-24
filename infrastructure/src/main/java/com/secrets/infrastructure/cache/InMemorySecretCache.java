package com.secrets.infrastructure.cache;

import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.service.SecretCache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of SecretCache.
 * Stores secrets in memory with configurable TTL.
 */
public class InMemorySecretCache implements SecretCache {

    private static class CacheEntry {
        final Secret secret;
        final Instant expiryTime;

        CacheEntry(Secret secret, Duration ttl) {
            this.secret = secret;
            this.expiryTime = Instant.now().plus(ttl);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }
    }

    private final Map<SecretReference, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<SecretReference, Duration> ttlOverrides = new ConcurrentHashMap<>();
    private Duration defaultTTL = Duration.ofMinutes(15); // Default 15 minute TTL

    @Override
    public Optional<Secret> get(SecretReference ref) {
        CacheEntry entry = cache.get(ref);
        if (entry == null) {
            return Optional.empty();
        }
        
        if (entry.isExpired()) {
            // Lazily clean up expired entries
            cache.remove(ref);
            return Optional.empty();
        }
        
        return Optional.of(entry.secret);
    }

    @Override
    public void put(Secret secret) {
        SecretReference ref = secret.getMetadata().getSourceRef();
        Duration ttl = ttlOverrides.getOrDefault(ref, defaultTTL);
        cache.put(ref, new CacheEntry(secret, ttl));
    }

    @Override
    public void invalidate(SecretReference ref) {
        cache.remove(ref);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void setDefaultTTL(Duration ttl) {
        this.defaultTTL = ttl;
    }

    @Override
    public Duration getDefaultTTL() {
        return defaultTTL;
    }

    @Override
    public void setTTL(SecretReference ref, Duration ttl) {
        ttlOverrides.put(ref, ttl);
    }

    @Override
    public boolean isStale(SecretReference ref) {
        CacheEntry entry = cache.get(ref);
        return entry == null || entry.isExpired();
    }
}
