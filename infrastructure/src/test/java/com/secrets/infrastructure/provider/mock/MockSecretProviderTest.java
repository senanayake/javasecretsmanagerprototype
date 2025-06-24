package com.secrets.infrastructure.provider.mock;

import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import com.secrets.domain.service.SecretAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockSecretProviderTest {

    private MockSecretProvider provider;
    private AccessCredential dummyCredential;

    @BeforeEach
    void setUp() {
        provider = MockSecretProvider.builder()
                .withStoreType(StoreType.AWS_SECRETS_MANAGER)
                .withDelayRange(1, 5) // Minimal delay for tests
                .withFailureProbability(0.0) // No failures in tests
                .build();
        
        dummyCredential = AccessCredential.forCyberArkApiKey("dummy-key");
    }

    @Test
    void shouldReturnExistingSecret() throws SecretAccessException {
        // Given
        String secretName = "test-secret";
        String secretValue = "test-value";
        provider.addSecret(secretName, secretValue);
        
        SecretReference reference = new SecretReference(
                StoreType.AWS_SECRETS_MANAGER, secretName, "latest");
        
        // When
        Secret secret = provider.fetchSecret(reference, dummyCredential);
        
        // Then
        assertThat(secret).isNotNull();
        assertThat(secret.getName()).isEqualTo(secretName);
        assertThat(new String(secret.getValue())).isEqualTo(secretValue);
        assertThat(secret.getMetadata().getStoreType()).isEqualTo(StoreType.AWS_SECRETS_MANAGER);
        assertThat(secret.getMetadata().getSourceRef()).isEqualTo(reference);
    }
    
    @Test
    void shouldReturnSecretWithSpecificVersionHint() throws SecretAccessException {
        // Given
        String secretName = "versioned-secret";
        String versionHint = "v1";
        String secretValue = "secret-value-v1";
        provider.addSecret(secretName, secretValue, versionHint);
        
        SecretReference reference = new SecretReference(
                StoreType.AWS_SECRETS_MANAGER, secretName, versionHint);
        
        // When
        Secret secret = provider.fetchSecret(reference, dummyCredential);
        
        // Then
        assertThat(secret).isNotNull();
        assertThat(new String(secret.getValue())).isEqualTo(secretValue);
    }
    
    @Test
    void shouldThrowExceptionForNonExistentSecret() {
        // Given
        SecretReference reference = new SecretReference(
                StoreType.AWS_SECRETS_MANAGER, "non-existent-secret", "latest");
        
        // When/Then
        assertThatThrownBy(() -> provider.fetchSecret(reference, dummyCredential))
                .isInstanceOf(SecretAccessException.class)
                .hasMessageContaining("not found");
    }
    
    @Test
    void shouldSupportCorrectStoreType() {
        // When/Then
        assertThat(provider.supportsStore(StoreType.AWS_SECRETS_MANAGER)).isTrue();
        assertThat(provider.supportsStore(StoreType.CYBERARK)).isFalse();
    }
    
    @Test
    void shouldUpdateExistingSecret() throws SecretAccessException {
        // Given
        String secretName = "update-test";
        String originalValue = "original-value";
        String updatedValue = "updated-value";
        
        provider.addSecret(secretName, originalValue);
        
        SecretReference reference = new SecretReference(
                StoreType.AWS_SECRETS_MANAGER, secretName, "latest");
        
        // When
        boolean updated = provider.updateSecret(secretName, updatedValue);
        Secret secret = provider.fetchSecret(reference, dummyCredential);
        
        // Then
        assertThat(updated).isTrue();
        assertThat(new String(secret.getValue())).isEqualTo(updatedValue);
    }
    
    @Test
    void shouldReturnFalseWhenUpdatingNonExistentSecret() {
        // When
        boolean result = provider.updateSecret("non-existent", "value");
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void shouldPerformRolloverCorrectly() throws SecretAccessException {
        // Given
        String secretName = "rollover-test";
        String activeValue = "active-value";
        String inactiveValue = "inactive-value";
        
        provider.addSecret(secretName, activeValue, "active");
        provider.addSecret(secretName, inactiveValue, "inactive");
        
        SecretReference activeRef = new SecretReference(
                StoreType.AWS_SECRETS_MANAGER, secretName, "active");
        SecretReference inactiveRef = new SecretReference(
                StoreType.AWS_SECRETS_MANAGER, secretName, "inactive");
        
        // Verify initial state
        assertThat(new String(provider.fetchSecret(activeRef, dummyCredential).getValue()))
                .isEqualTo(activeValue);
        assertThat(new String(provider.fetchSecret(inactiveRef, dummyCredential).getValue()))
                .isEqualTo(inactiveValue);
        
        // When
        boolean result = provider.performRollover(secretName);
        
        // Then
        assertThat(result).isTrue();
        
        // After rollover, the values should be swapped
        assertThat(new String(provider.fetchSecret(activeRef, dummyCredential).getValue()))
                .isEqualTo(inactiveValue);
        assertThat(new String(provider.fetchSecret(inactiveRef, dummyCredential).getValue()))
                .isEqualTo(activeValue);
    }
    
    @Test
    void shouldReturnFalseForRolloverWithMissingVersions() {
        // Given
        String secretName = "incomplete-rollover";
        provider.addSecret(secretName, "value", "active");
        // Note: No "inactive" version
        
        // When
        boolean result = provider.performRollover(secretName);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void shouldGetLatestVersionForExistingSecret() throws SecretAccessException {
        // Given
        String secretName = "versioned-secret";
        provider.addSecret(secretName, "value");
        
        SecretReference reference = new SecretReference(
                StoreType.AWS_SECRETS_MANAGER, secretName, "latest");
        
        // When
        Optional<String> version = provider.getLatestVersion(reference, dummyCredential);
        
        // Then
        assertThat(version).isPresent();
    }
    
    @Test
    void shouldReturnEmptyForNonExistentSecretVersion() throws SecretAccessException {
        // Given
        SecretReference reference = new SecretReference(
                StoreType.AWS_SECRETS_MANAGER, "non-existent", "latest");
        
        // When
        Optional<String> version = provider.getLatestVersion(reference, dummyCredential);
        
        // Then
        assertThat(version).isEmpty();
    }
}
