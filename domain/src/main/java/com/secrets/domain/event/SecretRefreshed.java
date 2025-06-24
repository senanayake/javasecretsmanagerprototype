package com.secrets.domain.event;

import com.secrets.domain.model.Secret;
import com.secrets.domain.model.SecretReference;

import java.util.Objects;

/**
 * Domain event indicating that a secret has been successfully refreshed.
 * This event is published after a secret has been fetched from its source
 * and updated in the cache.
 */
public class SecretRefreshed extends SecretEvent {
    private final SecretReference secretReference;
    private final String version;
    private final boolean valueChanged;

    /**
     * Creates a new SecretRefreshed event.
     *
     * @param secret       The refreshed secret
     * @param valueChanged Flag indicating if the secret value actually changed
     */
    public SecretRefreshed(Secret secret, boolean valueChanged) {
        Objects.requireNonNull(secret, "Secret cannot be null");
        this.secretReference = secret.getMetadata().getSourceRef();
        this.version = secret.getMetadata().getVersion();
        this.valueChanged = valueChanged;
    }

    /**
     * Creates a new SecretRefreshed event with the secret reference and version.
     * 
     * @param secretReference The reference to the refreshed secret
     * @param version         The version of the refreshed secret
     * @param valueChanged    Flag indicating if the secret value actually changed
     */
    public SecretRefreshed(SecretReference secretReference, String version, boolean valueChanged) {
        this.secretReference = Objects.requireNonNull(secretReference, "Secret reference cannot be null");
        this.version = Objects.requireNonNull(version, "Version cannot be null");
        this.valueChanged = valueChanged;
    }

    public SecretReference getSecretReference() {
        return secretReference;
    }

    public String getVersion() {
        return version;
    }

    public boolean isValueChanged() {
        return valueChanged;
    }

    @Override
    public String toString() {
        return "SecretRefreshed{" +
                "eventId='" + getEventId() + '\'' +
                ", timestamp=" + getTimestamp() +
                ", secretReference=" + secretReference +
                ", version='" + version + '\'' +
                ", valueChanged=" + valueChanged +
                '}';
    }
}
