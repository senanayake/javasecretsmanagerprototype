package com.secrets.domain.model;

import java.util.Objects;

/**
 * Value object representing a reference to a secret in a specific store.
 * This immutable object contains all information needed to uniquely identify a secret.
 */
public final class SecretReference {
    private final StoreType storeType;
    private final String name;
    private final String versionHint;

    /**
     * Creates a new SecretReference.
     *
     * @param storeType   The type of secret store
     * @param name        The name/identifier of the secret
     * @param versionHint The version hint (e.g., "latest", "v1", "inactive")
     */
    public SecretReference(StoreType storeType, String name, String versionHint) {
        this.storeType = Objects.requireNonNull(storeType, "Store type cannot be null");
        this.name = Objects.requireNonNull(name, "Secret name cannot be null or empty");
        this.versionHint = versionHint != null ? versionHint : "latest";
    }

    /**
     * Convenience constructor for creating a reference with "latest" version hint.
     *
     * @param storeType The type of secret store
     * @param name      The name/identifier of the secret
     */
    public SecretReference(StoreType storeType, String name) {
        this(storeType, name, "latest");
    }

    public StoreType getStoreType() {
        return storeType;
    }

    public String getName() {
        return name;
    }

    public String getVersionHint() {
        return versionHint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecretReference that = (SecretReference) o;
        return storeType == that.storeType &&
                Objects.equals(name, that.name) &&
                Objects.equals(versionHint, that.versionHint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeType, name, versionHint);
    }

    @Override
    public String toString() {
        return "SecretReference{" +
                "storeType=" + storeType +
                ", name='" + name + '\'' +
                ", versionHint='" + versionHint + '\'' +
                '}';
    }
}
