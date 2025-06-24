package com.secrets.domain.service;

import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretReference;

/**
 * Interface defining operations for secret refresh policies.
 * Implementations will determine how and when secrets are refreshed from their source.
 */
public interface RefreshPolicy {
    
    /**
     * Applies this refresh policy to a secret provider and cache.
     * This method typically starts any background processes or listeners
     * necessary to implement the refresh strategy.
     *
     * @param secretProvider The provider to fetch secrets from
     * @param secretCache The cache to store and invalidate secrets
     */
    void apply(SecretProvider secretProvider, SecretCache secretCache);
    
    /**
     * Checks if a refresh is needed for a specific secret.
     * 
     * @param reference The reference to the secret to check
     * @param cachedSecret The currently cached secret, if available
     * @return true if the secret should be refreshed, false otherwise
     */
    boolean isRefreshNeeded(SecretReference reference, Secret cachedSecret);
    
    /**
     * Triggers an immediate refresh for a specific secret.
     * 
     * @param reference The reference to the secret to refresh
     */
    void triggerRefresh(SecretReference reference);
    
    /**
     * Starts any background processes or listeners required by this policy.
     */
    void start();
    
    /**
     * Stops any background processes or listeners started by this policy.
     */
    void stop();
    
    /**
     * Checks if this policy is running.
     * 
     * @return true if the policy is actively monitoring or refreshing secrets, false otherwise
     */
    boolean isRunning();
}
