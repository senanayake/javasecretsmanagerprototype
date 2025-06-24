package com.secrets.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity class representing metadata associated with a secret.
 */
public class SecretMetadata {
    private final String version;
    private final Instant lastRetrieved;
    private final StoreType storeType;
    private final SecretReference sourceRef;

    /**
     * Creates a new SecretMetadata instance.
     *
     * @param version       The version of the secret
     * @param lastRetrieved The timestamp when the secret was last retrieved
     * @param storeType     The type of store this secret came from
     * @param sourceRef     The reference to the source secret
     */
    public SecretMetadata(String version, Instant lastRetrieved, StoreType storeType, SecretReference sourceRef) {
        this.version = Objects.requireNonNull(version, "Version cannot be null");
        this.lastRetrieved = Objects.requireNonNull(lastRetrieved, "Last retrieved timestamp cannot be null");
        this.storeType = Objects.requireNonNull(storeType, "Store type cannot be null");
        this.sourceRef = Objects.requireNonNull(sourceRef, "Source reference cannot be null");
    }

    /**
     * Creates a new SecretMetadata with current timestamp.
     *
     * @param version   The version of the secret
     * @param storeType The type of store this secret came from
     * @param sourceRef The reference to the source secret
     */
    public SecretMetadata(String version, StoreType storeType, SecretReference sourceRef) {
        this(version, Instant.now(), storeType, sourceRef);
    }

    public String getVersion() {
        return version;
    }

    public Instant getLastRetrieved() {
        return lastRetrieved;
    }

    public StoreType getStoreType() {
        return storeType;
    }

    public SecretReference getSourceRef() {
        return sourceRef;
    }

    /**
     * Creates a new SecretMetadata with the current timestamp updated.
     *
     * @return A new SecretMetadata instance with updated timestamp
     */
    public SecretMetadata withUpdatedTimestamp() {
        return new SecretMetadata(version, Instant.now(), storeType, sourceRef);
    }

    /**
     * Creates a new SecretMetadata with an updated version.
     *
     * @param newVersion The new version string
     * @return A new SecretMetadata instance with updated version and timestamp
     */
    public SecretMetadata withNewVersion(String newVersion) {
        return new SecretMetadata(newVersion, Instant.now(), storeType, sourceRef);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecretMetadata that = (SecretMetadata) o;
        return Objects.equals(version, that.version) &&
                Objects.equals(storeType, that.storeType) &&
                Objects.equals(sourceRef, that.sourceRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, storeType, sourceRef);
    }

    @Override
    public String toString() {
        return "SecretMetadata{" +
                "version='" + version + '\'' +
                ", lastRetrieved=" + lastRetrieved +
                ", storeType=" + storeType +
                ", sourceRef=" + sourceRef +
                '}';
    }
}
