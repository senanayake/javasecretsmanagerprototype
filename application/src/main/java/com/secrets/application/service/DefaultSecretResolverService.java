package com.secrets.application.service;

import com.secrets.application.port.EventPublisher;
import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretProviderAggregate;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import com.secrets.domain.service.RefreshPolicy;
import com.secrets.domain.service.SecretAccessException;
import com.secrets.domain.service.SecretCache;
import com.secrets.domain.service.SecretProvider;
import com.secrets.domain.service.SecretResolverService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of SecretResolverService that routes secret requests
 * to the appropriate provider and applies caching and refresh policies.
 */
public class DefaultSecretResolverService implements SecretResolverService {

    private final List<SecretProvider> providers = new ArrayList<>();
    private final SecretCache cache;
    private final RefreshPolicy defaultRefreshPolicy;
    private final EventPublisher eventPublisher;
    private final Map<SecretReference, SecretProviderAggregate> aggregates = new ConcurrentHashMap<>();

    /**
     * Creates a new DefaultSecretResolverService.
     *
     * @param cache The cache to use for storing secrets
     * @param defaultRefreshPolicy The default refresh policy to use
     * @param eventPublisher The publisher to use for domain events
     */
    public DefaultSecretResolverService(
            SecretCache cache,
            RefreshPolicy defaultRefreshPolicy,
            EventPublisher eventPublisher) {
        this.cache = cache;
        this.defaultRefreshPolicy = defaultRefreshPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Secret resolveSecret(SecretReference reference, AccessCredential credential) throws SecretAccessException {
        SecretProviderAggregate aggregate = getOrCreateAggregate(reference, credential);
        return aggregate.getSecret();
    }

    @Override
    public Secret refreshSecret(SecretReference reference, AccessCredential credential) throws SecretAccessException {
        SecretProviderAggregate aggregate = getOrCreateAggregate(reference, credential);
        return aggregate.refreshSecret();
    }

    @Override
    public void registerProvider(SecretProvider provider) {
        providers.add(provider);
    }

    @Override
    public void shutdown() {
        aggregates.values().forEach(SecretProviderAggregate::stop);
        aggregates.clear();
    }

    private SecretProviderAggregate getOrCreateAggregate(SecretReference reference, AccessCredential credential) {
        return aggregates.computeIfAbsent(reference, ref -> {
            SecretProvider provider = findProviderForStore(ref.getStoreType());
            if (provider == null) {
                throw new SecretAccessException(
                        "No provider available for store type " + ref.getStoreType(),
                        ref);
            }
            return new SecretProviderAggregate(
                    ref,
                    provider,
                    cache,
                    defaultRefreshPolicy,
                    credential,
                    eventPublisher::publish);
        });
    }

    private SecretProvider findProviderForStore(StoreType storeType) {
        return providers.stream()
                .filter(provider -> provider.supportsStore(storeType))
                .findFirst()
                .orElse(null);
    }
}
