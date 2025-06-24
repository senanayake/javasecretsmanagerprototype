package com.secrets.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessCredentialTest {

    @Test
    void shouldCreateValidCyberArkApiKeyCredential() {
        // Given
        String apiKey = "mock-api-key";
        
        // When
        AccessCredential credential = AccessCredential.forCyberArkApiKey(apiKey);
        
        // Then
        assertThat(credential.getCredentialMethod()).isEqualTo(CredentialMethod.CYBERARK_API_KEY);
        assertThat(credential.getValue()).isEqualTo(apiKey);
    }
    
    @Test
    void shouldCreateValidIamRoleCredential() {
        // Given
        STSAssumeRoleConfig config = STSAssumeRoleConfig.builder()
                .withRoleArn("arn:aws:iam::123456789012:role/TestRole")
                .withRoleSessionName("test-session")
                .build();
        
        // When
        AccessCredential credential = AccessCredential.forIamRole(config);
        
        // Then
        assertThat(credential.getCredentialMethod()).isEqualTo(CredentialMethod.IAM_ROLE);
        assertThat(credential.getValue()).isEqualTo(config);
    }
    
    @Test
    void shouldCreateCustomCredential() {
        // Given
        CredentialMethod method = CredentialMethod.CYBERARK_API_KEY;
        String value = "custom-value";
        
        // When
        AccessCredential credential = new AccessCredential(method, value);
        
        // Then
        assertThat(credential.getCredentialMethod()).isEqualTo(method);
        assertThat(credential.getValue()).isEqualTo(value);
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectNullOrEmptyCyberArkApiKey(String apiKey) {
        assertThatThrownBy(() -> AccessCredential.forCyberArkApiKey(apiKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key");
    }
    
    @Test
    void shouldRejectNullIamRoleConfig() {
        assertThatThrownBy(() -> AccessCredential.forIamRole(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("STS AssumeRole config");
    }
    
    @Test
    void shouldRejectNullCredentialMethod() {
        assertThatThrownBy(() -> new AccessCredential(null, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credential method");
    }
    
    @Test
    void shouldRejectNullCredentialValue() {
        assertThatThrownBy(() -> new AccessCredential(CredentialMethod.CYBERARK_API_KEY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credential value");
    }
    
    @Test
    void shouldValidateTypeMatchForIamRoleCredential() {
        // Given: Wrong type for IAM_ROLE (should be STSAssumeRoleConfig)
        String invalidValue = "invalid-value";
        
        // When/Then
        assertThatThrownBy(() -> new AccessCredential(CredentialMethod.IAM_ROLE, invalidValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IAM_ROLE credential must be an STSAssumeRoleConfig");
    }
    
    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        AccessCredential credential1 = AccessCredential.forCyberArkApiKey("api-key-1");
        AccessCredential credential2 = AccessCredential.forCyberArkApiKey("api-key-1");
        AccessCredential credential3 = AccessCredential.forCyberArkApiKey("api-key-2");
        
        STSAssumeRoleConfig config = STSAssumeRoleConfig.builder()
                .withRoleArn("arn:aws:iam::123456789012:role/TestRole")
                .withRoleSessionName("test-session")
                .build();
        AccessCredential credential4 = AccessCredential.forIamRole(config);
        
        // Then
        assertThat(credential1).isEqualTo(credential1); // self equality
        assertThat(credential1).isEqualTo(credential2); // same values
        assertThat(credential1).isNotEqualTo(credential3); // different values
        assertThat(credential1).isNotEqualTo(credential4); // different types
        assertThat(credential1).isNotEqualTo(null); // null check
        assertThat(credential1).isNotEqualTo("not-a-credential"); // type check
        
        assertThat(credential1.hashCode()).isEqualTo(credential2.hashCode());
        assertThat(credential1.hashCode()).isNotEqualTo(credential3.hashCode());
    }
    
    @Test
    void shouldImplementToString() {
        // Given
        AccessCredential apiKeyCredential = AccessCredential.forCyberArkApiKey("api-key");
        
        STSAssumeRoleConfig config = STSAssumeRoleConfig.builder()
                .withRoleArn("arn:aws:iam::123456789012:role/TestRole")
                .withRoleSessionName("test-session")
                .build();
        AccessCredential iamRoleCredential = AccessCredential.forIamRole(config);
        
        // When
        String apiKeyString = apiKeyCredential.toString();
        String iamRoleString = iamRoleCredential.toString();
        
        // Then
        assertThat(apiKeyString).contains("CYBERARK_API_KEY");
        assertThat(apiKeyString).doesNotContain("api-key"); // Should not contain the actual key
        
        assertThat(iamRoleString).contains("IAM_ROLE");
        assertThat(iamRoleString).contains("TestRole");
    }
}
