package com.secrets.domain.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity class representing a secret.
 * This is the core entity of the domain model.
 */
public class Secret {
    private final String id;
    private final String name;
    private final char[] value;
    private final SecretMetadata metadata;

    /**
     * Creates a new Secret instance.
     *
     * @param id       The unique identifier of the secret
     * @param name     The name/description of the secret
     * @param value    The actual secret value as char array (for security)
     * @param metadata The metadata associated with the secret
     */
    public Secret(String id, String name, char[] value, SecretMetadata metadata) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.value = Objects.requireNonNull(value, "Value cannot be null").clone();
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
    }

    /**
     * Creates a new Secret instance with a generated UUID.
     *
     * @param name     The name/description of the secret
     * @param value    The actual secret value as char array (for security)
     * @param metadata The metadata associated with the secret
     */
    public Secret(String name, char[] value, SecretMetadata metadata) {
        this(UUID.randomUUID().toString(), name, value, metadata);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets a copy of the secret value.
     * The returned array should be cleared after use for security.
     *
     * @return A copy of the secret value as char array
     */
    public char[] getValue() {
        return value.clone();
    }

    public SecretMetadata getMetadata() {
        return metadata;
    }

    /**
     * Creates a new Secret with updated metadata timestamp.
     *
     * @return A new Secret with the same values but updated metadata timestamp
     */
    public Secret withRefreshedTimestamp() {
        return new Secret(id, name, value, metadata.withUpdatedTimestamp());
    }

    /**
     * Creates a new Secret with a new version in the metadata.
     *
     * @param newVersion The new version identifier
     * @return A new Secret with the same values but updated metadata version
     */
    public Secret withNewVersion(String newVersion) {
        return new Secret(id, name, value, metadata.withNewVersion(newVersion));
    }

    /**
     * Creates a new Secret with a different value but same metadata.
     *
     * @param newValue The new secret value
     * @return A new Secret with the updated value and same metadata
     */
    public Secret withNewValue(char[] newValue) {
        return new Secret(id, name, newValue, metadata);
    }

    /**
     * Clears the secret value by overwriting it with zeros.
     * This should be called when the secret is no longer needed.
     */
    public void clearValue() {
        Arrays.fill(value, '0');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Secret secret = (Secret) o;
        return Objects.equals(id, secret.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Secret{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", value=[REDACTED]" +
                ", metadata=" + metadata +
                '}';
    }

    /**
     * Auto-closeable implementation to ensure secret values are cleared.
     * This allows the Secret to be used in try-with-resources blocks.
     */
    public static class AutoClearingSecret implements AutoCloseable {
        private final Secret secret;

        public AutoClearingSecret(Secret secret) {
            this.secret = secret;
        }

        public Secret getSecret() {
            return secret;
        }

        @Override
        public void close() {
            secret.clearValue();
        }
    }
}
