package com.secrets.api;

import com.secrets.application.port.EventPublisher;
import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.CredentialMethod;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretMetadata;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import com.secrets.domain.service.RefreshPolicy;
import com.secrets.domain.service.SecretAccessException;
import com.secrets.domain.service.SecretCache;
import com.secrets.domain.service.SecretProvider;
import com.secrets.domain.service.SecretRefreshCoordinator;
import com.secrets.domain.service.SecretResolverService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecretManagerTest {

    private SecretManager secretManager;
    private SecretResolverService mockResolver;
    private SecretRefreshCoordinator mockCoordinator;
    private SecretProvider mockProvider;
    private SecretCache mockCache;
    private EventPublisher mockEventPublisher;
    
    private SecretReference testRef;
    private AccessCredential testCredential;
    private Secret testSecret;
    private RefreshPolicy mockRefreshPolicy;

    @BeforeEach
    void setUp() {
        // Set up mocks
        mockResolver = mock(SecretResolverService.class);
        mockCoordinator = mock(SecretRefreshCoordinator.class);
        mockProvider = mock(SecretProvider.class);
        mockCache = mock(SecretCache.class);
        mockEventPublisher = mock(EventPublisher.class);
        mockRefreshPolicy = mock(RefreshPolicy.class);
        
        // Set up test data
        testRef = new SecretReference(StoreType.AWS_SECRETS_MANAGER, "test-secret", "latest");
        testCredential = AccessCredential.forCyberArkApiKey("test-api-key");
        
        SecretMetadata metadata = new SecretMetadata(
                UUID.randomUUID().toString(),
                Instant.now(),
                StoreType.AWS_SECRETS_MANAGER,
                testRef);
        
        testSecret = new Secret(
                UUID.randomUUID().toString(),
                "test-secret",
                "secret-value".toCharArray(),
                metadata);
        
        // Create SecretManager with mocks using reflection for testing
        // (The constructor is private, so we'd normally need to use the builder)
        secretManager = new SecretManagerTestHelper()
                .createSecretManager(mockResolver, mockCoordinator);
    }

    @AfterEach
    void tearDown() throws Exception {
        secretManager.close();
    }

    @Test
    void shouldRegisterAndGetSecret() throws SecretAccessException {
        // Given
        when(mockResolver.resolveSecret(testRef, testCredential)).thenReturn(testSecret);
        
        // When
        secretManager.registerSecret("secret-name", testRef, testCredential, mockRefreshPolicy);
        Secret result = secretManager.getSecret("secret-name");
        
        // Then
        assertThat(result).isEqualTo(testSecret);
        verify(mockResolver).resolveSecret(testRef, testCredential);
        verify(mockCoordinator).registerSecret(testRef, mockRefreshPolicy);
    }
    
    @Test
    void shouldGetSecretAsString() throws SecretAccessException {
        // Given
        when(mockResolver.resolveSecret(testRef, testCredential)).thenReturn(testSecret);
        secretManager.registerSecret("secret-name", testRef, testCredential, mockRefreshPolicy);
        
        // When
        String result = secretManager.getSecretAsString("secret-name");
        
        // Then
        assertThat(result).isEqualTo("secret-value");
    }
    
    @Test
    void shouldThrowExceptionForUnregisteredSecret() {
        // When/Then
        assertThatThrownBy(() -> secretManager.getSecret("non-existent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not registered");
    }
    
    @Test
    void shouldRefreshSecret() throws SecretAccessException {
        // Given
        when(mockResolver.refreshSecret(testRef, testCredential)).thenReturn(testSecret);
        secretManager.registerSecret("secret-name", testRef, testCredential, mockRefreshPolicy);
        
        // When
        Secret result = secretManager.refreshSecret("secret-name");
        
        // Then
        assertThat(result).isEqualTo(testSecret);
        verify(mockResolver).refreshSecret(testRef, testCredential);
    }
    
    @Test
    void shouldThrowExceptionWhenRefreshingUnregisteredSecret() {
        // When/Then
        assertThatThrownBy(() -> secretManager.refreshSecret("non-existent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not registered");
    }
    
    @Test
    void shouldUnregisterSecret() throws SecretAccessException {
        // Given
        secretManager.registerSecret("secret-name", testRef, testCredential, mockRefreshPolicy);
        
        // When
        secretManager.unregisterSecret("secret-name");
        
        // Then
        assertThatThrownBy(() -> secretManager.getSecret("secret-name"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mockCoordinator).unregisterSecret(testRef);
    }
    
    @Test
    void shouldThrowExceptionForExistingSecretNameOnRegister() {
        // Given
        secretManager.registerSecret("secret-name", testRef, testCredential, mockRefreshPolicy);
        
        // When/Then
        assertThatThrownBy(() -> 
                secretManager.registerSecret("secret-name", testRef, testCredential, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }
    
    @Test
    void shouldBuildSecretManagerWithRequiredComponents() {
        // When
        SecretManager manager = null;
        try {
            manager = SecretManager.builder()
                    .withProvider(mockProvider)
                    .withCache(mockCache)
                    .withEventPublisher(mockEventPublisher)
                    .withDefaultRefreshPolicy(mockRefreshPolicy)
                    .withDefaultCacheTtl(Duration.ofMinutes(5))
                    .build();
            
            // Then
            assertThat(manager).isNotNull();
        } finally {
            if (manager != null) {
                try {
                    manager.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }
    
    @Test
    void shouldThrowExceptionWhenBuildingWithoutCache() {
        // When/Then
        assertThatThrownBy(() -> SecretManager.builder()
                .withProvider(mockProvider)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No cache provided");
    }
    
    /**
     * Helper class for testing SecretManager with constructor injection
     * since the constructor is private and normally accessed via the builder
     */
    private static class SecretManagerTestHelper extends SecretManager {
        public SecretManagerTestHelper() {
            super(null, null);
        }
        
        public SecretManager createSecretManager(
                SecretResolverService resolver, 
                SecretRefreshCoordinator coordinator) {
            try {
                java.lang.reflect.Constructor<SecretManager> constructor = 
                        SecretManager.class.getDeclaredConstructor(
                                SecretResolverService.class, 
                                SecretRefreshCoordinator.class);
                constructor.setAccessible(true);
                return constructor.newInstance(resolver, coordinator);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create SecretManager for testing", e);
            }
        }
    }
}
