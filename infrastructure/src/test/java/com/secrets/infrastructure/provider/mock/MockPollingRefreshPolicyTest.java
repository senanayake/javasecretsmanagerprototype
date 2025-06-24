package com.secrets.infrastructure.provider.mock;

import com.secrets.domain.event.SecretRefreshRequested;
import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretMetadata;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import com.secrets.domain.service.SecretAccessException;
import com.secrets.domain.service.SecretCache;
import com.secrets.domain.service.SecretProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MockPollingRefreshPolicyTest {
    
    private MockPollingRefreshPolicy refreshPolicy;
    private SecretProvider mockProvider;
    private SecretCache mockCache;
    private List<Object> publishedEvents;
    private Consumer<Object> eventPublisher;
    
    private SecretReference testRef;
    private AccessCredential testCredential;
    private Secret testSecret;
    
    @BeforeEach
    void setUp() {
        publishedEvents = new ArrayList<>();
        eventPublisher = publishedEvents::add;
        
        refreshPolicy = new MockPollingRefreshPolicy(Duration.ofMillis(100), eventPublisher);
        
        mockProvider = mock(SecretProvider.class);
        mockCache = mock(SecretCache.class);
        
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
        
        refreshPolicy.apply(mockProvider, mockCache);
    }
    
    @AfterEach
    void tearDown() {
        if (refreshPolicy.isRunning()) {
            refreshPolicy.stop();
        }
    }
    
    @Test
    void shouldReportRefreshNeededForNullSecret() {
        // When/Then
        assertThat(refreshPolicy.isRefreshNeeded(testRef, null)).isTrue();
    }
    
    @Test
    void shouldUseStaleCheckFromCache() {
        // Given
        when(mockCache.isStale(testRef)).thenReturn(true);
        
        // When/Then
        assertThat(refreshPolicy.isRefreshNeeded(testRef, testSecret)).isTrue();
        
        // Verify the cache was called
        verify(mockCache).isStale(testRef);
    }
    
    @Test
    void shouldTriggerRefreshSuccessfully() throws SecretAccessException {
        // Given
        refreshPolicy.registerSecret(testRef, testCredential);
        when(mockProvider.fetchSecret(testRef, testCredential)).thenReturn(testSecret);
        
        // When
        refreshPolicy.triggerRefresh(testRef);
        
        // Then
        // Verify event was published
        assertThat(publishedEvents).hasSize(1);
        assertThat(publishedEvents.get(0)).isInstanceOf(SecretRefreshRequested.class);
        
        // Verify provider was called
        verify(mockProvider).fetchSecret(testRef, testCredential);
        
        // Verify cache was updated
        verify(mockCache).put(testSecret);
    }
    
    @Test
    void shouldIgnoreTriggerRefreshForUnregisteredSecret() {
        // When
        refreshPolicy.triggerRefresh(testRef);
        
        // Then
        assertThat(publishedEvents).isEmpty();
        verifyNoInteractions(mockProvider);
        verifyNoInteractions(mockCache);
    }
    
    @Test
    void shouldStartAndStopPolling() throws InterruptedException {
        // Given
        refreshPolicy.registerSecret(testRef, testCredential);
        when(mockCache.get(testRef)).thenReturn(Optional.empty());
        when(mockProvider.fetchSecret(testRef, testCredential)).thenReturn(testSecret);
        
        // Create a latch to wait for the first polling cycle
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mockCache).put(any(Secret.class));
        
        // When
        refreshPolicy.start();
        
        // Then
        assertThat(refreshPolicy.isRunning()).isTrue();
        
        // Wait for at least one polling cycle
        assertThat(latch.await(500, TimeUnit.MILLISECONDS)).isTrue();
        
        // Verify provider was called at least once
        verify(mockProvider, atLeastOnce()).fetchSecret(eq(testRef), any(AccessCredential.class));
        
        // Stop polling
        refreshPolicy.stop();
        assertThat(refreshPolicy.isRunning()).isFalse();
    }
    
    @Test
    void shouldRegisterAndUnregisterSecret() {
        // When
        refreshPolicy.registerSecret(testRef, testCredential);
        refreshPolicy.unregisterSecret(testRef);
        
        // Then
        // No direct way to verify, so trigger refresh which should do nothing
        refreshPolicy.triggerRefresh(testRef);
        
        // Verify no interactions
        verifyNoInteractions(mockProvider);
        verifyNoInteractions(mockCache);
    }
    
    @Test
    void shouldHandleProviderExceptions() throws SecretAccessException {
        // Given
        refreshPolicy.registerSecret(testRef, testCredential);
        when(mockProvider.fetchSecret(testRef, testCredential))
                .thenThrow(new SecretAccessException("Test error", testRef));
        
        // When
        refreshPolicy.triggerRefresh(testRef);
        
        // Then
        // Event should still be published
        assertThat(publishedEvents).hasSize(1);
        
        // Provider should be called
        verify(mockProvider).fetchSecret(testRef, testCredential);
        
        // Cache should not be updated
        verifyNoInteractions(mockCache);
    }
}
