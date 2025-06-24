package com.secrets.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretReferenceTest {

    @Test
    void shouldCreateValidSecretReference() {
        // Given
        StoreType storeType = StoreType.AWS_SECRETS_MANAGER;
        String name = "test-secret";
        String versionHint = "latest";
        
        // When
        SecretReference reference = new SecretReference(storeType, name, versionHint);
        
        // Then
        assertThat(reference.getStoreType()).isEqualTo(storeType);
        assertThat(reference.getName()).isEqualTo(name);
        assertThat(reference.getVersionHint()).isEqualTo(versionHint);
    }
    
    @Test
    void shouldUseLatestVersionHintByDefault() {
        // Given
        StoreType storeType = StoreType.AWS_SECRETS_MANAGER;
        String name = "test-secret";
        
        // When
        SecretReference reference = new SecretReference(storeType, name);
        
        // Then
        assertThat(reference.getVersionHint()).isEqualTo("latest");
    }
    
    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        SecretReference reference1 = new SecretReference(StoreType.AWS_SECRETS_MANAGER, "test-secret", "latest");
        SecretReference reference2 = new SecretReference(StoreType.AWS_SECRETS_MANAGER, "test-secret", "latest");
        SecretReference reference3 = new SecretReference(StoreType.CYBERARK, "test-secret", "latest");
        SecretReference reference4 = new SecretReference(StoreType.AWS_SECRETS_MANAGER, "other-secret", "latest");
        SecretReference reference5 = new SecretReference(StoreType.AWS_SECRETS_MANAGER, "test-secret", "v1");
        
        // Then
        assertThat(reference1).isEqualTo(reference2);
        assertThat(reference1).isNotEqualTo(reference3);
        assertThat(reference1).isNotEqualTo(reference4);
        assertThat(reference1).isNotEqualTo(reference5);
        
        assertThat(reference1.hashCode()).isEqualTo(reference2.hashCode());
        assertThat(reference1.hashCode()).isNotEqualTo(reference3.hashCode());
    }
    
    @Test
    void shouldImplementToString() {
        // Given
        SecretReference reference = new SecretReference(StoreType.AWS_SECRETS_MANAGER, "test-secret", "latest");
        
        // When
        String toString = reference.toString();
        
        // Then
        assertThat(toString).contains("AWS_SECRETS_MANAGER");
        assertThat(toString).contains("test-secret");
        assertThat(toString).contains("latest");
    }
    
    @Test
    void shouldRejectNullStoreType() {
        assertThatThrownBy(() -> new SecretReference(null, "test-secret", "latest"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Store type");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectNullOrEmptyName(String name) {
        assertThatThrownBy(() -> new SecretReference(StoreType.AWS_SECRETS_MANAGER, name, "latest"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("name");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectNullOrEmptyVersionHint(String versionHint) {
        assertThatThrownBy(() -> new SecretReference(StoreType.AWS_SECRETS_MANAGER, "test-secret", versionHint))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("version hint");
    }
}
