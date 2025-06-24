package com.secrets.api;

import com.secrets.application.port.EventPublisher;
import com.secrets.application.service.DefaultSecretRefreshCoordinator;
import com.secrets.application.service.DefaultSecretResolverService;
import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import com.secrets.domain.service.RefreshPolicy;
import com.secrets.domain.service.SecretAccessException;
import com.secrets.domain.service.SecretCache;
import com.secrets.domain.service.SecretProvider;
import com.secrets.domain.service.SecretRefreshCoordinator;
import com.secrets.domain.service.SecretResolverService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Main entry point for the Java Secrets Library.
 * This facade provides a simplified API for accessing secrets from various providers.
 */
public class SecretManager implements AutoCloseable {

    private final SecretResolverService secretResolver;
    private final SecretRefreshCoordinator refreshCoordinator;
    private final Map<String, SecretReference> registeredSecrets = new HashMap<>();
    private final Map<String, AccessCredential> credentials = new HashMap<>();

    /**
     * Creates a new SecretManager with the provided components.
     * This constructor is used by the Builder to create a customized instance.
     *
     * @param secretResolver     The service to resolve secrets
     * @param refreshCoordinator The coordinator for secret refresh operations
     */
    private SecretManager(SecretResolverService secretResolver, SecretRefreshCoordinator refreshCoordinator) {
        this.secretResolver = secretResolver;
        this.refreshCoordinator = refreshCoordinator;
        
        // Start the refresh coordinator
        this.refreshCoordinator.start();
    }

    /**
     * Gets a secret by its name.
     * This uses the stored reference and credential for the secret.
     *
     * @param secretName The name of the secret to get
     * @return The secret value
     * @throws SecretAccessException if the secret cannot be accessed
     * @throws IllegalArgumentException if the secret name is not registered
     */
    public Secret getSecret(String secretName) throws SecretAccessException {
        SecretReference reference = registeredSecrets.get(secretName);
        if (reference == null) {
            throw new IllegalArgumentException("Secret not registered: " + secretName);
        }
        
        AccessCredential credential = credentials.get(secretName);
        if (credential == null) {
            throw new IllegalArgumentException("No credential found for secret: " + secretName);
        }
        
        return secretResolver.resolveSecret(reference, credential);
    }
    
    /**
     * Gets a secret's value as a string.
     * WARNING: This returns the secret as a string, which is less secure than using the Secret class.
     * Only use this when necessary and ensure proper handling of sensitive data.
     *
     * @param secretName The name of the secret to get
     * @return The secret value as a string
     * @throws SecretAccessException if the secret cannot be accessed
     * @throws IllegalArgumentException if the secret name is not registered
     */
    public String getSecretAsString(String secretName) throws SecretAccessException {
        try (Secret.AutoClearingSecret autoSecret = new Secret.AutoClearingSecret(getSecret(secretName))) {
            return new String(autoSecret.getSecret().getValue());
        }
    }

    /**
     * Refreshes a secret by its name.
     *
     * @param secretName The name of the secret to refresh
     * @return The refreshed secret
     * @throws SecretAccessException if the secret cannot be refreshed
     * @throws IllegalArgumentException if the secret name is not registered
     */
    public Secret refreshSecret(String secretName) throws SecretAccessException {
        SecretReference reference = registeredSecrets.get(secretName);
        if (reference == null) {
            throw new IllegalArgumentException("Secret not registered: " + secretName);
        }
        
        AccessCredential credential = credentials.get(secretName);
        if (credential == null) {
            throw new IllegalArgumentException("No credential found for secret: " + secretName);
        }
        
        return secretResolver.refreshSecret(reference, credential);
    }
    
    /**
     * Registers a secret for use with this SecretManager.
     *
     * @param secretName    The name to register the secret under
     * @param reference     The reference to the secret
     * @param credential    The credential to use for accessing the secret
     * @param refreshPolicy The policy to use for refreshing this secret
     * @throws IllegalArgumentException if the secret name is already registered
     */
    public void registerSecret(
            String secretName, 
            SecretReference reference, 
            AccessCredential credential,
            RefreshPolicy refreshPolicy) {
        if (registeredSecrets.containsKey(secretName)) {
            throw new IllegalArgumentException("Secret already registered: " + secretName);
        }
        
        registeredSecrets.put(secretName, reference);
        credentials.put(secretName, credential);
        
        // Register with the refresh coordinator
        if (refreshPolicy != null) {
            refreshCoordinator.registerSecret(reference, refreshPolicy);
        }
    }
    
