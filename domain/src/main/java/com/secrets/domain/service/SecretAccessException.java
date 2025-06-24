package com.secrets.domain.service;

import com.secrets.domain.model.SecretReference;

/**
 * Exception thrown when there is a problem accessing a secret.
 */
public class SecretAccessException extends RuntimeException {
    
    private final SecretReference reference;

    /**
     * Creates a new SecretAccessException with a message.
     *
     * @param message   The detailed message
     * @param reference The reference to the secret that caused the exception
     */
    public SecretAccessException(String message, SecretReference reference) {
        super(message);
        this.reference = reference;
    }

    /**
     * Creates a new SecretAccessException with a message and cause.
     *
     * @param message   The detailed message
     * @param cause     The cause of this exception
     * @param reference The reference to the secret that caused the exception
     */
    public SecretAccessException(String message, Throwable cause, SecretReference reference) {
        super(message, cause);
        this.reference = reference;
    }

    /**
     * Gets the reference to the secret that caused the exception.
     *
     * @return The secret reference
     */
    public SecretReference getReference() {
        return reference;
    }
}
