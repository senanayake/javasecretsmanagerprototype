package com.secrets.application.service;

import com.secrets.application.port.EventPublisher;
import com.secrets.domain.event.SecretRefreshRequested;
import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.service.RefreshPolicy;
import com.secrets.domain.service.SecretRefreshCoordinator;
import com.secrets.domain.service.SecretResolverService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of SecretRefreshCoordinator that coordinates
 * refresh operations across multiple secrets.
 */
public class DefaultSecretRefreshCoordinator implements SecretRefreshCoordinator {

    private final SecretResolverService secretResolver;
    private final EventPublisher eventPublisher;
    private final Map<SecretReference, RefreshContext> refreshContexts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Represents the context for a secret being coordinated for refresh.
     */
    private static class RefreshContext {
        final RefreshPolicy refreshPolicy;
        final AccessCredential credential;

        RefreshContext(RefreshPolicy refreshPolicy, AccessCredential credential) {
            this.refreshPolicy = refreshPolicy;
            this.credential = credential;
        }
    }

    /**
     * Creates a new DefaultSecretRefreshCoordinator.
     *
     * @param secretResolver The service used to resolve and refresh secrets
     * @param eventPublisher The publisher used for domain events
     */
    public DefaultSecretRefreshCoordinator(
            SecretResolverService secretResolver,
            EventPublisher eventPublisher) {
        this.secretResolver = secretResolver;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void registerSecret(SecretReference reference, RefreshPolicy refreshPolicy) {
        // We'll need credentials later for the actual refresh,
        // but this is just a mock implementation
        refreshContexts.put(reference, new RefreshContext(refreshPolicy, null));
    }

    @Override
    public void unregisterSecret(SecretReference reference) {
        refreshContexts.remove(reference);
    }

    @Override
    public boolean triggerRefresh(SecretReference reference, String reason) {
        if (!refreshContexts.containsKey(reference)) {
            return false;
        }

        // Publish refresh requested event
        eventPublisher.publish(new SecretRefreshRequested(reference, reason));
        
        // In a real implementation, this would trigger the actual refresh
        // through the RefreshPolicy for this secret
        RefreshContext context = refreshContexts.get(reference);
        if (context != null && context.credential != null) {
            try {
                secretResolver.refreshSecret(reference, context.credential);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        return false;
    }

    @Override
    public void handleRefreshEvent(SecretRefreshRequested event) {
        SecretReference reference = event.getSecretReference();
        RefreshContext context = refreshContexts.get(reference);
        
        if (context != null && context.credential != null) {
            try {
                secretResolver.refreshSecret(reference, context.credential);
            } catch (Exception e) {
                // In a real implementation, we would log this error and possibly retry
            }
        }
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            // In a real implementation, this would set up listeners for external events
            // and schedule periodic polling for secrets with polling-based refresh policies
            
            // For example, schedule a task to check for refreshes every minute
            scheduler.scheduleAtFixedRate(() -> {
                refreshContexts.forEach((reference, context) -> {
                    // This is a simplified implementation. In reality, we would use the
                    // refresh policy to determine if a refresh is needed.
                    if (context.refreshPolicy != null) {
                        triggerRefresh(reference, "Scheduled check");
                    }
                });
            }, 1, 1, TimeUnit.MINUTES);
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
