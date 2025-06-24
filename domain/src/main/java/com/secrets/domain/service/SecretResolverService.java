package com.secrets.domain.service;

import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretReference;

/**
 * Domain service interface for resolving secrets from various providers.
 * This service is responsible for routing secret requests to the appropriate provider
 * and applying business rules like caching, refresh policies, and rollover logic.
 */
public interface SecretResolverService {

    /**
     * Resolves a secret from the appropriate provider based on the reference.
     *
     * @param reference  The reference to the secret to resolve
     * @param credential The credentials to use for accessing the secret
     * @return The resolved secret
     * @throws SecretAccessException if the secret cannot be accessed
     */
    Secret resolveSecret(SecretReference reference, AccessCredential credential) throws SecretAccessException;

    /**
     * Refreshes a secret from its source, bypassing any caching.
     *
     * @param reference  The reference to the secret to refresh
     * @param credential The credentials to use for accessing the secret
     * @return The refreshed secret
     * @throws SecretAccessException if the secret cannot be accessed
     */
    Secret refreshSecret(SecretReference reference, AccessCredential credential) throws SecretAccessException;

    /**
     * Registers a provider with this resolver service.
     * The provider will be used to resolve secrets from stores it supports.
     *
     * @param provider The provider to register
     */
    void registerProvider(SecretProvider provider);
    
    /**
     * Stops all background processes and clears any resources used by this service.
     */
    void shutdown();
}
