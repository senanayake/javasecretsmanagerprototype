package com.secrets.domain.model;

import java.util.Objects;

/**
 * Value object representing credentials used to access a secret store.
 * This immutable object contains information needed to authenticate against a secret provider.
 */
public final class AccessCredential {
    private final CredentialMethod method;
    private final Object value;

    /**
     * Creates a new AccessCredential.
     *
     * @param method The authentication method
     * @param value  The credential value (e.g., API key, role ARN)
     */
    public AccessCredential(CredentialMethod method, Object value) {
        this.method = Objects.requireNonNull(method, "Credential method cannot be null");
        this.value = Objects.requireNonNull(value, "Credential value cannot be null");
        
        // Validate that the value is of the expected type for the method
        validateCredentialValue();
    }
    
    private void validateCredentialValue() {
        switch (method) {
            case CYBERARK_API_KEY:
                if (!(value instanceof String)) {
                    throw new IllegalArgumentException("CYBERARK_API_KEY credential must be a String");
                }
                break;
            case IAM_ROLE:
                if (!(value instanceof String) && !(value instanceof STSAssumeRoleConfig)) {
                    throw new IllegalArgumentException("IAM_ROLE credential must be a String or STSAssumeRoleConfig");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported credential method: " + method);
        }
    }

    public CredentialMethod getMethod() {
        return method;
    }

    public Object getValue() {
        return value;
    }
    
    /**
     * Returns the credential value as a String.
     * Should only be called when the value is known to be a String.
     *
     * @return the credential value as a String
     * @throws ClassCastException if the value is not a String
     */
    public String getValueAsString() {
        return (String) value;
    }
    
    /**
     * Returns the credential value as an STSAssumeRoleConfig.
     * Should only be called when the value is known to be an STSAssumeRoleConfig.
     *
     * @return the credential value as an STSAssumeRoleConfig
     * @throws ClassCastException if the value is not an STSAssumeRoleConfig
     */
    public STSAssumeRoleConfig getValueAsSTSConfig() {
        return (STSAssumeRoleConfig) value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessCredential that = (AccessCredential) o;
        return method == that.method &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, value);
    }

    @Override
    public String toString() {
        // Don't log the actual credential value for security reasons
        return "AccessCredential{" +
                "method=" + method +
                ", value=[REDACTED]" +
                '}';
    }
}
