package com.secrets.infrastructure.provider.mock;

import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretMetadata;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import com.secrets.domain.service.SecretAccessException;
import com.secrets.domain.service.SecretProvider;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock implementation of SecretProvider for testing and demo purposes.
 * This provider simulates access to a secret store with configurable behavior.
 */
public class MockSecretProvider implements SecretProvider {

    private final StoreType supportedStoreType;
    private final Map<String, Secret> secrets = new ConcurrentHashMap<>();
    private final int minDelayMs;
    private final int maxDelayMs;
    private final double failureProbability;
    
    /**
     * Builder for creating MockSecretProvider instances with custom configuration.
     */
    public static class Builder {
        private StoreType supportedStoreType = StoreType.AWS_SECRETS_MANAGER;
        private int minDelayMs = 50;
        private int maxDelayMs = 200;
        private double failureProbability = 0.0;
        
        /**
         * Sets the store type that this mock provider will support.
         *
         * @param storeType The store type to support
         * @return This builder
         */
        public Builder withStoreType(StoreType storeType) {
            this.supportedStoreType = storeType;
            return this;
        }
        
        /**
         * Sets the delay range for simulating network latency.
         *
         * @param minDelayMs Minimum delay in milliseconds
         * @param maxDelayMs Maximum delay in milliseconds
         * @return This builder
         */
        public Builder withDelayRange(int minDelayMs, int maxDelayMs) {
            this.minDelayMs = minDelayMs;
            this.maxDelayMs = maxDelayMs;
            return this;
        }
        
        /**
         * Sets the probability of simulated failures.
         *
         * @param probability Value between 0.0 (never fail) and 1.0 (always fail)
         * @return This builder
         */
        public Builder withFailureProbability(double probability) {
            this.failureProbability = Math.min(1.0, Math.max(0.0, probability));
            return this;
        }
        
        /**
         * Builds and returns a configured MockSecretProvider.
         *
         * @return A new MockSecretProvider instance
         */
        public MockSecretProvider build() {
            return new MockSecretProvider(
                    supportedStoreType, minDelayMs, maxDelayMs, failureProbability);
        }
    }
    
    /**
     * Creates a new builder for configuring a MockSecretProvider.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    private MockSecretProvider(
            StoreType supportedStoreType,
            int minDelayMs,
            int maxDelayMs,
            double failureProbability) {
        this.supportedStoreType = supportedStoreType;
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.failureProbability = failureProbability;
        
        // Preload some secrets for testing
        addSecret("test-secret", "test-value-123");
        addSecret("db-password", "password123!");
        addSecret("api-key", "sk_test_abcdefghijklmnopqrstuvwxyz");
        
        // Add an active/inactive pair for testing rollover
        addSecret("rollover-secret", "active-value", "active");
        addSecret("rollover-secret", "inactive-value", "inactive");
    }
    
    /**
     * Adds a secret to this mock provider with the "latest" version hint.
     *
     * @param name  The name of the secret
     * @param value The value of the secret
     */
    public void addSecret(String name, String value) {
        addSecret(name, value, "latest");
    }
    
    /**
     * Adds a secret to this mock provider with a specific version hint.
     *
     * @param name        The name of the secret
     * @param value       The value of the secret
     * @param versionHint The version hint (e.g., "latest", "v1", "active", "inactive")
     */
    public void addSecret(String name, String value, String versionHint) {
        SecretReference ref = new SecretReference(supportedStoreType, name, versionHint);
        SecretMetadata metadata = new SecretMetadata(
                UUID.randomUUID().toString(),
                Instant.now(),
                supportedStoreType,
                ref);
        
        Secret secret = new Secret(
                UUID.randomUUID().toString(),
                name,
                value.toCharArray(),
                metadata);
        
        secrets.put(createKey(name, versionHint), secret);
    }
    
    /**
     * Updates an existing secret with a new value.
     *
     * @param name  The name of the secret
     * @param value The new value
     * @return true if the secret was updated, false if it didn't exist
     */
    public boolean updateSecret(String name, String value) {
        return updateSecret(name, value, "latest");
    }
    
