package com.secrets.domain.service;

import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;

import java.util.Optional;

/**
 * Interface defining operations for secret providers.
 * Implementations will connect to specific secret stores like AWS Secrets Manager or CyberArk.
 */
public interface SecretProvider {
    
    /**
     * Fetches a secret from the underlying store.
     *
     * @param ref        The reference to the secret to fetch
     * @param credential The credentials to use for accessing the secret
     * @return The fetched secret
     * @throws SecretAccessException if the secret cannot be fetched
     */
    Secret fetchSecret(SecretReference ref, AccessCredential credential) throws SecretAccessException;
    
    /**
     * Checks if this provider can handle secrets from the specified store type.
     *
     * @param type The store type to check
     * @return true if this provider supports the specified store type, false otherwise
     */
    boolean supportsStore(StoreType type);
    
    /**
     * Gets the latest version identifier for a secret.
     * This can be used to check if a secret has been updated without fetching the full value.
     *
     * @param ref        The reference to the secret
     * @param credential The credentials to use for accessing the secret
     * @return An Optional containing the version identifier, or empty if not supported
     * @throws SecretAccessException if there is an error accessing the version information
     */
    default Optional<String> getLatestVersion(SecretReference ref, AccessCredential credential) 
            throws SecretAccessException {
        return Optional.empty();
    }
    
    /**
     * Checks if the provider supports notifications for secret changes.
     *
     * @return true if change notifications are supported, false otherwise
     */
    default boolean supportsChangeNotifications() {
        return false;
    }
}
