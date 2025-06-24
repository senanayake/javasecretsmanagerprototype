package com.secrets.domain.service;

import com.secrets.domain.event.SecretRefreshRequested;
import com.secrets.domain.model.SecretReference;

/**
 * Domain service interface for coordinating refresh operations across multiple secrets.
 * This service is responsible for scheduling refreshes, handling external refresh events,
 * and coordinating refresh activities across multiple secrets.
 */
public interface SecretRefreshCoordinator {

    /**
     * Registers a secret reference for monitoring and refreshing.
     *
     * @param reference     The reference to the secret to monitor
     * @param refreshPolicy The policy to use for refreshing this secret
     */
    void registerSecret(SecretReference reference, RefreshPolicy refreshPolicy);

    /**
     * Unregisters a secret reference from monitoring.
     *
     * @param reference The reference to the secret to stop monitoring
     */
    void unregisterSecret(SecretReference reference);

    /**
     * Triggers a refresh for a specific secret.
     *
     * @param reference The reference to the secret to refresh
     * @param reason    The reason for the refresh request
     * @return true if the refresh was successfully triggered, false otherwise
     */
    boolean triggerRefresh(SecretReference reference, String reason);

    /**
     * Handles an external refresh event.
     * This could be from an AWS EventBridge notification, CyberArk webhook, etc.
     *
     * @param event The refresh event to handle
     */
    void handleRefreshEvent(SecretRefreshRequested event);

    /**
     * Starts the coordinator and any associated background processes.
     */
    void start();

    /**
     * Stops the coordinator and any associated background processes.
     */
    void stop();

    /**
     * Checks if the coordinator is running.
     *
     * @return true if the coordinator is running, false otherwise
     */
    boolean isRunning();
}