    /**
     * Unregisters a secret from this SecretManager.
     *
     * @param secretName The name of the secret to unregister
     */
    public void unregisterSecret(String secretName) {
        SecretReference reference = registeredSecrets.remove(secretName);
        credentials.remove(secretName);
        
        if (reference != null) {
            refreshCoordinator.unregisterSecret(reference);
        }
    }

    /**
     * Closes this SecretManager and releases any resources.
     */
    @Override
    public void close() {
        // Stop the refresh coordinator
        refreshCoordinator.stop();
        
        // Shutdown the secret resolver
        if (secretResolver instanceof AutoCloseable) {
            try {
                ((AutoCloseable) secretResolver).close();
            } catch (Exception e) {
                // Log and ignore
                System.err.println("Error closing secret resolver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Creates a new builder for configuring a SecretManager.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for creating SecretManager instances with custom configuration.
     */
    public static class Builder {
        private final List<SecretProvider> providers = new ArrayList<>();
        private SecretCache cache;
        private RefreshPolicy defaultRefreshPolicy;
        private EventPublisher eventPublisher;
        private Duration defaultCacheTtl = Duration.ofMinutes(15);
        
        /**
         * Adds a secret provider to this builder.
         *
         * @param provider The provider to add
         * @return This builder
         */
        public Builder withProvider(SecretProvider provider) {
            providers.add(provider);
            return this;
        }
        
        /**
         * Sets the cache to use for secrets.
         *
         * @param cache The cache to use
         * @return This builder
         */
        public Builder withCache(SecretCache cache) {
            this.cache = cache;
            return this;
        }
        
        /**
         * Sets the default refresh policy to use for secrets.
         *
         * @param refreshPolicy The refresh policy to use
         * @return This builder
         */
        public Builder withDefaultRefreshPolicy(RefreshPolicy refreshPolicy) {
            this.defaultRefreshPolicy = refreshPolicy;
            return this;
        }
        
        /**
         * Sets the event publisher to use for domain events.
         *
         * @param eventPublisher The event publisher to use
         * @return This builder
         */
        public Builder withEventPublisher(EventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
            return this;
        }
        
        /**
         * Sets the default cache TTL for secrets.
         *
         * @param ttl The time-to-live duration
         * @return This builder
         */
        public Builder withDefaultCacheTtl(Duration ttl) {
            this.defaultCacheTtl = ttl;
            return this;
        }
        
        /**
         * Adds an event listener for a specific event type.
         * This requires an event publisher to be set.
         *
         * @param eventType The class of events to listen for
         * @param listener  The listener to receive events
         * @param <T>       The event type
         * @return This builder
         */
        public <T> Builder withEventListener(Class<T> eventType, Consumer<T> listener) {
            // We'll set this up when we build if an event publisher is provided
            return this;
        }
        
        /**
         * Builds and returns a configured SecretManager.
         *
         * @return A new SecretManager instance
         */
        public SecretManager build() {
            // Create or use provided components
            EventPublisher publisher = eventPublisher != null ? 
                    eventPublisher : event -> {};
            
            // If no cache was provided, create an error (we don't want to automatically
            // create one without the client being aware)
            if (cache == null) {
                throw new IllegalArgumentException(
                        "No cache provided. Please configure a cache using withCache().");
            }
            
            // Set default TTL on cache
            cache.setDefaultTTL(defaultCacheTtl);
            
            // Create the resolver service
            DefaultSecretResolverService resolverService = 
                    new DefaultSecretResolverService(cache, defaultRefreshPolicy, publisher);
            
            // Register all providers
            for (SecretProvider provider : providers) {
                resolverService.registerProvider(provider);
            }
            
            // Create the refresh coordinator
            DefaultSecretRefreshCoordinator refreshCoordinator =
                    new DefaultSecretRefreshCoordinator(resolverService, publisher);
            
            return new SecretManager(resolverService, refreshCoordinator);
        }
    }
}