    /**
     * Updates an existing secret with a new value and specific version hint.
     *
     * @param name        The name of the secret
     * @param value       The new value
     * @param versionHint The version hint
     * @return true if the secret was updated, false if it didn't exist
     */
    public boolean updateSecret(String name, String value, String versionHint) {
        String key = createKey(name, versionHint);
        Secret existingSecret = secrets.get(key);
        
        if (existingSecret == null) {
            return false;
        }
        
        SecretReference ref = existingSecret.getMetadata().getSourceRef();
        SecretMetadata metadata = new SecretMetadata(
                UUID.randomUUID().toString(),
                Instant.now(),
                supportedStoreType,
                ref);
        
        Secret updatedSecret = new Secret(
                existingSecret.getId(),
                name,
                value.toCharArray(),
                metadata);
        
        secrets.put(key, updatedSecret);
        return true;
    }
    
    /**
     * Simulates a rollover by swapping active and inactive secrets.
     *
     * @param name The name of the secret to roll over
     * @return true if rollover was successful, false if the active/inactive pair doesn't exist
     */
    public boolean performRollover(String name) {
        Secret active = secrets.get(createKey(name, "active"));
        Secret inactive = secrets.get(createKey(name, "inactive"));
        
        if (active == null || inactive == null) {
            return false;
        }
        
        // Create new versions
        Secret newActive = new Secret(
                inactive.getId(),
                name,
                inactive.getValue(),
                new SecretMetadata(
                        UUID.randomUUID().toString(),
                        Instant.now(),
                        supportedStoreType,
                        new SecretReference(supportedStoreType, name, "active")
                )
        );
        
        Secret newInactive = new Secret(
                active.getId(),
                name,
                active.getValue(),
                new SecretMetadata(
                        UUID.randomUUID().toString(),
                        Instant.now(),
                        supportedStoreType,
                        new SecretReference(supportedStoreType, name, "inactive")
                )
        );
        
        secrets.put(createKey(name, "active"), newActive);
        secrets.put(createKey(name, "inactive"), newInactive);
        return true;
    }

    @Override
    public Secret fetchSecret(SecretReference ref, AccessCredential credential) throws SecretAccessException {
        // Simulate network delay
        simulateDelay();
        
        // Simulate random failures
        simulateFailure(ref);
        
        String key = createKey(ref.getName(), ref.getVersionHint());
        Secret secret = secrets.get(key);
        
        if (secret == null) {
            throw new SecretAccessException("Secret not found: " + ref.getName(), ref);
        }
        
        // Return a copy of the secret with updated timestamp
        return new Secret(
                secret.getId(),
                secret.getName(),
                secret.getValue(),
                new SecretMetadata(
                        secret.getMetadata().getVersion(),
                        Instant.now(),
                        secret.getMetadata().getStoreType(),
                        secret.getMetadata().getSourceRef()
                )
        );
    }

    @Override
    public boolean supportsStore(StoreType type) {
        return type == supportedStoreType;
    }

    @Override
    public Optional<String> getLatestVersion(SecretReference ref, AccessCredential credential) 
            throws SecretAccessException {
        // Simulate network delay (shorter than full fetch)
        simulateDelay(minDelayMs / 2, maxDelayMs / 2);
        
        // Simulate random failures
        simulateFailure(ref);
        
        String key = createKey(ref.getName(), ref.getVersionHint());
        Secret secret = secrets.get(key);
        
        if (secret == null) {
            return Optional.empty();
        }
        
        return Optional.of(secret.getMetadata().getVersion());
    }
    
    private void simulateDelay() {
        simulateDelay(minDelayMs, maxDelayMs);
    }
    
    private void simulateDelay(int min, int max) {
        try {
            int delay = ThreadLocalRandom.current().nextInt(min, max + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateFailure(SecretReference ref) {
        if (failureProbability > 0 && 
                ThreadLocalRandom.current().nextDouble() < failureProbability) {
            throw new SecretAccessException(
                    "Simulated failure accessing secret: " + ref.getName(),
                    ref);
        }
    }
    
    private String createKey(String name, String versionHint) {
        return name + ":" + versionHint;
    }
}
