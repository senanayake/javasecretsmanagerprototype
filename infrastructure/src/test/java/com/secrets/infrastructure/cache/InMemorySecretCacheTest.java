package com.secrets.infrastructure.cache;

import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretMetadata;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySecretCacheTest {

    private InMemorySecretCache cache;
    private SecretReference reference;
    private Secret secret;

    @BeforeEach
    void setUp() {
        cache = new InMemorySecretCache();
        reference = new SecretReference(StoreType.AWS_SECRETS_MANAGER, "test-secret", "latest");
        
        SecretMetadata metadata = new SecretMetadata(
                UUID.randomUUID().toString(),
                Instant.now(),
                StoreType.AWS_SECRETS_MANAGER,
                reference);
        
        secret = new Secret(
                UUID.randomUUID().toString(),
                "test-secret",
                "secret-value".toCharArray(),
                metadata);
    }

    @Test
    void shouldReturnEmptyForNonExistentSecret() {
        // When
        Optional<Secret> result = cache.get(reference);
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldStoreAndRetrieveSecret() {
        // When
        cache.put(secret);
        Optional<Secret> result = cache.get(reference);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(secret);
    }

    @Test
    void shouldReturnEmptyForExpiredSecret() throws Exception {
        // Given: Set a very short TTL
        cache.setDefaultTTL(Duration.ofMillis(100));
        
        // When
        cache.put(secret);
        
        // Wait for the secret to expire
        TimeUnit.MILLISECONDS.sleep(200);
        
        Optional<Secret> result = cache.get(reference);
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldUseSecretSpecificTTL() throws Exception {
        // Given: Set a very short default TTL but a longer specific TTL
        cache.setDefaultTTL(Duration.ofMillis(100));
        cache.setTTL(reference, Duration.ofSeconds(1));
        
        // When
        cache.put(secret);
        
        // Wait longer than the default TTL
        TimeUnit.MILLISECONDS.sleep(200);
        
        // The secret should still be available
        Optional<Secret> result = cache.get(reference);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(secret);
    }

    @Test
    void shouldInvalidateSpecificSecret() {
        // Given
        cache.put(secret);
        
        // When
        cache.invalidate(reference);
        Optional<Secret> result = cache.get(reference);
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldClearAllSecrets() {
        // Given
        cache.put(secret);
        
        // Create a second secret
        SecretReference ref2 = new SecretReference(StoreType.CYBERARK, "another-secret", "latest");
        SecretMetadata metadata2 = new SecretMetadata(
                UUID.randomUUID().toString(),
                Instant.now(),
                StoreType.CYBERARK,
                ref2);
        
        Secret secret2 = new Secret(
                UUID.randomUUID().toString(),
                "another-secret",
                "another-value".toCharArray(),
                metadata2);
        
        cache.put(secret2);
        
        // When
        cache.clear();
        
        // Then
        assertThat(cache.get(reference)).isEmpty();
        assertThat(cache.get(ref2)).isEmpty();
    }

    @Test
    void shouldReportStaleStatusCorrectly() throws Exception {
        // Given
        cache.setDefaultTTL(Duration.ofMillis(100));
        cache.put(secret);
        
        // When/Then - Initially not stale
        assertThat(cache.isStale(reference)).isFalse();
        
        // Wait for the secret to expire
        TimeUnit.MILLISECONDS.sleep(200);
        
        // Now it should be stale
        assertThat(cache.isStale(reference)).isTrue();
    }

    @Test
    void shouldGetAndSetDefaultTTL() {
        // Given
        Duration newTTL = Duration.ofMinutes(30);
        
        // When
        cache.setDefaultTTL(newTTL);
        
        // Then
        assertThat(cache.getDefaultTTL()).isEqualTo(newTTL);
    }

    @Test
    void shouldReportNonExistentSecretAsStale() {
        // When/Then
        SecretReference nonExistentRef = new SecretReference(
                StoreType.AWS_SECRETS_MANAGER, "non-existent", "latest");
                
        assertThat(cache.isStale(nonExistentRef)).isTrue();
    }
}
