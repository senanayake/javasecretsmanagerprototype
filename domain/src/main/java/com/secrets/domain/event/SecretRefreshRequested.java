package com.secrets.domain.event;

import com.secrets.domain.model.SecretReference;

import java.util.Objects;

/**
 * Domain event indicating that a secret refresh has been requested.
 * This could be triggered by a timer, external notification, or manual request.
 */
public class SecretRefreshRequested extends SecretEvent {
    private final SecretReference secretReference;
    private final String reason;

    /**
     * Creates a new SecretRefreshRequested event.
     *
     * @param secretReference The reference to the secret that should be refreshed
     * @param reason A description of why the refresh was requested
     */
    public SecretRefreshRequested(SecretReference secretReference, String reason) {
        this.secretReference = Objects.requireNonNull(secretReference, "Secret reference cannot be null");
        this.reason = reason != null ? reason : "Periodic refresh";
    }

    public SecretReference getSecretReference() {
        return secretReference;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "SecretRefreshRequested{" +
                "eventId='" + getEventId() + '\'' +
                ", timestamp=" + getTimestamp() +
                ", secretReference=" + secretReference +
                ", reason='" + reason + '\'' +
                '}';
    }
}
