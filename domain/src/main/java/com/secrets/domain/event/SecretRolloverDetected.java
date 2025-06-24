package com.secrets.domain.event;

import com.secrets.domain.model.SecretReference;

import java.util.Objects;

/**
 * Domain event indicating that a secret rollover has been detected.
 * This occurs when an active/inactive pair of secrets has been switched,
 * indicating that a rotation has occurred.
 */
public class SecretRolloverDetected extends SecretEvent {
    private final SecretReference activeReference;
    private final SecretReference inactiveReference;
    private final String newActiveVersion;

    /**
     * Creates a new SecretRolloverDetected event.
     *
     * @param activeReference   Reference to the now active secret
     * @param inactiveReference Reference to the now inactive secret
     * @param newActiveVersion  Version of the new active secret
     */
    public SecretRolloverDetected(
            SecretReference activeReference,
            SecretReference inactiveReference,
            String newActiveVersion) {
        this.activeReference = Objects.requireNonNull(activeReference, "Active reference cannot be null");
        this.inactiveReference = Objects.requireNonNull(inactiveReference, "Inactive reference cannot be null");
        this.newActiveVersion = Objects.requireNonNull(newActiveVersion, "New active version cannot be null");
    }

    public SecretReference getActiveReference() {
        return activeReference;
    }

    public SecretReference getInactiveReference() {
        return inactiveReference;
    }

    public String getNewActiveVersion() {
        return newActiveVersion;
    }

    @Override
    public String toString() {
        return "SecretRolloverDetected{" +
                "eventId='" + getEventId() + '\'' +
                ", timestamp=" + getTimestamp() +
                ", activeReference=" + activeReference +
                ", inactiveReference=" + inactiveReference +
                ", newActiveVersion='" + newActiveVersion + '\'' +
                '}';
    }
}
