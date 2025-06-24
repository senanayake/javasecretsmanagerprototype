package com.secrets.application.service;

import com.secrets.application.port.EventPublisher;
import com.secrets.domain.event.SecretRefreshed;
import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretMetadata;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import com.secrets.domain.service.RefreshPolicy;
import com.secrets.domain.service.SecretAccessException;
import com.secrets.domain.service.SecretCache;
import com.secrets.domain.service.SecretProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DefaultSecretResolverServiceTest {

    private DefaultSecretResolverService service;
    private SecretCache mockCache;
    private SecretProvider mockProvider;
    private RefreshPolicy mockRefreshPolicy;
    private EventPublisher mockEventPublisher;
    
    private SecretReference testRef;
    private AccessCredential testCredential;
    private Secret testSecret;

    @BeforeEach
    void setUp() {
        mockCache = mock(SecretCache.class);
        mockProvider = mock(SecretProvider.class);
        mockRefreshPolicy = mock(RefreshPolicy.class);
        mockEventPublisher = mock(EventPublisher.class);
        
        service = new DefaultSecretResolverService(mockCache, mockRefreshPolicy, mockEventPublisher);
        service.registerProvider(mockProvider);
        
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
        
        // Set up common mocks
        when(mockProvider.supportsStore(StoreType.AWS_SECRETS_MANAGER)).thenReturn(true);
    }

    @Test
    void shouldReturnCachedSecretWhenAvailable() throws SecretAccessException {
        // Given
        when(mockCache.get(testRef)).thenReturn(Optional.of(testSecret));
        
        // When
        Secret result = service.resolveSecret(testRef, testCredential);
        
        // Then
        assertThat(result).isEqualTo(testSecret);
        verify(mockCache).get(testRef);
        verifyNoMoreInteractions(mockProvider); // Provider should not be called
    }
    
    @Test
    void shouldFetchFromProviderWhenNotInCache() throws SecretAccessException {
        // Given
        when(mockCache.get(testRef)).thenReturn(Optional.empty());
        when(mockProvider.fetchSecret(testRef, testCredential)).thenReturn(testSecret);
        
        // When
        Secret result = service.resolveSecret(testRef, testCredential);
        
        // Then
        assertThat(result).isEqualTo(testSecret);
        verify(mockCache).get(testRef);
        verify(mockProvider).fetchSecret(testRef, testCredential);
        verify(mockCache).put(testSecret);
    }
    
    @Test
    void shouldThrowExceptionWhenNoProviderSupportsStoreType() {
        // Given
        SecretReference unsupportedRef = new SecretReference(StoreType.CYBERARK, "test", "latest");
        when(mockProvider.supportsStore(StoreType.CYBERARK)).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> service.resolveSecret(unsupportedRef, testCredential))
                .isInstanceOf(SecretAccessException.class)
                .hasMessageContaining("No provider found");
    }
    
    @Test
    void shouldRefreshSecretAndUpdateCache() throws SecretAccessException {
        // Given
        when(mockProvider.fetchSecret(testRef, testCredential)).thenReturn(testSecret);
        
        // When
        Secret result = service.refreshSecret(testRef, testCredential);
        
        // Then
        assertThat(result).isEqualTo(testSecret);
        verify(mockProvider).fetchSecret(testRef, testCredential);
        verify(mockCache).put(testSecret);
        
        // Verify event was published
        ArgumentCaptor<SecretRefreshed> eventCaptor = ArgumentCaptor.forClass(SecretRefreshed.class);
        verify(mockEventPublisher).publish(eventCaptor.capture());
        
        SecretRefreshed event = eventCaptor.getValue();
        assertThat(event.getSecretReference()).isEqualTo(testRef);
    }
    
    @Test
    void shouldPropagateExceptionFromProvider() throws SecretAccessException {
        // Given
        String errorMessage = "Provider failure";
        when(mockCache.get(testRef)).thenReturn(Optional.empty());
        when(mockProvider.fetchSecret(testRef, testCredential))
                .thenThrow(new SecretAccessException(errorMessage, testRef));
        
        // When/Then
        assertThatThrownBy(() -> service.resolveSecret(testRef, testCredential))
                .isInstanceOf(SecretAccessException.class)
                .hasMessageContaining(errorMessage);
    }
    
    @Test
    void shouldUseRefreshPolicyToCheckForRefresh() throws SecretAccessException {
        // Given
        when(mockCache.get(testRef)).thenReturn(Optional.of(testSecret));
        when(mockRefreshPolicy.isRefreshNeeded(eq(testRef), any(Secret.class))).thenReturn(true);
        when(mockProvider.fetchSecret(testRef, testCredential)).thenReturn(testSecret);
        
        // When
        service.resolveSecret(testRef, testCredential);
        
        // Then
        verify(mockRefreshPolicy).isRefreshNeeded(eq(testRef), any(Secret.class));
        verify(mockProvider).fetchSecret(testRef, testCredential); // Should fetch because refresh is needed
    }
    
    @Test
    void shouldNotRefreshWhenPolicyIndicatesNoRefreshNeeded() throws SecretAccessException {
        // Given
        when(mockCache.get(testRef)).thenReturn(Optional.of(testSecret));
        when(mockRefreshPolicy.isRefreshNeeded(eq(testRef), any(Secret.class))).thenReturn(false);
        
        // When
        service.resolveSecret(testRef, testCredential);
        
        // Then
        verify(mockRefreshPolicy).isRefreshNeeded(eq(testRef), any(Secret.class));
        verifyNoInteractions(mockProvider); // Should not fetch because no refresh is needed
    }
    
    @Test
    void shouldSkipRefreshCheckWhenRefreshPolicyIsNull() throws SecretAccessException {
        // Given
        DefaultSecretResolverService serviceWithoutPolicy = 
                new DefaultSecretResolverService(mockCache, null, mockEventPublisher);
        serviceWithoutPolicy.registerProvider(mockProvider);
        
        when(mockCache.get(testRef)).thenReturn(Optional.of(testSecret));
        
        // When
        serviceWithoutPolicy.resolveSecret(testRef, testCredential);
        
        // Then
        verifyNoInteractions(mockProvider); // Should not fetch because no refresh policy
    }
    
    @Test
    void shouldThrowExceptionWhenRefreshingWithNoProvider() {
        // Given
        SecretReference unsupportedRef = new SecretReference(StoreType.CYBERARK, "test", "latest");
        when(mockProvider.supportsStore(StoreType.CYBERARK)).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> service.refreshSecret(unsupportedRef, testCredential))
                .isInstanceOf(SecretAccessException.class)
                .hasMessageContaining("No provider found");
    }
}
