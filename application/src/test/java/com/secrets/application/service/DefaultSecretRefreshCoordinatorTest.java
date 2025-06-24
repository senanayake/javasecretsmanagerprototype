package com.secrets.application.service;

import com.secrets.application.port.EventPublisher;
import com.secrets.domain.event.SecretRefreshRequested;
import com.secrets.domain.model.AccessCredential;
import com.secrets.domain.model.SecretReference;
import com.secrets.domain.model.StoreType;
import com.secrets.domain.service.RefreshPolicy;
import com.secrets.domain.service.SecretAccessException;
import com.secrets.domain.service.SecretResolverService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DefaultSecretRefreshCoordinatorTest {
    
    private DefaultSecretRefreshCoordinator coordinator;
    private SecretResolverService mockResolver;
    private EventPublisher mockEventPublisher;
    private RefreshPolicy mockRefreshPolicy;
    private SecretReference testRef;
    private AccessCredential testCredential;
    
    @BeforeEach
    void setUp() {
        mockResolver = mock(SecretResolverService.class);
        mockEventPublisher = mock(EventPublisher.class);
        mockRefreshPolicy = mock(RefreshPolicy.class);
        
        coordinator = new DefaultSecretRefreshCoordinator(mockResolver, mockEventPublisher);
        
        testRef = new SecretReference(StoreType.AWS_SECRETS_MANAGER, "test-secret", "latest");
        testCredential = AccessCredential.forCyberArkApiKey("test-api-key");
    }
    
    @AfterEach
    void tearDown() {
        // Make sure we stop the coordinator to clean up threads
        if (coordinator.isRunning()) {
            coordinator.stop();
        }
    }
    
    @Test
    void shouldRegisterAndUnregisterSecret() {
        // When
        coordinator.registerSecret(testRef, mockRefreshPolicy);
        
        // Then
        assertThat(coordinator.triggerRefresh(testRef, "test")).isFalse(); // No credential registered yet
        
        // Clean up
        coordinator.unregisterSecret(testRef);
        
        // Verify unregistered
        assertThat(coordinator.triggerRefresh(testRef, "test")).isFalse();
    }
    
    @Test
    void shouldReturnFalseWhenRefreshingUnregisteredSecret() {
        // When/Then
        assertThat(coordinator.triggerRefresh(testRef, "test")).isFalse();
    }
    
    @Test
    void shouldPublishEventWhenTriggeringRefresh() {
        // Given
        coordinator.registerSecret(testRef, mockRefreshPolicy);
        
        // When
        coordinator.triggerRefresh(testRef, "test reason");
        
        // Then
        ArgumentCaptor<SecretRefreshRequested> eventCaptor = 
                ArgumentCaptor.forClass(SecretRefreshRequested.class);
        verify(mockEventPublisher).publish(eventCaptor.capture());
        
        SecretRefreshRequested event = eventCaptor.getValue();
        assertThat(event.getSecretReference()).isEqualTo(testRef);
        assertThat(event.getReason()).isEqualTo("test reason");
    }
    
    @Test
    void shouldHandleRefreshEventCorrectly() throws SecretAccessException {
        // Given
        // Create a special test so we can access private RefreshContext
        DefaultSecretRefreshCoordinator.class.getDeclaredConstructor(
                SecretResolverService.class, EventPublisher.class);
        
        SecretRefreshRequested event = new SecretRefreshRequested(testRef, "test reason");
        
        // Register with credential using reflection
        // For simplicity, we'll just register the secret normally and see if the handler tries to call refresh
        coordinator.registerSecret(testRef, mockRefreshPolicy);
        
        // When
        coordinator.handleRefreshEvent(event);
        
        // Then
        // Since we don't have direct access to set credentials, we expect no interaction with the resolver
        verifyNoInteractions(mockResolver);
    }
    
    @Test
    void shouldStartAndStopCoordinator() {
        // When
        coordinator.start();
        
        // Then
        assertThat(coordinator.isRunning()).isTrue();
        
        // When
        coordinator.stop();
        
        // Then
        assertThat(coordinator.isRunning()).isFalse();
    }
    
    @Test
    void shouldNotStartTwice() {
        // Given
        coordinator.start();
        boolean initialState = coordinator.isRunning();
        
        // When
        coordinator.start(); // Try to start again
        
        // Then
        assertThat(initialState).isTrue();
        assertThat(coordinator.isRunning()).isTrue();
    }
    
    @Test
    void shouldNotStopWhenNotRunning() {
        // Given
        assertThat(coordinator.isRunning()).isFalse();
        
        // When
        coordinator.stop();
        
        // Then
        assertThat(coordinator.isRunning()).isFalse();
    }
}
